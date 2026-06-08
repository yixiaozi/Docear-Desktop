package org.freeplane.plugin.workspace.features.nodepins;

import java.io.File;

import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.workspace.features.favorites.FavoriteUriUtils;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;

public final class NodePinKeyUtils {

	private static final String KEY_SEPARATOR = "#";

	private NodePinKeyUtils() {
	}

	public static String fromNode(final NodeModel node) {
		if (node == null || node.getMap() == null) {
			return null;
		}
		final File mapFile = node.getMap().getFile();
		if (mapFile == null) {
			return null;
		}
		final AWorkspaceProject project = FavoriteUriUtils.findProjectForFile(mapFile);
		final String mapUri = FavoriteUriUtils.toStoredUri(mapFile, project);
		if (mapUri == null) {
			return null;
		}
		return mapUri + KEY_SEPARATOR + node.createID();
	}

	public static String parseMapUri(final String globalKey) {
		if (globalKey == null) {
			return null;
		}
		final int separator = globalKey.lastIndexOf(KEY_SEPARATOR);
		if (separator <= 0) {
			return null;
		}
		return globalKey.substring(0, separator);
	}

	public static String parseNodeId(final String globalKey) {
		if (globalKey == null) {
			return null;
		}
		final int separator = globalKey.lastIndexOf(KEY_SEPARATOR);
		if (separator < 0 || separator >= globalKey.length() - 1) {
			return null;
		}
		return globalKey.substring(separator + 1);
	}

	public static File resolveMapFile(final String globalKey) {
		return FavoriteUriUtils.resolveToFile(parseMapUri(globalKey));
	}

	public static String toRelativeStorageKey(final String globalKey, final AWorkspaceProject project) {
		if (globalKey == null || project == null) {
			return null;
		}
		final String mapUri = parseMapUri(globalKey);
		final String nodeId = parseNodeId(globalKey);
		final String relativePath = FavoriteUriUtils.toRelativePath(mapUri, project);
		if (relativePath == null || nodeId == null) {
			return null;
		}
		return relativePath + KEY_SEPARATOR + nodeId;
	}

	public static String toGlobalKey(final AWorkspaceProject project, final String relativeStorageKey) {
		if (project == null || relativeStorageKey == null) {
			return null;
		}
		final int separator = relativeStorageKey.lastIndexOf(KEY_SEPARATOR);
		if (separator <= 0) {
			return null;
		}
		final String relativePath = relativeStorageKey.substring(0, separator);
		final String nodeId = relativeStorageKey.substring(separator + 1);
		final String mapUri = FavoriteUriUtils.fromRelativePath(project, relativePath);
		if (mapUri == null) {
			return null;
		}
		return mapUri + KEY_SEPARATOR + nodeId;
	}
}
