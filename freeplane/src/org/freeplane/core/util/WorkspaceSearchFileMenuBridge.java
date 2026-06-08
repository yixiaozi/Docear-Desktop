package org.freeplane.core.util;

import java.io.File;

import javax.swing.JPopupMenu;

/**
 * Bridge for file-search side tabs to append workspace favorite/tag menu items.
 * The workspace plugin registers a provider at startup (OSGi bundles cannot be loaded by reflection from core).
 */
public final class WorkspaceSearchFileMenuBridge {

	public interface Provider {
		boolean appendFavoriteItems(JPopupMenu menu, File file);
	}

	private static volatile Provider provider;

	private WorkspaceSearchFileMenuBridge() {
	}

	public static void setProvider(final Provider newProvider) {
		provider = newProvider;
	}

	public static boolean appendFavoriteItems(final JPopupMenu menu, final File file) {
		if (menu == null || file == null) {
			return false;
		}
		final Provider active = provider;
		if (active == null) {
			LogUtils.warn("Workspace search file menu provider is not registered");
			return false;
		}
		try {
			return active.appendFavoriteItems(menu, file);
		}
		catch (final Exception e) {
			LogUtils.warn("Workspace search file menu failed: " + e.getMessage());
			return false;
		}
	}
}
