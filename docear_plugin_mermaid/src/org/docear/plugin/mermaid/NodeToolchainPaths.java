package org.docear.plugin.mermaid;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Locate {@code node} / {@code npx} / {@code npm} on PATH and common install dirs. */
final class NodeToolchainPaths {

	private NodeToolchainPaths() {
	}

	static String findNode() {
		return findExecutable(isWindows() ? "node.exe" : "node", "node");
	}

	static String findNpx() {
		if (isWindows()) {
			final String npx = findExecutable("npx.cmd", "npx.exe", "npx");
			if (npx != null) {
				return npx;
			}
		}
		return findExecutable("npx");
	}

	static String findNpm() {
		if (isWindows()) {
			final String npm = findExecutable("npm.cmd", "npm.exe", "npm");
			if (npm != null) {
				return npm;
			}
		}
		return findExecutable("npm");
	}

	private static String findExecutable(final String... names) {
		final List<String> dirs = collectSearchDirs();
		for (int n = 0; n < names.length; n++) {
			for (int d = 0; d < dirs.size(); d++) {
				final File f = new File(dirs.get(d), names[n]);
				if (f.isFile() && f.canExecute()) {
					return f.getAbsolutePath();
				}
			}
		}
		return null;
	}

	private static List<String> collectSearchDirs() {
		final List<String> dirs = new ArrayList<String>();
		final String pathEnv = System.getenv("PATH");
		if (pathEnv != null) {
			for (final String part : pathEnv.split(File.pathSeparator)) {
				if (part != null && part.length() > 0) {
					dirs.add(part);
				}
			}
		}
		if (isWindows()) {
			final String pf = System.getenv("ProgramFiles");
			if (pf != null) {
				dirs.add(pf + "\\nodejs");
			}
			final String pf86 = System.getenv("ProgramFiles(x86)");
			if (pf86 != null) {
				dirs.add(pf86 + "\\nodejs");
			}
			final String appData = System.getenv("APPDATA");
			if (appData != null) {
				dirs.add(appData + "\\npm");
			}
		}
		else {
			dirs.add("/opt/homebrew/bin");
			dirs.add("/usr/local/bin");
			dirs.add("/usr/bin");
			dirs.add(System.getProperty("user.home", "") + "/.nvm/current/bin");
		}
		return dirs;
	}

	static boolean isWindows() {
		return System.getProperty("os.name", "").toLowerCase().indexOf("win") >= 0;
	}
}
