package org.freeplane.plugin.workspace.model;

/**
 * Project roots that expose only the physical "My files" folder contents in the workspace tree.
 */
public interface IMyFilesTreeHoist {

	int getDisplayChildCount();

	AWorkspaceTreeNode getDisplayChildAt(int childIndex);

	int getDisplayChildIndex(javax.swing.tree.TreeNode node);
}
