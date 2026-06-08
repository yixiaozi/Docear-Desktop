package org.docear.plugin.core.todoist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;

final class TodoistMappingStore {
	private static final String FILE_NAME = "todoist-sync-map.properties";

	private final File storeFile;
	private final Map mappings = new HashMap();

	TodoistMappingStore() {
		storeFile = new File(Compat.getApplicationUserDirectory(), FILE_NAME);
		load();
	}

	String getTaskId(String syncKey) {
		return (String) mappings.get(syncKey);
	}

	void putMapping(String syncKey, String taskId, long remindAt, String contentHash) {
		mappings.put(syncKey, taskId + "|" + remindAt + "|" + contentHash);
	}

	void removeMapping(String syncKey) {
		mappings.remove(syncKey);
	}

	String getStoredRemindAt(String syncKey) {
		String value = (String) mappings.get(syncKey);
		if (value == null) {
			return null;
		}
		int first = value.indexOf('|');
		int second = value.indexOf('|', first + 1);
		if (first < 0 || second < 0) {
			return null;
		}
		return value.substring(first + 1, second);
	}

	String getStoredContentHash(String syncKey) {
		String value = (String) mappings.get(syncKey);
		if (value == null) {
			return null;
		}
		int second = value.lastIndexOf('|');
		if (second < 0) {
			return null;
		}
		return value.substring(second + 1);
	}

	String getTaskIdOnly(String syncKey) {
		String value = (String) mappings.get(syncKey);
		if (value == null) {
			return null;
		}
		int first = value.indexOf('|');
		if (first < 0) {
			return value;
		}
		return value.substring(0, first);
	}

	Set keySet() {
		return mappings.keySet();
	}

	java.util.Set getAllMappedTaskIds() {
		java.util.Set ids = new java.util.HashSet();
		for (Iterator it = mappings.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			String taskId = getTaskIdOnly(key);
			if (taskId != null && taskId.length() > 0) {
				ids.add(taskId);
			}
		}
		return ids;
	}

	void save() {
		OutputStream out = null;
		try {
			out = new FileOutputStream(storeFile);
			Writer writer = new OutputStreamWriter(out, "UTF-8");
			for (Iterator it = mappings.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				writer.write(escape((String) entry.getKey()));
				writer.write('=');
				writer.write(escape((String) entry.getValue()));
				writer.write('\n');
			}
			writer.flush();
		}
		catch (IOException e) {
			LogUtils.warn("Todoist: could not save mapping store", e);
		}
		finally {
			if (out != null) {
				try {
					out.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	private void load() {
		if (!storeFile.isFile()) {
			return;
		}
		InputStream in = null;
		try {
			in = new FileInputStream(storeFile);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				mappings.put(unescape(line.substring(0, eq)), unescape(line.substring(eq + 1)));
			}
		}
		catch (IOException e) {
			LogUtils.warn("Todoist: could not load mapping store", e);
		}
		finally {
			if (in != null) {
				try {
					in.close();
				}
				catch (IOException e) {
				}
			}
		}
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
	}

	private static String unescape(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		boolean esc = false;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (esc) {
				if (c == 'n') {
					sb.append('\n');
				}
				else {
					sb.append(c);
				}
				esc = false;
			}
			else if (c == '\\') {
				esc = true;
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
}
