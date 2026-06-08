package org.freeplane.plugin.workspace.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.freeplane.plugin.workspace.io.IFileSystemRepresentation;
import org.freeplane.plugin.workspace.nodes.FolderFileNode;
import org.freeplane.plugin.workspace.nodes.FolderLinkNode;
import org.freeplane.plugin.workspace.nodes.FolderTypeMyFilesNode;
import org.freeplane.plugin.workspace.nodes.ProjectRootNode;

/**
 * Resolves workspace tree children for project roots: show only the contents of
 * the "My files" folder and hide repository/Case siblings.
 */
public final class MyFilesTreeDisplayHelper {

	private MyFilesTreeDisplayHelper() {
	}

	public static boolean isProjectRoot(final AWorkspaceTreeNode node) {
		if (node instanceof ProjectRootNode) {
			return true;
		}
		return node != null
		    && "org.docear.plugin.core.workspace.node.DocearProjectRootNode".equals(node.getClass().getName());
	}

	public static List<AWorkspaceTreeNode> getDisplayChildren(final AWorkspaceTreeNode projectRoot) {
		if (!isProjectRoot(projectRoot)) {
			return null;
		}
		if (projectRoot instanceof IMyFilesTreeHoist) {
			final IMyFilesTreeHoist hoist = (IMyFilesTreeHoist) projectRoot;
			final List<AWorkspaceTreeNode> children = new ArrayList<AWorkspaceTreeNode>();
			for (int i = 0; i < hoist.getDisplayChildCount(); i++) {
				final AWorkspaceTreeNode child = hoist.getDisplayChildAt(i);
				if (!isHiddenWorkspaceFolder(child)) {
					children.add(child);
				}
			}
			return children;
		}
		final FolderTypeMyFilesNode myFiles = findMyFilesNode(projectRoot);
		if (myFiles == null) {
			return null;
		}
		final List<AWorkspaceTreeNode> children = new ArrayList<AWorkspaceTreeNode>();
		for (int i = 0; i < myFiles.getModelChildCount(); i++) {
			final AWorkspaceTreeNode child = myFiles.getModelChildAt(i);
			if (!isHiddenWorkspaceFolder(child)) {
				children.add(child);
			}
		}
		return children;
	}

	public static boolean isHiddenWorkspaceFolder(final AWorkspaceTreeNode node) {
		if (node == null) {
			return false;
		}
		if (node instanceof FolderFileNode || node instanceof FolderLinkNode) {
			final File file = node instanceof IFileSystemRepresentation
			    ? ((IFileSystemRepresentation) node).getFile() : null;
			if (file != null && file.isDirectory() && file.getName().startsWith("_")) {
				return true;
			}
		}
		final String name = node.getName();
		return name != null && name.startsWith("_");
	}

	public static FolderTypeMyFilesNode findMyFilesNode(final AWorkspaceTreeNode parent) {
		if (parent == null) {
			return null;
		}
		for (int i = 0; i < parent.getModelChildCount(); i++) {
			final AWorkspaceTreeNode child = parent.getModelChildAt(i);
			if (child instanceof FolderTypeMyFilesNode) {
				return (FolderTypeMyFilesNode) child;
			}
		}
		return null;
	}
}
