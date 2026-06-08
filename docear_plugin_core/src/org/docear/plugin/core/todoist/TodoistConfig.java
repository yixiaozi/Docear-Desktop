package org.docear.plugin.core.todoist;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;

public final class TodoistConfig {
	public static final String PROP_API_TOKEN = "todoist.api_token";
	public static final String PROP_PROJECT_NAME = "todoist.project_name";
	public static final String PROP_LABEL = "todoist.label";
	public static final String DEFAULT_PROJECT_NAME = "Docear";
	public static final String DEFAULT_LABEL = "Docear";

	private TodoistConfig() {
	}

	public static void registerDefaults() {
		final ResourceController resources = ResourceController.getResourceController();
		resources.setDefaultProperty(PROP_API_TOKEN, "");
		resources.setDefaultProperty(PROP_PROJECT_NAME, DEFAULT_PROJECT_NAME);
		resources.setDefaultProperty(PROP_LABEL, DEFAULT_LABEL);
	}

	public static String getApiToken() {
		final ResourceController resources = ResourceController.getResourceController();
		String token = resources.getProperty(PROP_API_TOKEN, "").trim();
		if (token.length() > 0) {
			return token;
		}
		return loadTokenFromLocalFile();
	}

	public static void setApiToken(String token) {
		ResourceController.getResourceController().setProperty(PROP_API_TOKEN, token == null ? "" : token.trim());
		saveTokenToLocalFile(token);
	}

	public static String getLabel() {
		String label = ResourceController.getResourceController().getProperty(PROP_LABEL, DEFAULT_LABEL);
		if (label == null || label.trim().length() == 0) {
			return DEFAULT_LABEL;
		}
		return label.trim();
	}

	public static String getProjectName() {
		String name = ResourceController.getResourceController().getProperty(PROP_PROJECT_NAME, DEFAULT_PROJECT_NAME);
		if (name == null || name.trim().length() == 0) {
			return DEFAULT_PROJECT_NAME;
		}
		return name.trim();
	}

	public static void setProjectName(String name) {
		ResourceController.getResourceController().setProperty(PROP_PROJECT_NAME,
				name == null ? DEFAULT_PROJECT_NAME : name.trim());
	}

	private static File localPropertiesFile() {
		return new File(Compat.getApplicationUserDirectory(), "todoist.local.properties");
	}

	private static String loadTokenFromLocalFile() {
		File file = localPropertiesFile();
		if (!file.isFile()) {
			return "";
		}
		FileInputStream in = null;
		try {
			Properties props = new Properties();
			in = new FileInputStream(file);
			props.load(in);
			String token = props.getProperty("todoist.api_token", "").trim();
			if (token.length() == 0) {
				token = props.getProperty("key", "").trim();
			}
			if (token.length() > 0) {
				ResourceController.getResourceController().setProperty(PROP_API_TOKEN, token);
			}
			return token;
		}
		catch (IOException e) {
			LogUtils.warn("Todoist: could not read " + file.getPath(), e);
			return "";
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

	private static void saveTokenToLocalFile(String token) {
		if (token == null) {
			return;
		}
		token = token.trim();
		File file = localPropertiesFile();
		FileOutputStream out = null;
		try {
			Properties props = new Properties();
			if (file.isFile()) {
				FileInputStream in = new FileInputStream(file);
				try {
					props.load(in);
				}
				finally {
					in.close();
				}
			}
			props.setProperty("todoist.api_token", token);
			out = new FileOutputStream(file);
			props.store(out, "Todoist integration (local only, do not commit)");
		}
		catch (IOException e) {
			LogUtils.warn("Todoist: could not write " + file.getPath(), e);
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
}
