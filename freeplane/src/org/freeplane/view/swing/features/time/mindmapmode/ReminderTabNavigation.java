package org.freeplane.view.swing.features.time.mindmapmode;

import java.io.File;
import java.net.URL;
import java.util.Map;

import javax.swing.Timer;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;

/**
 * Opens a reminder node in the mind map editor.
 */
final class ReminderTabNavigation {

	private ReminderTabNavigation() {
	}

	static void openEntry(final ReminderCalendarEntry entry) {
		if (entry == null || entry.file == null) {
			return;
		}
		try {
			final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			final URL url = entry.file.toURI().toURL();
			if (!mapViewManager.tryToChangeToMapView(url)) {
				Controller.getCurrentModeController().getMapController().newMap(url);
			}
			selectNodeWithRetry(entry.file, entry.nodeId, 0);
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private static void selectNodeWithRetry(final File file, final String nodeId, final int attempt) {
		final int maxAttempts = 12;
		if (file == null || nodeId == null || attempt > maxAttempts) {
			return;
		}
		try {
			final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			final Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (final Object mapObj : maps.values()) {
				final MapModel map = (MapModel) mapObj;
				if (isSameFile(map.getFile(), file)) {
					final NodeModel node = map.getNodeForID(nodeId);
					if (node != null) {
						Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(node);
						Controller.getCurrentModeController().getMapController().centerNode(node);
						return;
					}
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
		final Timer retry = new Timer(250, new java.awt.event.ActionListener() {
			public void actionPerformed(final java.awt.event.ActionEvent e) {
				selectNodeWithRetry(file, nodeId, attempt + 1);
			}
		});
		retry.setRepeats(false);
		retry.start();
	}

	private static boolean isSameFile(final File file1, final File file2) {
		if (file1 == null || file2 == null) {
			return file1 == file2;
		}
		try {
			return file1.getCanonicalPath().equals(file2.getCanonicalPath());
		}
		catch (Exception e) {
			return file1.getAbsolutePath().equals(file2.getAbsolutePath());
		}
	}
}
