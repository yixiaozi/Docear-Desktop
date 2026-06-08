package org.docear.plugin.core.todoist;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.MindMapDataRootResolver;

final class TodoistApiClient {
	private static final String API_BASE = "https://api.todoist.com/api/v1";
	private static final int CONNECT_TIMEOUT_MS = 15000;
	private static final int READ_TIMEOUT_MS = 30000;

	private final String apiToken;
	private final Map sectionIdCache = new HashMap();

	TodoistApiClient(String apiToken) {
		this.apiToken = apiToken;
	}

	String ensureProject(String projectName) throws IOException {
		String projectId = findProjectIdByName(projectName);
		if (projectId != null) {
			return projectId;
		}
		String body = "{\"name\":\"" + TodoistJson.escape(projectName) + "\"}";
		String response = request("POST", "/projects", body);
		projectId = TodoistJson.extractStringField(response, "id");
		if (projectId == null || projectId.length() == 0) {
			throw new IOException("Todoist create project: missing id");
		}
		LogUtils.info("Todoist: created project " + projectName);
		return projectId;
	}

	String ensureSection(String projectId, String sectionName, TodoistSectionStore store) throws IOException {
		String cacheKey = projectId + "|" + sectionName;
		if (sectionIdCache.containsKey(cacheKey)) {
			return (String) sectionIdCache.get(cacheKey);
		}
		String stored = store.getSectionId(projectId, sectionName);
		if (stored != null && stored.length() > 0) {
			sectionIdCache.put(cacheKey, stored);
			return stored;
		}
		String sectionId = findSectionIdByName(projectId, sectionName);
		if (sectionId == null) {
			String body = "{\"name\":\"" + TodoistJson.escape(truncate(sectionName, 120))
					+ "\",\"project_id\":\"" + TodoistJson.escape(projectId) + "\"}";
			String response = request("POST", "/sections", body);
			sectionId = TodoistJson.extractStringField(response, "id");
			if (sectionId == null || sectionId.length() == 0) {
				throw new IOException("Todoist create section: missing id");
			}
			LogUtils.info("Todoist: created section " + sectionName);
		}
		store.putSectionId(projectId, sectionName, sectionId);
		sectionIdCache.put(cacheKey, sectionId);
		return sectionId;
	}

	static String sectionNameForFile(File file) {
		String base = file.getName();
		if (base.toLowerCase().endsWith(".mm")) {
			base = base.substring(0, base.length() - 3);
		}
		String parentPath = MindMapDataRootResolver.getRelativePathWithinScanRoots(file.getParentFile());
		if (parentPath != null && parentPath.length() > 0) {
			return parentPath.replace("/", " / ") + " / " + base;
		}
		return base;
	}

	String createTask(TodoistReminderRecord record, String projectId, String sectionId) throws IOException {
		String body = buildTaskJson(record, projectId, sectionId);
		String response = request("POST", "/tasks", body);
		String taskId = TodoistJson.extractStringField(response, "id");
		if (taskId == null || taskId.length() == 0) {
			throw new IOException("Todoist create task: missing id in response");
		}
		return taskId;
	}

	void updateTask(String taskId, TodoistReminderRecord record, String projectId, String sectionId)
			throws IOException {
		String body = buildTaskJson(record, projectId, sectionId);
		request("POST", "/tasks/" + taskId, body);
	}

	void closeTask(String taskId) throws IOException {
		request("POST", "/tasks/" + taskId + "/close", null);
	}

	private String findProjectIdByName(String projectName) throws IOException {
		String cursor = null;
		do {
			String path = "/projects?limit=200";
			if (cursor != null) {
				path += "&cursor=" + urlEncode(cursor);
			}
			String response = request("GET", path, null);
			String id = TodoistJson.findIdByExactName(response, projectName);
			if (id != null) {
				return id;
			}
			cursor = TodoistJson.extractNextCursor(response);
		}
		while (cursor != null);
		return null;
	}

	private String findSectionIdByName(String projectId, String sectionName) throws IOException {
		String cursor = null;
		do {
			String path = "/sections?project_id=" + urlEncode(projectId) + "&limit=200";
			if (cursor != null) {
				path += "&cursor=" + urlEncode(cursor);
			}
			String response = request("GET", path, null);
			String id = TodoistJson.findIdByExactName(response, sectionName);
			if (id != null) {
				return id;
			}
			cursor = TodoistJson.extractNextCursor(response);
		}
		while (cursor != null);
		return null;
	}

	private String buildTaskJson(TodoistReminderRecord record, String projectId, String sectionId) {
		StringBuilder sb = new StringBuilder(320);
		sb.append("{\"content\":\"").append(TodoistJson.escape(truncate(record.nodeText, 500))).append("\"");
		sb.append(",\"description\":\"").append(TodoistJson.escape(buildDescription(record))).append("\"");
		sb.append(",\"project_id\":\"").append(TodoistJson.escape(projectId)).append("\"");
		sb.append(",\"section_id\":\"").append(TodoistJson.escape(sectionId)).append("\"");
		appendDueFields(sb, record);
		sb.append('}');
		return sb.toString();
	}

	private void appendDueFields(StringBuilder sb, TodoistReminderRecord record) {
		if (record.recurring) {
			String dueString = toRecurringDueString(record);
			if (dueString != null && dueString.length() > 0) {
				sb.append(",\"due_string\":\"").append(TodoistJson.escape(dueString)).append("\"");
				sb.append(",\"due_lang\":\"en\"");
			}
			else {
				sb.append(",\"due_datetime\":\"").append(TodoistJson.escape(formatUtcDateTime(record.remindAt)))
						.append("\"");
			}
		}
		else {
			sb.append(",\"due_datetime\":\"").append(TodoistJson.escape(formatUtcDateTime(record.remindAt))).append("\"");
		}
	}

	private static String toRecurringDueString(TodoistReminderRecord record) {
		int period = record.period <= 0 ? 1 : record.period;
		String unit = record.periodUnit == null ? "DAY" : record.periodUnit.toUpperCase();
		if ("MINUTE".equals(unit) || "HOUR".equals(unit)) {
			return null;
		}
		if ("DAY".equals(unit)) {
			return period == 1 ? "every day" : "every " + period + " days";
		}
		if ("WEEK".equals(unit)) {
			return period == 1 ? "every week" : "every " + period + " weeks";
		}
		if ("MONTH".equals(unit)) {
			return period == 1 ? "every month" : "every " + period + " months";
		}
		if ("YEAR".equals(unit)) {
			return period == 1 ? "every year" : "every " + period + " years";
		}
		return null;
	}

	private static String buildDescription(TodoistReminderRecord record) {
		StringBuilder sb = new StringBuilder();
		sb.append("Docear reminder\n");
		sb.append("Map: ").append(record.file.getName()).append('\n');
		sb.append("Path: ").append(record.file.getAbsolutePath()).append('\n');
		sb.append("Node ID: ").append(record.nodeId);
		return sb.toString();
	}

	private static String formatUtcDateTime(long millis) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		format.setTimeZone(TimeZone.getTimeZone("UTC"));
		return format.format(new Date(millis));
	}

	private static String truncate(String text, int maxLen) {
		if (text == null) {
			return "";
		}
		if (text.length() <= maxLen) {
			return text;
		}
		return text.substring(0, maxLen - 3) + "...";
	}

	private static String urlEncode(String value) throws UnsupportedEncodingException {
		return URLEncoder.encode(value, "UTF-8");
	}

	private String request(String method, String path, String jsonBody) throws IOException {
		URL url = new URL(API_BASE + path);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		InputStream in = null;
		OutputStream out = null;
		try {
			connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
			connection.setReadTimeout(READ_TIMEOUT_MS);
			connection.setRequestMethod(method);
			connection.setRequestProperty("Authorization", "Bearer " + apiToken);
			connection.setRequestProperty("Accept", "application/json");
			if (jsonBody != null) {
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				out = connection.getOutputStream();
				out.write(jsonBody.getBytes("UTF-8"));
				out.flush();
			}
			int code = connection.getResponseCode();
			in = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
			if (in == null) {
				throw new IOException("Todoist HTTP " + code + " (empty body) for " + path);
			}
			String response = readStream(in);
			if (code >= 400) {
				throw new IOException("Todoist HTTP " + code + " for " + path + ": " + response);
			}
			return response;
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
				}
			}
			connection.disconnect();
		}
	}

	private static String readStream(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = in.read(buffer)) >= 0) {
			sb.append(new String(buffer, 0, read, "UTF-8"));
		}
		return sb.toString();
	}
}
