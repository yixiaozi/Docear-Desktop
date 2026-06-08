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

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;

final class TodoistSectionStore {
	private static final String FILE_NAME = "todoist-section-map.properties";

	private final Map mappings = new HashMap();

	TodoistSectionStore() {
		load();
	}

	String getSectionId(String projectId, String sectionName) {
		return (String) mappings.get(key(projectId, sectionName));
	}

	void putSectionId(String projectId, String sectionName, String sectionId) {
		mappings.put(key(projectId, sectionName), sectionId);
	}

	void save() {
		OutputStream out = null;
		try {
			out = new FileOutputStream(new File(Compat.getApplicationUserDirectory(), FILE_NAME));
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
			LogUtils.warn("Todoist: could not save section map", e);
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

	private static String key(String projectId, String sectionName) {
		return projectId + "|" + sectionName;
	}

	private void load() {
		File file = new File(Compat.getApplicationUserDirectory(), FILE_NAME);
		if (!file.isFile()) {
			return;
		}
		InputStream in = null;
		try {
			in = new FileInputStream(file);
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
			LogUtils.warn("Todoist: could not load section map", e);
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
				sb.append(c == 'n' ? '\n' : c);
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
