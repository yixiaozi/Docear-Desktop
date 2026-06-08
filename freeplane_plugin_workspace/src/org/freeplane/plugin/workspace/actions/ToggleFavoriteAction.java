package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;

import javax.swing.tree.TreePath;

import org.freeplane.core.ui.SelectableAction;
import org.freeplane.plugin.workspace.components.menu.CheckEnableOnPopup;
import org.freeplane.plugin.workspace.features.favorites.FavoritesAndTagsStore;
import org.freeplane.plugin.workspace.features.favorites.WorkspaceMindMapUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

@CheckEnableOnPopup
@SelectableAction(checkOnPopup = true)
public class ToggleFavoriteAction extends AWorkspaceAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.favorites.toggle";

	public ToggleFavoriteAction() {
		super(KEY);
	}

	@Override
	public void setEnabledFor(final AWorkspaceTreeNode node, final TreePath[] selectedPaths) {
		setEnabled(WorkspaceMindMapUtils.isWorkspaceFileNode(node));
	}

	@Override
	public void setSelectedFor(final AWorkspaceTreeNode node, final TreePath[] selectedPaths) {
		final String uri = WorkspaceMindMapUtils.getWorkspaceFileUri(node);
		setSelected(uri != null && FavoritesAndTagsStore.getInstance().isFavorite(uri));
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final AWorkspaceTreeNode node = getNodeFromActionEvent(e);
		final String uri = WorkspaceMindMapUtils.getWorkspaceFileUri(node);
		if (uri == null) {
			return;
		}
		FavoritesAndTagsStore.getInstance().toggleFavorite(uri);
	}

}
