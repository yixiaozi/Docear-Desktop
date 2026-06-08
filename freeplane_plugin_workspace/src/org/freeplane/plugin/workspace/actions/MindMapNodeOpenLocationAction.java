package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.freeplane.core.ui.AMultipleNodeAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.features.nodepins.NodeMindMapActionUtils;

@EnabledAction(checkOnNodeChange = true)
public class MindMapNodeOpenLocationAction extends AMultipleNodeAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.mindmap.node.open.location";

	public MindMapNodeOpenLocationAction() {
		super(KEY);
	}

	@Override
	protected void actionPerformed(final ActionEvent e, final NodeModel node) {
		final File file = NodeMindMapActionUtils.getMapFile(node);
		if (file != null) {
			MindMapOpenLocationAction.openContainingFolder(file);
		}
	}

	@Override
	public void setEnabled() {
		setEnabled(hasSavedMap());
	}

	private boolean hasSavedMap() {
		final Collection nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		for (final Iterator it = nodes.iterator(); it.hasNext();) {
			if (NodeMindMapActionUtils.isSavedMapNode((NodeModel) it.next())) {
				return true;
			}
		}
		return false;
	}
}
