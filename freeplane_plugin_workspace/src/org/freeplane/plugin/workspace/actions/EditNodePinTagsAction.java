package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import org.freeplane.core.ui.AMultipleNodeAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.plugin.workspace.components.nodepins.EditNodePinTagsDialog;
import org.freeplane.plugin.workspace.features.nodepins.NodeMindMapActionUtils;

@EnabledAction(checkOnNodeChange = true)
public class EditNodePinTagsAction extends AMultipleNodeAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.nodepins.edit.tags";

	public EditNodePinTagsAction() {
		super(KEY);
	}

	@Override
	protected void actionPerformed(final ActionEvent e, final NodeModel node) {
		if (NodeMindMapActionUtils.isSavedMapNode(node)) {
			EditNodePinTagsDialog.showForNode(node);
		}
	}

	@Override
	public void setEnabled() {
		setEnabled(hasWritableNode());
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
