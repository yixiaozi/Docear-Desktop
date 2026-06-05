package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;

import javax.swing.tree.TreePath;

import org.freeplane.plugin.workspace.components.favorites.EditTagsDialog;
import org.freeplane.plugin.workspace.components.menu.CheckEnableOnPopup;
import org.freeplane.plugin.workspace.features.favorites.WorkspaceMindMapUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

@CheckEnableOnPopup
public class EditFavoriteTagsAction extends AWorkspaceAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.favorites.edit.tags";

	public EditFavoriteTagsAction() {
		super(KEY);
	}

	@Override
	public void setEnabledFor(final AWorkspaceTreeNode node, final TreePath[] selectedPaths) {
		setEnabled(WorkspaceMindMapUtils.isMindMapNode(node));
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final AWorkspaceTreeNode node = getNodeFromActionEvent(e);
		final String uri = WorkspaceMindMapUtils.getMindMapUri(node);
		if (uri != null) {
			EditTagsDialog.showForUri(uri);
		}
	}
}
