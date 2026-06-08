package org.freeplane.plugin.workspace.features.favorites;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.actions.EditFavoriteTagsAction;
import org.freeplane.plugin.workspace.actions.MindMapOpenLocationAction;
import org.freeplane.plugin.workspace.actions.ToggleFavoriteAction;
import org.freeplane.plugin.workspace.components.favorites.EditTagsDialog;
import org.freeplane.plugin.workspace.model.project.AWorkspaceProject;

/**
 * Context menu items (favorite, tags, open folder) for file lists outside the workspace tree.
 */
public final class SearchFileContextMenuHelper {

	private SearchFileContextMenuHelper() {
	}

	public static void showContextMenu(final JComponent invoker, final File file, final int x, final int y) {
		if (invoker == null || file == null || !file.isFile()) {
			return;
		}
		final JPopupMenu menu = new JPopupMenu();
		if (appendFavoriteItems(menu, file)) {
			menu.show(invoker, x, y);
		}
	}

	public static boolean appendFavoriteItems(final JPopupMenu menu, final File file) {
		if (menu == null || file == null || !file.isFile() || !file.exists()) {
			return false;
		}
		final String uri = resolveStoredUri(file);
		if (uri == null) {
			return false;
		}
		final FavoritesAndTagsStore store = FavoritesAndTagsStore.getInstance();
		final JCheckBoxMenuItem favoriteItem = new JCheckBoxMenuItem(
		    TextUtils.getText(ToggleFavoriteAction.KEY + ".label"));
		favoriteItem.setSelected(store.isFavorite(uri));
		favoriteItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				store.toggleFavorite(uri);
			}
		});
		menu.add(favoriteItem);

		final JMenuItem editTagsItem = new JMenuItem(TextUtils.getText(EditFavoriteTagsAction.KEY + ".label"));
		editTagsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				EditTagsDialog.showForUri(uri);
			}
		});
		menu.add(editTagsItem);

		final JMenuItem openFolderItem = new JMenuItem(TextUtils.getText(MindMapOpenLocationAction.KEY + ".label"));
		openFolderItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				MindMapOpenLocationAction.openContainingFolder(file);
			}
		});
		menu.add(openFolderItem);
		return true;
	}

	private static String resolveStoredUri(final File file) {
		final AWorkspaceProject project = FavoriteUriUtils.findProjectForFile(file);
		return FavoriteUriUtils.toStoredUri(file, project);
	}
}
