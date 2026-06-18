package org.freeplane.features.icon.mindmapmode;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

import org.freeplane.core.ui.AMultipleNodeAction;
import org.freeplane.core.ui.EnabledAction;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconStore;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.icon.factory.MindIconFactory;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;

@EnabledAction(checkOnNodeChange = true)
abstract class ToggleBuiltinIconAction extends AMultipleNodeAction {
	private static final long serialVersionUID = 1L;
	private final String iconName;

	protected ToggleBuiltinIconAction(final String key, final String iconName) {
		super(key);
		this.iconName = iconName;
	}

	@Override
	protected void actionPerformed(final ActionEvent e, final NodeModel node) {
		final MindIcon mindIcon = resolveIcon(iconName);
		final MIconController iconController = (MIconController) IconController.getController();
		final int index = findIconIndex(node, iconName);
		if (index >= 0) {
			iconController.removeIcon(node, index);
		}
		else {
			iconController.addIcon(node, mindIcon);
		}
	}

	static MindIcon resolveIcon(final String iconName) {
		final IconStore store = IconStoreFactory.create();
		final MindIcon icon = store.getMindIcon(iconName);
		if (icon != null && iconName.equals(icon.getName())) {
			return icon;
		}
		return MindIconFactory.create(iconName);
	}

	private static int findIconIndex(final NodeModel node, final String iconName) {
		final List icons = node.getIcons();
		for (int i = 0; i < icons.size(); i++) {
			if (iconName.equals(((MindIcon) icons.get(i)).getName())) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public void setEnabled() {
		final ModeController modeController = Controller.getCurrentModeController();
		if (modeController == null || !modeController.canEdit()) {
			setEnabled(false);
			return;
		}
		final Collection selected = modeController.getMapController().getSelectedNodes();
		setEnabled(selected != null && !selected.isEmpty());
	}
}
