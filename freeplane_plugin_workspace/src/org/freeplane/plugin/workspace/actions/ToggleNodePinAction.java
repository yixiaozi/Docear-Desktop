package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import org.freeplane.core.ui.AMultipleNodeAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.core.ui.SelectableAction;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.features.nodepins.NodeDetailsTagService;
import org.freeplane.plugin.workspace.features.nodepins.NodeMindMapActionUtils;

@SelectableAction(checkOnNodeChange = true, checkOnPopup = true)
@EnabledAction(checkOnNodeChange = true)
public class ToggleNodePinAction extends AMultipleNodeAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.nodepins.toggle";

	public ToggleNodePinAction() {
		super(KEY);
	}

	@Override
	protected void actionPerformed(final ActionEvent e, final NodeModel node) {
		if (NodeMindMapActionUtils.isSavedMapNode(node)) {
			NodeDetailsTagService.togglePin(node);
		}
	}

	@Override
	public void setEnabled() {
		setEnabled(hasWritableNode());
	}

	@Override
	public void setSelected() {
		boolean pinned = false;
		final Collection nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		for (final Iterator it = nodes.iterator(); it.hasNext();) {
			final NodeModel node = (NodeModel) it.next();
			if (NodeMindMapActionUtils.isSavedMapNode(node) && NodeDetailsTagService.isPinned(node)) {
				pinned = true;
				break;
			}
		}
		setSelected(pinned);
	}

	private boolean hasWritableNode() {
		final Collection nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		for (final Iterator it = nodes.iterator(); it.hasNext();) {
			if (NodeMindMapActionUtils.isSavedMapNode((NodeModel) it.next())) {
				return true;
			}
		}
		return false;
	}
}
