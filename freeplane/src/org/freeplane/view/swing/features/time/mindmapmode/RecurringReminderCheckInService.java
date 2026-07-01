package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.freeplane.core.ui.DoubleClickTimer;
import org.freeplane.core.undo.IActor;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinks;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.ZoomableLabelUI;

/**
 * Records check-in notes under year/month/day child nodes and optionally advances recurring reminders.
 */
public final class RecurringReminderCheckInService {

	private RecurringReminderCheckInService() {
	}

	public static boolean handleReminderIconClick(final MainView mainView, final MouseEvent e, final NodeModel node,
			final DoubleClickTimer doubleClickTimer, final ModeController modeController) {
		if (!isRecurringReminderNode(node)) {
			return false;
		}
		final boolean inLinkRegion = mainView.isInFollowLinkRegion(e.getX());
		final Rectangle iconR = ((ZoomableLabelUI) mainView.getUI()).getIconR(mainView);
		final boolean inIconRegion = iconR != null && iconR.width > 0 && iconR.contains(e.getPoint());
		if (!inLinkRegion && !inIconRegion) {
			return false;
		}
		if (e.getClickCount() >= 2) {
			doubleClickTimer.cancel();
			return showCheckInDialogAndPerform(node, resolveRecordDate(node));
		}
		if (e.getClickCount() == 1 && inLinkRegion && NodeLinks.getValidLink(node) != null) {
			doubleClickTimer.start(new Runnable() {
				public void run() {
					LinkController.getController(modeController).loadURL(node, e);
				}
			});
			return true;
		}
		return false;
	}

	public static boolean openCheckInForEntry(final RecurringReminderEntry entry, final long recordDateMillis) {
		if (entry == null || entry.file == null) {
			return false;
		}
		final NodeModel node = resolveNode(entry);
		if (node == null) {
			JOptionPane.showMessageDialog(getDialogParent(), "\u672a\u627e\u5230\u5bf9\u5e94\u8282\u70b9\uff0c\u8bf7\u5148\u6253\u5f00\u76f8\u5173\u5bfc\u56fe\u3002",
					"\u5468\u671f\u4efb\u52a1\u6253\u5361", JOptionPane.WARNING_MESSAGE);
			return false;
		}
		return showCheckInDialogAndPerform(node, recordDateMillis);
	}

	static boolean isRecurringReminderNode(final NodeModel node) {
		if (node == null || ReminderExtension.getExtension(node) == null) {
			return false;
		}
		return ReminderCycleAttributes.readFromNode(node).isRecurring();
	}

	private static boolean showCheckInDialogAndPerform(final NodeModel node, final long recordDateMillis) {
		final JTextArea noteField = new JTextArea(5, 40);
		noteField.setLineWrap(true);
		noteField.setWrapStyleWord(true);
		final JScrollPane noteScroll = new JScrollPane(noteField);
		noteScroll.setPreferredSize(new java.awt.Dimension(420, 120));
		final JCheckBox advanceBox = new JCheckBox("\u5b8c\u6210\u5e76\u8fdb\u5165\u4e0b\u4e00\u5468\u671f", true);
		final JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.add(new JLabel("\u8bb0\u5f55\u5185\u5bb9\uff1a"), BorderLayout.NORTH);
		panel.add(noteScroll, BorderLayout.CENTER);
		panel.add(advanceBox, BorderLayout.SOUTH);
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(recordDateMillis);
		final String dateHint = cal.get(Calendar.YEAR) + "\u5e74" + (cal.get(Calendar.MONTH) + 1) + "\u6708"
				+ cal.get(Calendar.DAY_OF_MONTH) + "\u65e5";
		final int result = JOptionPane.showConfirmDialog(getDialogParent(), panel,
				"\u5468\u671f\u4efb\u52a1\u6253\u5361 \u2014 " + dateHint, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return false;
		}
		final String note = noteField.getText().trim();
		if (note.length() == 0) {
			JOptionPane.showMessageDialog(getDialogParent(), "\u8bf7\u8f93\u5165\u8bb0\u5f55\u5185\u5bb9\u3002", "\u5468\u671f\u4efb\u52a1\u6253\u5361",
					JOptionPane.WARNING_MESSAGE);
			return false;
		}
		performCheckIn(node, recordDateMillis, note, advanceBox.isSelected());
		return true;
	}

	private static String toNodeText(final String note) {
		final String normalized = note.replace("\r\n", "\n").replace('\r', '\n');
		if (normalized.indexOf('\n') >= 0) {
			return HtmlUtils.plainToHTML(normalized);
		}
		return normalized;
	}

	private static long resolveRecordDate(final NodeModel node) {
		final ReminderExtension reminder = ReminderExtension.getExtension(node);
		if (reminder != null) {
			return reminder.getRemindUserAt();
		}
		return System.currentTimeMillis();
	}

	private static void performCheckIn(final NodeModel taskNode, final long recordDateMillis, final String note,
			final boolean advanceCycle) {
		final ModeController modeController = Controller.getCurrentModeController();
		final MMapController mapController = (MMapController) modeController.getMapController();
		final MapModel map = taskNode.getMap();
		final ReminderHook reminderHook = (ReminderHook) modeController.getExtension(ReminderHook.class);
		final ReminderExtension reminder = ReminderExtension.getExtension(taskNode);
		if (reminder == null) {
			return;
		}
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(recordDateMillis);
		final String year = String.valueOf(cal.get(Calendar.YEAR));
		final String month = String.valueOf(cal.get(Calendar.MONTH) + 1);
		final String day = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
		final long oldRemindAt = reminder.getRemindUserAt();
		final ReminderCycleAttributes.CycleConfig cycleConfig = ReminderCycleAttributes.readFromNode(taskNode);
		final long newRemindAt = advanceCycle ? ReminderCycleScheduler.computeNextAfterCheckIn(oldRemindAt, cycleConfig)
				: oldRemindAt;
		final boolean shouldAdvance = advanceCycle && newRemindAt > oldRemindAt;
		final MindIcon okIcon = IconStoreFactory.create().getMindIcon("button_ok");
		final NodeModel[] recordNodeHolder = new NodeModel[1];
		modeController.execute(new IActor() {
			private NodeModel recordNode;
			private NodeModel dayNode;

			public void act() {
				final Map structureCache = new HashMap();
				final NodeModel yearNode = findOrCreateStructureNode(mapController, map, taskNode, year, structureCache);
				final NodeModel monthNode = findOrCreateStructureNode(mapController, map, yearNode, month, structureCache);
				dayNode = findOrCreateStructureNode(mapController, map, monthNode, day, structureCache);
				recordNode = mapController.newNode(toNodeText(note), map);
				recordNode.setLeft(false);
				mapController.insertNodeIntoWithoutUndo(recordNode, dayNode, dayNode.getChildCount());
				recordNodeHolder[0] = recordNode;
				if (okIcon != null) {
					recordNode.addIcon(okIcon);
				}
				if (shouldAdvance) {
					reminder.deactivateTimer();
					reminder.setRemindUserAt(newRemindAt);
					if (reminderHook != null) {
						reminderHook.rescheduleReminder(reminder);
					}
					mapController.nodeChanged(taskNode, ReminderExtension.class, Long.valueOf(oldRemindAt),
							Long.valueOf(newRemindAt));
				}
				mapController.nodeChanged(dayNode);
				mapController.setSaved(map, false);
			}

			public String getDescription() {
				return "recurring reminder check-in";
			}

			public void undo() {
				if (recordNode != null) {
					mapController.deleteWithoutUndo(recordNode);
					recordNodeHolder[0] = null;
					pruneEmptyStructure(mapController, dayNode, taskNode);
				}
				if (shouldAdvance) {
					reminder.deactivateTimer();
					reminder.setRemindUserAt(oldRemindAt);
					if (reminderHook != null) {
						reminderHook.rescheduleReminder(reminder);
					}
					mapController.nodeChanged(taskNode, ReminderExtension.class, Long.valueOf(newRemindAt),
							Long.valueOf(oldRemindAt));
				}
				mapController.setSaved(map, false);
			}
		}, map);
		final NodeModel recordNode = recordNodeHolder[0];
		if (recordNode != null) {
			final MapController displayController = modeController.getMapController();
			displayController.displayNode(recordNode, null);
			Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(recordNode);
			displayController.centerNode(recordNode);
		}
	}

	private static NodeModel resolveNode(final RecurringReminderEntry entry) {
		try {
			final ModeController modeController = Controller.getCurrentModeController();
			final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			final URL url = entry.file.toURI().toURL();
			MapModel map = findOpenMap(mapViewManager, entry.file);
			if (map == null) {
				if (!mapViewManager.tryToChangeToMapView(url)) {
					modeController.getMapController().newMap(url);
				}
				map = findOpenMap(mapViewManager, entry.file);
			}
			if (map == null) {
				return null;
			}
			return map.getNodeForID(entry.nodeId);
		}
		catch (Exception e) {
			LogUtils.warn(e);
			return null;
		}
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

	private static NodeModel findOrCreateStructureNode(final MMapController mapController, final MapModel map,
			final NodeModel parent, final String label, final Map structureCache) {
		final String cacheKey = parent.getID() + "|" + label;
		final NodeModel cached = (NodeModel) structureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		for (NodeModel child : parent.getChildren()) {
			if (label.equals(normalizeNodeText(child.getText()))) {
				structureCache.put(cacheKey, child);
				return child;
			}
		}
		final NodeModel newNode = mapController.newNode(label, map);
		newNode.setLeft(false);
		mapController.insertNodeIntoWithoutUndo(newNode, parent, findStructureInsertIndex(parent, label));
		structureCache.put(cacheKey, newNode);
		return newNode;
	}

	private static int findStructureInsertIndex(final NodeModel parent, final String label) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			final String existing = normalizeNodeText(((NodeModel) parent.getChildAt(i)).getText());
			if (compareStructureLabel(existing, label) > 0) {
				return i;
			}
		}
		return parent.getChildCount();
	}

	private static int compareStructureLabel(final String a, final String b) {
		try {
			return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
		}
		catch (NumberFormatException e) {
			return a.compareTo(b);
		}
	}

	private static void pruneEmptyStructure(final MMapController mapController, NodeModel node,
			final NodeModel taskNode) {
		while (node != null && node != taskNode && node.getChildCount() == 0) {
			final NodeModel parent = node.getParentNode();
			mapController.deleteWithoutUndo(node);
			node = parent;
		}
	}

	private static String normalizeNodeText(final String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}

	private static Component getDialogParent() {
		return UITools.getFrame();
	}
}
