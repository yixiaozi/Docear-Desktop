package org.freeplane.features.hashphotos.mindmapmode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 记录每个 dateKey 上次从 CSV 同步的标题（不写入导图，避免显示属性行）。
 */
public final class HashPhotosTitleStore {

	public static final String TITLES_FILE = ".hashphotos-titles.properties";

	private HashPhotosTitleStore() {
	}

	public static File getStoreFile(final File exportDir) {
		return new File(exportDir, TITLES_FILE);
	}

	public static Map load(final File exportDir) {
		final Map result = new HashMap();
		final File file = getStoreFile(exportDir);
		if (!file.isFile()) {
			return result;
		}
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				final int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				result.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
			}
		}
		catch (Exception e) {
			// ignore
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
		return result;
	}

	public static void save(final File exportDir, final Map titlesByDateKey) {
		final File file = getStoreFile(exportDir);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
			for (Object keyObj : titlesByDateKey.keySet()) {
				final String key = (String) keyObj;
				final String value = (String) titlesByDateKey.get(key);
				if (key == null || value == null) {
					continue;
				}
				writer.write(key);
				writer.write('=');
				writer.write(value);
				writer.write('\n');
			}
		}
		catch (Exception e) {
			// ignore
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (Exception e) {
					// ignore
				}
			}
		}
	}
}
