package org.freeplane.plugin.workspace.components.favorites;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.TransferHandler;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mapio.MapIO;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.dnd.WorkspaceTransferable;
import org.freeplane.plugin.workspace.features.favorites.FavoriteEntry;
import org.freeplane.plugin.workspace.features.favorites.FavoriteUriUtils;
import org.freeplane.plugin.workspace.features.favorites.FavoritesAndTagsStore;
import org.freeplane.plugin.workspace.features.favorites.WorkspaceMindMapUtils;

public class FavoritesTabPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final DataFlavor REORDER_FLAVOR = DataFlavor.stringFlavor;

	private final FavoritesAndTagsStore store = FavoritesAndTagsStore.getInstance();
	private final DefaultListModel listModel = new DefaultListModel();
	private final JList favoritesList = new JList(listModel);
	private final JPanel tagFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
	private final JLabel statusLabel = new JLabel();
	private final Runnable refreshListener = new Runnable() {
		public void run() {
			refreshView();
		}
	};

	private String activeTagFilter = null;
	private int draggedIndex = -1;
	private int dropIndex = -1;

	public FavoritesTabPanel() {
		super(new BorderLayout(0, 4));
		setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		buildTagFilterPanel();
		buildFavoritesList();
		statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 0, 2));
		add(tagFilterPanel, BorderLayout.NORTH);
		add(new JScrollPane(favoritesList), BorderLayout.CENTER);
		add(statusLabel, BorderLayout.SOUTH);
		store.addChangeListener(refreshListener);
		refreshView();
	}

	private void buildTagFilterPanel() {
		tagFilterPanel.setBorder(BorderFactory.createTitledBorder(TextUtils.getText("workspace.favorites.filter.label")));
		rebuildTagButtons();
	}

	private void rebuildTagButtons() {
		tagFilterPanel.removeAll();
		tagFilterPanel.add(createTagButton(null, TextUtils.getText("workspace.favorites.filter.all")));
		for (final Iterator it = getAvailableTags().iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			tagFilterPanel.add(createTagButton(tag, tag));
		}
		tagFilterPanel.revalidate();
		tagFilterPanel.repaint();
	}

	private Set getAvailableTags() {
		return store.getQuickSelectTags();
	}

	private JToggleButton createTagButton(final String tag, final String label) {
		final JToggleButton button = new JToggleButton(label);
		button.setSelected(tag == null ? activeTagFilter == null : tag.equals(activeTagFilter));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				activeTagFilter = tag;
				rebuildTagButtons();
				refreshList();
			}
		});
		return button;
	}

	private void buildFavoritesList() {
		favoritesList.setCellRenderer(new FavoriteEntryRenderer());
		favoritesList.setDragEnabled(true);
		favoritesList.setTransferHandler(new FavoritesReorderTransferHandler());
		favoritesList.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(final MouseEvent e) {
				updateDropIndex(e);
			}

			public void mouseMoved(final MouseEvent e) {
				updateDropIndex(e);
			}
		});
		favoritesList.addMouseListener(new MouseAdapter() {
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() == 2) {
					openSelectedFavorite();
				}
				else if (e.isPopupTrigger() || (e.getButton() == MouseEvent.BUTTON3 && e.getClickCount() == 1)) {
					showPopup(e);
				}
			}

			public void mousePressed(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopup(e);
				}
			}

			public void mouseReleased(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopup(e);
				}
			}
		});
	}

	private void updateDropIndex(final MouseEvent e) {
		dropIndex = favoritesList.locationToIndex(e.getPoint());
		if (dropIndex < 0) {
			dropIndex = listModel.size();
		}
	}

	private void showPopup(final MouseEvent e) {
		final int index = favoritesList.locationToIndex(e.getPoint());
		if (index < 0) {
			return;
		}
		favoritesList.setSelectedIndex(index);
		final FavoriteEntry entry = (FavoriteEntry) listModel.getElementAt(index);
		final JPopupMenu popup = new JPopupMenu();
		final JMenuItem openItem = new JMenuItem(TextUtils.getText("workspace.favorites.action.open"));
		openItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				openFavorite(entry);
			}
		});
		popup.add(openItem);
		final JMenuItem editTagsItem = new JMenuItem(TextUtils.getText("workspace.action.favorites.edit.tags.label"));
		editTagsItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				EditTagsDialog.showForUri(entry.getUri());
			}
		});
		popup.add(editTagsItem);
		popup.addSeparator();
		final JMenuItem removeItem = new JMenuItem(TextUtils.getText("workspace.action.favorites.remove.label"));
		removeItem.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				store.removeFavorite(entry.getUri());
				persist();
			}
		});
		popup.add(removeItem);
		popup.show(favoritesList, e.getX(), e.getY());
	}

	private void openSelectedFavorite() {
		final FavoriteEntry entry = (FavoriteEntry) favoritesList.getSelectedValue();
		if (entry != null) {
			openFavorite(entry);
		}
	}

	private void openFavorite(final FavoriteEntry entry) {
		if (entry == null) {
			return;
		}
		final File file = entry.getFile();
		if (file == null || !file.exists()) {
			statusLabel.setText(TextUtils.format("workspace.favorites.missing.text", entry.getDisplayName()));
			return;
		}
		try {
			final URL mapUrl = Compat.fileToUrl(file);
			final MapIO mapIO = (MapIO) Controller.getCurrentModeController().getExtension(MapIO.class);
			mapIO.newMap(mapUrl);
		}
		catch (final Exception e) {
			LogUtils.severe(e);
			statusLabel.setText(TextUtils.format("workspace.favorites.open.error.text", entry.getDisplayName()));
		}
	}

	public void refreshView() {
		store.reloadIfChanged();
		rebuildTagButtons();
		refreshList();
	}

	private void refreshList() {
		listModel.clear();
		final List favorites = store.getFavorites();
		for (int i = 0; i < favorites.size(); i++) {
			final FavoriteEntry entry = (FavoriteEntry) favorites.get(i);
			if (matchesTagFilter(entry)) {
				listModel.addElement(entry);
			}
		}
		statusLabel.setText(TextUtils.format("workspace.favorites.count.text", String.valueOf(listModel.size())));
	}

	private boolean matchesTagFilter(final FavoriteEntry entry) {
		if (activeTagFilter == null || activeTagFilter.length() == 0) {
			return true;
		}
		return entry.getTags().contains(activeTagFilter);
	}

	private void persist() {
		store.saveAllProjects();
	}

	private class FavoriteEntryRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		public Component getListCellRendererComponent(final JList list, final Object value, final int index,
				final boolean isSelected, final boolean cellHasFocus) {
			final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FavoriteEntry) {
				final FavoriteEntry entry = (FavoriteEntry) value;
				label.setText(formatEntryLabel(entry));
				if (!entry.exists()) {
					label.setForeground(Color.GRAY);
				}
			}
			return label;
		}

		private String formatEntryLabel(final FavoriteEntry entry) {
			final StringBuilder builder = new StringBuilder(entry.getDisplayName());
			final File file = entry.getFile();
			if (file != null && file.getParentFile() != null) {
				builder.append(" \u2014 ").append(file.getParentFile().getName());
			}
			if (!entry.getTags().isEmpty()) {
				builder.append("  [");
				boolean first = true;
				for (final String tag : entry.getTags()) {
					if (!first) {
						builder.append(", ");
					}
					builder.append(tag);
					first = false;
				}
				builder.append(']');
			}
			return builder.toString();
		}
	}

	private class FavoritesReorderTransferHandler extends TransferHandler {

		private static final long serialVersionUID = 1L;

		public int getSourceActions(final JComponent c) {
			return MOVE;
		}

		protected Transferable createTransferable(final JComponent c) {
			final JList list = (JList) c;
			draggedIndex = list.getSelectedIndex();
			return new StringSelection("favorite-reorder");
		}

		public boolean canImport(final JComponent comp, final DataFlavor[] transferFlavors) {
			if (comp != favoritesList) {
				return false;
			}
			return hasFlavor(transferFlavors, REORDER_FLAVOR)
					|| hasFlavor(transferFlavors, WorkspaceTransferable.WORKSPACE_URI_LIST_FLAVOR)
					|| hasFlavor(transferFlavors, WorkspaceTransferable.WORKSPACE_FILE_LIST_FLAVOR);
		}

		public boolean importData(final JComponent comp, final Transferable transferable) {
			try {
				if (hasTransferableFlavor(transferable, REORDER_FLAVOR) && draggedIndex >= 0) {
					final int targetIndex = dropIndex >= 0 ? dropIndex : listModel.size();
					reorderEntry((FavoriteEntry) listModel.getElementAt(draggedIndex), targetIndex);
					return true;
				}
				final String uri = FavoriteUriUtils.normalizeToStoredUri(extractUriFromTransfer(transferable));
				if (uri != null && WorkspaceMindMapUtils.isMindMapUri(uri)) {
					store.addFavorite(uri);
					persist();
					return true;
				}
			}
			catch (final Exception e) {
				LogUtils.warn(e);
			}
			return false;
		}

		protected void exportDone(final JComponent source, final Transferable data, final int action) {
			draggedIndex = -1;
			dropIndex = -1;
		}
	}

	private void reorderEntry(final FavoriteEntry entry, final int filteredDropIndex) {
		if (entry == null) {
			return;
		}
		final List allEntries = store.getFavorites();
		int fromIndex = -1;
		for (int i = 0; i < allEntries.size(); i++) {
			if (((FavoriteEntry) allEntries.get(i)).getUri().equals(entry.getUri())) {
				fromIndex = i;
				break;
			}
		}
		if (fromIndex < 0) {
			return;
		}
		int toIndex = allEntries.size();
		if (filteredDropIndex >= 0 && filteredDropIndex < listModel.size()) {
			final FavoriteEntry targetEntry = (FavoriteEntry) listModel.getElementAt(filteredDropIndex);
			for (int i = 0; i < allEntries.size(); i++) {
				if (((FavoriteEntry) allEntries.get(i)).getUri().equals(targetEntry.getUri())) {
					toIndex = i;
					break;
				}
			}
		}
		store.reorder(fromIndex, toIndex);
		persist();
	}

	private static boolean hasFlavor(final DataFlavor[] flavors, final DataFlavor flavor) {
		for (int i = 0; i < flavors.length; i++) {
			if (flavor.equals(flavors[i])) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTransferableFlavor(final Transferable transferable, final DataFlavor flavor) {
		return transferable.isDataFlavorSupported(flavor);
	}

	private String extractUriFromTransfer(final Transferable transferable) throws Exception {
		if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_URI_LIST_FLAVOR)) {
			return (String) transferable.getTransferData(WorkspaceTransferable.WORKSPACE_URI_LIST_FLAVOR);
		}
		if (transferable.isDataFlavorSupported(WorkspaceTransferable.WORKSPACE_FILE_LIST_FLAVOR)) {
			final List files = (List) transferable.getTransferData(WorkspaceTransferable.WORKSPACE_FILE_LIST_FLAVOR);
			if (files != null && !files.isEmpty()) {
				return ((File) files.get(0)).toURI().toString();
			}
		}
		return null;
	}
}
