package org.freeplane.features.encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;

public final class EncryptionConfig {
	public static final String PROP_ENCRYPT_PASSWORD = "encryption.password";

	private EncryptionConfig() {
	}

	public static String getPassword() {
		File file = localPropertiesFile();
		if (!file.isFile()) {
			return "";
		}
		FileInputStream in = null;
		try {
			Properties props = new Properties();
			in = new FileInputStream(file);
			props.load(in);
			return props.getProperty(PROP_ENCRYPT_PASSWORD, "").trim();
		}
		catch (IOException e) {
			LogUtils.warn("Encryption: could not read " + file.getPath(), e);
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

	public static void setPassword(String password) {
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
			props.setProperty(PROP_ENCRYPT_PASSWORD, password == null ? "" : password);
			out = new FileOutputStream(file);
			props.store(out, "Freeplane encryption settings (local only)");
		}
		catch (IOException e) {
			LogUtils.warn("Encryption: could not write " + file.getPath(), e);
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

	public static boolean hasPassword() {
		return getPassword().length() > 0;
	}

	private static File localPropertiesFile() {
		return new File(Compat.getApplicationUserDirectory(), "encryption.local.properties");
	}
}