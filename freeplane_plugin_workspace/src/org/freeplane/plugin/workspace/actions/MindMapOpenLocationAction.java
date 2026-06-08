package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.tree.TreePath;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.components.menu.CheckEnableOnPopup;
import org.freeplane.plugin.workspace.features.favorites.WorkspaceMindMapUtils;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

@CheckEnableOnPopup
public class MindMapOpenLocationAction extends AWorkspaceAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.mindmap.open.location";

	public MindMapOpenLocationAction() {
		super(KEY);
	}

	@Override
	public void setEnabledFor(final AWorkspaceTreeNode node, final TreePath[] selectedPaths) {
		setEnabled(WorkspaceMindMapUtils.isWorkspaceFileNode(node));
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final AWorkspaceTreeNode node = getNodeFromActionEvent(e);
		final File file = WorkspaceMindMapUtils.getWorkspaceFile(node);
		if (file == null) {
			return;
		}
		openContainingFolder(file);
	}

	public static void openContainingFolder(final File file) {
		if (file == null) {
			return;
		}
		File folder = file.getParentFile();
		if (folder == null || !folder.exists()) {
			folder = file;
		}
		try {
			Controller.getCurrentController().getViewController().openDocument(Compat.fileToUrl(folder));
		}
		catch (final Exception ex) {
			LogUtils.warn(ex);
		}
	}
}
