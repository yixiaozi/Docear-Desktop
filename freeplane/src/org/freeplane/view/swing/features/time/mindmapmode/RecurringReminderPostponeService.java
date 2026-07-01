package org.freeplane.view.swing.features.time.mindmapmode;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.freeplane.core.undo.IActor;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;

/**
 * Advances expired recurring reminders to their next cycle date.
 */
final class RecurringReminderPostponeService {

	private RecurringReminderPostponeService() {
	}

	static int postponeAllExpired(final List entries) {
		if (entries == null || entries.isEmpty()) {
			return 0;
		}
		final Map byFile = new HashMap();
		for (int i = 0; i < entries.size(); i++) {
			final RecurringReminderEntry entry = (RecurringReminderEntry) entries.get(i);
			if (entry == null || entry.file == null) {
				continue;
			}
			List group = (List) byFile.get(entry.file);
			if (group == null) {
				group = new java.util.ArrayList();
				byFile.put(entry.file, group);
			}
			group.add(entry);
		}
		int updated = 0;
		for (final Iterator it = byFile.entrySet().iterator(); it.hasNext();) {
			final Map.Entry fileEntry = (Map.Entry) it.next();
			final File file = (File) fileEntry.getKey();
			final List records = (List) fileEntry.getValue();
			try {
				updated += postponeInFile(file, records);
			}
			catch (Exception e) {
				LogUtils.warn(e);
			}
		}
		return updated;
	}

	private static int postponeInFile(final File file, final List records) throws Exception {
		final ModeController modeController = Controller.getCurrentModeController();
		final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
		final URL url = file.toURI().toURL();
		MapModel map = findOpenMap(mapViewManager, file);
		if (map == null) {
			if (!mapViewManager.tryToChangeToMapView(url)) {
				modeController.getMapController().newMap(url);
			}
			map = findOpenMap(mapViewManager, file);
		}
		if (map == null) {
			return 0;
		}
		final ReminderHook reminderHook = (ReminderHook) modeController.getExtension(ReminderHook.class);
		if (reminderHook == null) {
			return 0;
		}
		int updated = 0;
		for (int i = 0; i < records.size(); i++) {
			final RecurringReminderEntry entry = (RecurringReminderEntry) records.get(i);
			final NodeModel node = map.getNodeForID(entry.nodeId);
			if (node == null || entry.cycleConfig == null) {
				continue;
			}
			final ReminderExtension reminder = ReminderExtension.getExtension(node);
			if (reminder == null) {
				continue;
			}
			final long oldTime = reminder.getRemindUserAt();
			final long newTime = ReminderCycleScheduler.computeNextAfterNow(oldTime, entry.cycleConfig);
			if (newTime <= oldTime) {
				continue;
			}
			updateReminderTime(modeController, reminderHook, node, reminder, oldTime, newTime);
			updated++;
		}
		if (updated > 0) {
			modeController.getMapController().setSaved(map, false);
		}
		return updated;
	}

	private static void updateReminderTime(final ModeController modeController, final ReminderHook reminderHook,
			final NodeModel node, final ReminderExtension reminder, final long oldTime, final long newTime) {
		final MapController mapController = modeController.getMapController();
		modeController.execute(new IActor() {
			public void act() {
				reminder.deactivateTimer();
				reminder.setRemindUserAt(newTime);
				reminderHook.rescheduleReminder(reminder);
				mapController.nodeChanged(node, ReminderExtension.class, Long.valueOf(oldTime), Long.valueOf(newTime));
			}

			public String getDescription() {
				return "postpone recurring reminder";
			}

			public void undo() {
				reminder.deactivateTimer();
				reminder.setRemindUserAt(oldTime);
				reminderHook.rescheduleReminder(reminder);
				mapController.nodeChanged(node, ReminderExtension.class, Long.valueOf(newTime), Long.valueOf(oldTime));
			}
		}, node.getMap());
	}

	private static MapModel findOpenMap(final IMapViewManager mapViewManager, final File file) {
		final Map maps = mapViewManager.getMaps(MModeController.MODENAME);
		for (final Object mapObj : maps.values()) {
			final MapModel map = (MapModel) mapObj;
			if (isSameFile(map.getFile(), file)) {
				return map;
			}
		}
		return null;
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
