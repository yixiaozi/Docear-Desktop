package org.freeplane.plugin.workspace;

import java.io.File;
import java.io.IOException;

import org.freeplane.core.util.LogUtils;

/**
 * Helpers for workspace paths on Windows (junctions / symlinks) and similar cases
 * where {@link File#listFiles()} or child resolution behaves more reliably on the
 * canonical target directory.
 */
public final class WorkspacePathUtils {

	private WorkspacePathUtils() {
	}

	/**
	 * If {@code raw} exists, returns {@link File#getCanonicalFile()} so junctions and
	 * directory symlinks resolve to their target; otherwise returns
	 * {@link File#getAbsoluteFile()}. On {@link IOException}, returns the absolute file.
	 */
	public static File resolveWorkspaceRootDirectory(final File raw) {
		if (raw == null) {
			return null;
		}
		final File absolute = raw.getAbsoluteFile();
		try {
			if (absolute.exists()) {
				return absolute.getCanonicalFile();
			}
		}
		catch (IOException e) {
			LogUtils.warn(e);
		}
		return absolute;
	}
}
