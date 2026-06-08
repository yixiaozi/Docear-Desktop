package org.freeplane.plugin.workspace.features.favorites;

import java.io.File;

import org.freeplane.plugin.workspace.URIUtils;
import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.nodes.DefaultFileNode;
import org.freeplane.plugin.workspace.nodes.LinkTypeFileNode;

public final class WorkspaceMindMapUtils {

	private WorkspaceMindMapUtils() {
	}

	public static boolean isWorkspaceFileNode(final AWorkspaceTreeNode node) {
		return getWorkspaceFile(node) != null;
	}

	public static File getWorkspaceFile(final AWorkspaceTreeNode node) {
		if (node instanceof DefaultFileNode) {
			final File file = ((DefaultFileNode) node).getFile();
			if (file != null && file.isFile()) {
				return file;
			}
		}
		else if (node instanceof LinkTypeFileNode) {
			final File file = URIUtils.getAbsoluteFile(((LinkTypeFileNode) node).getLinkURI());
			if (file != null && file.isFile()) {
				return file;
			}
		}
		return null;
	}

	public static String getWorkspaceFileUri(final AWorkspaceTreeNode node) {
		final File file = getWorkspaceFile(node);
		if (file == null) {
			return null;
		}
		return FavoriteUriUtils.toStoredUri(file, WorkspaceController.getSelectedProject(node));
	}

	public static boolean isWorkspaceFileUri(final String uri) {
		final File file = FavoriteUriUtils.resolveToFile(uri);
		return file != null && file.isFile();
	}

	public static boolean isMindMapNode(final AWorkspaceTreeNode node) {
		return getMindMapFile(node) != null;
	}

	public static File getMindMapFile(final AWorkspaceTreeNode node) {
		final File file = getWorkspaceFile(node);
		if (file != null && isMindMapFileName(file.getName())) {
			return file;
		}
		return null;
	}

	public static String getMindMapUri(final AWorkspaceTreeNode node) {
		return FavoriteUriUtils.toStoredUri(node);
	}

	public static boolean isMindMapFileName(final String name) {
		if (name == null) {
			return false;
		}
		final String lower = name.toLowerCase();
		return lower.endsWith(".mm") || lower.endsWith(".dcr");
	}

	public static boolean isMindMapUri(final String uri) {
		if (uri == null) {
			return false;
		}
		String name = uri;
		final int slash = Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('\\'));
		if (slash >= 0) {
			name = uri.substring(slash + 1);
		}
		final int query = name.indexOf('?');
		if (query >= 0) {
			name = name.substring(0, query);
		}
		return isMindMapFileName(name);
	}
}
