/*
 *  Docear - mind map editor extension
 */
package org.freeplane.features.url.mindmapmode;

import java.awt.EventQueue;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.features.mode.Controller;

/**
 * Reloads a background mind map when the user switches to it after an external file change.
 */
public class ExternalModificationMapSelectionListener implements IMapSelectionListener {
	private final MMapController mapController;

	public ExternalModificationMapSelectionListener(final MMapController mapController) {
		this.mapController = mapController;
	}

	public void beforeMapChange(final MapModel oldMap, final MapModel newMap) {
	}

	public void afterMapChange(final MapModel oldMap, final MapModel newMap) {
		if (!(newMap instanceof MMapModel)) {
			return;
		}
		final MMapModel mapModel = (MMapModel) newMap;
		if (!mapModel.isPendingExternalReload() || mapModel.getNumberOfChangesSinceLastSave() != 0) {
			return;
		}
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					if (Controller.getCurrentController().getMap() != mapModel) {
						return;
					}
					if (!mapModel.isPendingExternalReload()) {
						return;
					}
					mapModel.setPendingExternalReload(false);
					mapController.restoreCurrentMapPreservingSelection();
				}
				catch (Exception e) {
					LogUtils.warn(e);
					mapModel.setPendingExternalReload(false);
				}
			}
		});
	}
}
