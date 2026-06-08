package org.docear.plugin.core.workspace.node;

import java.util.Enumeration;
import java.util.NoSuchElementException;

import javax.swing.tree.TreeNode;

import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;
import org.freeplane.plugin.workspace.model.IMyFilesTreeHoist;
import org.freeplane.plugin.workspace.nodes.FolderLinkNode;
import org.freeplane.plugin.workspace.nodes.FolderTypeMyFilesNode;
import org.freeplane.plugin.workspace.nodes.ProjectRootNode;

/**
 * Project root for customized Docear builds: hides default repository nodes in the
 * workspace tree while keeping them in the model for file/BibTeX support.
 * The tree shows only the contents of the "My files" folder.
 */
public class DocearProjectRootNode extends ProjectRootNode implements IMyFilesTreeHoist {

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

	private FolderTypeMyFilesNode findMyFilesNode() {
		for (int i = 0; i < getModelChildCount(); i++) {
			final AWorkspaceTreeNode child = getModelChildAt(i);
			if (child instanceof FolderTypeMyFilesNode) {
				return (FolderTypeMyFilesNode) child;
			}
		}
		return null;
	}

	public DocearProjectRootNode() {
		super();
	}

	public int getDisplayChildCount() {
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getChildCount();
		}
		return getChildCount();
	}

	public AWorkspaceTreeNode getDisplayChildAt(int childIndex) {
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getChildAt(childIndex);
		}
		return getChildAt(childIndex);
	}

	public int getDisplayChildIndex(TreeNode node) {
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getIndex(node);
		}
		return getIndex(node);
	}

	@Override
	public AWorkspaceTreeNode getChildAt(int childIndex) {
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getChildAt(childIndex);
		}
		int visible = 0;
		for (int i = 0; i < getModelChildCount(); i++) {
			final AWorkspaceTreeNode c = getModelChildAt(i);
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
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getChildCount();
		}
		int total = getModelChildCount();
		int hidden = 0;
		for (int i = 0; i < total; i++) {
			if (isHiddenFromTree(getModelChildAt(i))) {
				hidden++;
			}
		}
		return total - hidden;
	}

	@Override
	public int getIndex(TreeNode node) {
		final FolderTypeMyFilesNode myFiles = findMyFilesNode();
		if (myFiles != null) {
			return myFiles.getIndex(node);
		}
		int visible = 0;
		for (int i = 0; i < getModelChildCount(); i++) {
			final AWorkspaceTreeNode c = getModelChildAt(i);
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
	public Enumeration<AWorkspaceTreeNode> children() {
		final int count = getChildCount();
		return new Enumeration<AWorkspaceTreeNode>() {
			private int index = 0;

			public boolean hasMoreElements() {
				return index < count;
			}

			public AWorkspaceTreeNode nextElement() {
				if (index < count) {
					return getChildAt(index++);
				}
				throw new NoSuchElementException("DocearProjectRootNode Enumeration");
			}
		};
	}

	@Override
	public AWorkspaceTreeNode clone() {
		DocearProjectRootNode node = new DocearProjectRootNode();
		return clone(node);
	}
}
