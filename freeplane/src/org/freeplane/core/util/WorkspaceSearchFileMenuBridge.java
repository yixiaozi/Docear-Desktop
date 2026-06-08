package org.freeplane.core.util;

import java.io.File;
import java.lang.reflect.Method;

import javax.swing.JPopupMenu;

/**
 * Invokes workspace favorite/tag menu helpers when the workspace plugin is loaded.
 */
public final class WorkspaceSearchFileMenuBridge {

	private static final String WORKSPACE_CONTROLLER =
	    "org.freeplane.plugin.workspace.WorkspaceController";

	private WorkspaceSearchFileMenuBridge() {
	}

	public static boolean appendFavoriteItems(final JPopupMenu menu, final File file) {
		if (menu == null || file == null) {
			return false;
		}
		try {
			final Class workspace = loadWorkspaceClass(WORKSPACE_CONTROLLER);
			final Method method = workspace.getMethod("appendSearchFileContextMenuItems",
			    new Class[] { JPopupMenu.class, File.class });
			final Object result = method.invoke(null, new Object[] { menu, file });
			return result instanceof Boolean && ((Boolean) result).booleanValue();
		}
		catch (final Exception e) {
			LogUtils.warn("Workspace search file menu unavailable: " + e.getMessage());
			return false;
		}
	}

	private static Class loadWorkspaceClass(final String className) throws ClassNotFoundException {
		final ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
		final ClassLoader bridgeLoader = WorkspaceSearchFileMenuBridge.class.getClassLoader();
		ClassNotFoundException last = null;
		if (contextLoader != null) {
			try {
				return Class.forName(className, true, contextLoader);
			}
			catch (final ClassNotFoundException e) {
				last = e;
			}
		}
		if (bridgeLoader != null && bridgeLoader != contextLoader) {
			try {
				return Class.forName(className, true, bridgeLoader);
			}
			catch (final ClassNotFoundException e) {
				last = e;
			}
		}
		try {
			return Class.forName(className);
		}
		catch (final ClassNotFoundException e) {
			if (last != null) {
				throw last;
			}
			throw e;
		}
	}
}
