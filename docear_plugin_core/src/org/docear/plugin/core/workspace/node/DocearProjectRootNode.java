package org.docear.plugin.core.workspace.node;

import javax.swing.tree.TreeNode;

import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.nodes.FolderLinkNode;
import org.freeplane.plugin.workspace.nodes.ProjectRootNode;

/**
 * Project root for customized Docear builds: hides default repository nodes in the
 * workspace tree while keeping them in the model for file/BibTeX support.
 */
public class DocearProjectRootNode extends ProjectRootNode {

	private static final long serialVersionUID = 1L;

	private static boolean isHiddenFromTree(AWorkspaceTreeNode n) {
		if (n == null) {
			return false;
		}
		if (n instanceof FolderTypeLibraryNode) {
			return true;
		}
		if (n instanceof FolderTypeLiteratureRepositoryNode) {
			return true;
		}
		if (n instanceof LinkTypeReferencesNode) {
			return true;
		}
		if (n instanceof FolderLinkNode) {
			String draftsLabel = TextUtils.getText("org.freeplane.plugin.workspace.nodes.folderlinknode.drafts.label", null);
			String name = n.getName();
			if (name != null && name.equals(draftsLabel)) {
				return true;
			}
		}
		return false;
	}

	public DocearProjectRootNode() {
		super();
	}

	@Override
	public AWorkspaceTreeNode getChildAt(int childIndex) {
		int n = super.getChildCount();
		int visible = 0;
		for (int i = 0; i < n; i++) {
			AWorkspaceTreeNode c = super.getChildAt(i);
			if (isHiddenFromTree(c)) {
				continue;
			}
			if (visible == childIndex) {
				return c;
			}
			visible++;
		}
		throw new ArrayIndexOutOfBoundsException("Index: " + childIndex);
	}

	@Override
	public int getChildCount() {
		int total = super.getChildCount();
		int hidden = 0;
		for (int i = 0; i < total; i++) {
			if (isHiddenFromTree(super.getChildAt(i))) {
				hidden++;
			}
		}
		return total - hidden;
	}

	@Override
	public int getIndex(TreeNode node) {
		int visible = 0;
		for (int i = 0; i < super.getChildCount(); i++) {
			AWorkspaceTreeNode c = super.getChildAt(i);
			if (isHiddenFromTree(c)) {
				continue;
			}
			if (c == node) {
				return visible;
			}
			visible++;
		}
		return -1;
	}

	@Override
	public AWorkspaceTreeNode clone() {
		DocearProjectRootNode node = new DocearProjectRootNode();
		return clone(node);
	}
}
