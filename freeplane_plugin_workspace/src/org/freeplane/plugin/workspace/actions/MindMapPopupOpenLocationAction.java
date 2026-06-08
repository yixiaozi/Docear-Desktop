package org.freeplane.plugin.workspace.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.SelectableAction;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;

@SelectableAction(checkOnPopup = true)
public class MindMapPopupOpenLocationAction extends AFreeplaneAction {

	private static final long serialVersionUID = 1L;
	public static final String KEY = "workspace.action.mindmap.popup.open.location";

	public MindMapPopupOpenLocationAction() {
		super(KEY);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final MapModel map = Controller.getCurrentController().getMap();
		if (map == null) {
			return;
		}
		final File file = map.getFile();
		if (file != null) {
			MindMapOpenLocationAction.openContainingFolder(file);
		}
	}

	@Override
	public void setEnabled() {
		final MapModel map = Controller.getCurrentController().getMap();
		setEnabled(map != null && map.getFile() != null);
	}
}
