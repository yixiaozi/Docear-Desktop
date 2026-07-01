package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.features.time.mindmapmode.ReminderWorkspaceScanHelper.TimelineOccurrence;

/**
 * Chronological timeline of all reminders including future recurring occurrences.
 */
public class ReminderTimelineTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static final String[] TABLE_COLUMNS = { "\u65e5\u671f\u65f6\u95f4", "\u7c7b\u578b", "\u65f6\u957f", "\u7b49\u7ea7",
			"\u7d27\u6025", "\u4efb\u52a1", "\u5bfc\u56fe" };

	private static final class CachedFileResult {
		private final long modified;
		private final long length;
		private final List entries;

		private CachedFileResult(final long modified, final long length, final List entries) {
			this.modified = modified;
			this.length = length;
			this.entries = entries;
		}
	}

	private final JButton refreshButton = new JButton("\u5237\u65b0");
	private final JLabel statusLabel = new JLabel("\u65f6\u95f4\u8f74\u603b\u6761\u76ee: 0");
	private final DefaultTableModel tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
		private static final long serialVersionUID = 1L;

		public boolean isCellEditable(final int row, final int column) {
			return false;
		}
	};
	private final List tableRows = new ArrayList();
	private final JTable table = new JTable(tableModel);
	private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
	private final Map cacheByFile = new HashMap();
	private final List allEntries = new ArrayList();
	private SwingWorker activeWorker;
	private boolean rescanRequested;
	private final Timer autoRefreshTimer;

	public ReminderTimelineTabPanel() {
		super(new BorderLayout(4, 4));
		final JPanel top = new JPanel(new BorderLayout(4, 0));
		top.add(statusLabel, BorderLayout.CENTER);
		final JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
		topButtons.add(refreshButton);
		top.add(topButtons, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(48);
		table.getColumnModel().getColumn(2).setPreferredWidth(44);
		table.getColumnModel().getColumn(3).setPreferredWidth(40);
		table.getColumnModel().getColumn(4).setPreferredWidth(40);
		table.getColumnModel().getColumn(5).setPreferredWidth(220);
		table.getColumnModel().getColumn(6).setPreferredWidth(100);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getClickCount() >= 1) {
					openSelectedEntry();
				}
			}
		});
		add(new JScrollPane(table), BorderLayout.CENTER);

		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(final java.awt.event.ActionEvent e) {
				refreshInBackground();
			}
		});
		autoRefreshTimer = new Timer(300000, new java.awt.event.ActionListener() {
			public void actionPerformed(final java.awt.event.ActionEvent e) {
				refreshInBackground();
			}
		});
		autoRefreshTimer.setRepeats(true);
		autoRefreshTimer.start();

		Controller.getCurrentController().getMapViewManager().addMapSelectionListener(new IMapSelectionListener() {
			public void beforeMapChange(final MapModel oldMap, final MapModel newMap) {
			}

			public void afterMapChange(final MapModel oldMap, final MapModel newMap) {
				refreshInBackground();
			}
		});
		refreshInBackground();
	}

	private synchronized void refreshInBackground() {
		if (activeWorker != null && !activeWorker.isDone()) {
			rescanRequested = true;
			return;
		}
		rescanRequested = false;
		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				final List files = ReminderWorkspaceScanHelper.collectAllMindmapFiles();
				cleanupCache(files);
				allEntries.clear();
				for (int i = 0; i < files.size(); i++) {
					if (isCancelled()) {
						break;
					}
					final File file = (File) files.get(i);
					allEntries.addAll(getEntriesForFile(file));
				}
				return ReminderWorkspaceScanHelper.buildTimelineOccurrences(allEntries,
						ReminderWorkspaceScanHelper.timelineRangeStart(),
						ReminderWorkspaceScanHelper.timelineRangeEnd());
			}

			protected void done() {
				try {
					rebuildTable((List) get());
				}
				catch (Exception e) {
					LogUtils.warn(e);
				}
				if (rescanRequested) {
					rescanRequested = false;
					refreshInBackground();
				}
			}
		};
		activeWorker.execute();
	}

	private List getEntriesForFile(final File file) {
		final long modified = file.lastModified();
		final long length = file.length();
		final CachedFileResult cached = (CachedFileResult) cacheByFile.get(file.getAbsolutePath());
		if (cached != null && cached.modified == modified && cached.length == length) {
			return cached.entries;
		}
		final List entries = ReminderWorkspaceScanHelper.scanRemindersFromFile(file);
		cacheByFile.put(file.getAbsolutePath(), new CachedFileResult(modified, length, entries));
		return entries;
	}

	private void cleanupCache(final List currentFiles) {
		final Map currentPaths = new HashMap();
		for (int i = 0; i < currentFiles.size(); i++) {
			currentPaths.put(((File) currentFiles.get(i)).getAbsolutePath(), Boolean.TRUE);
		}
		final List toRemove = new ArrayList();
		for (final Object key : cacheByFile.keySet()) {
			if (!currentPaths.containsKey(key)) {
				toRemove.add(key);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			cacheByFile.remove(toRemove.get(i));
		}
	}

	private void rebuildTable(final List occurrences) {
		while (tableModel.getRowCount() > 0) {
			tableModel.removeRow(0);
		}
		tableRows.clear();
		if (occurrences == null) {
			statusLabel.setText("\u65f6\u95f4\u8f74\u603b\u6761\u76ee: 0");
			return;
		}
		for (int i = 0; i < occurrences.size(); i++) {
			final TimelineOccurrence item = (TimelineOccurrence) occurrences.get(i);
			final ReminderCalendarEntry entry = item.entry;
			final String typeLabel = entry.recurring ? "\u5468\u671f" : "\u4e00\u6b21";
			tableModel.addRow(new Object[] { dateTimeFormat.format(new Date(item.occurrenceAt)), typeLabel,
					ReminderTaskFormatter.formatDurationMinutes(entry.taskTime),
					ReminderTaskFormatter.formatLevel(entry.taskLevel),
					ReminderTaskFormatter.formatUrgency(entry.jinji), normalizeTaskText(entry.nodeText),
					entry.file.getName() });
			tableRows.add(item);
		}
		statusLabel.setText("\u65f6\u95f4\u8f74\u603b\u6761\u76ee: " + occurrences.size()
				+ " \uff08\u672a\u676590\u5929\u81f3\u672a\u6765366\u5929\uff09");
	}

	private void openSelectedEntry() {
		final int row = table.getSelectedRow();
		if (row < 0 || row >= tableRows.size()) {
			return;
		}
		final TimelineOccurrence item = (TimelineOccurrence) tableRows.get(row);
		ReminderTabNavigation.openEntry(item.entry);
	}

	private static String normalizeTaskText(final String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}
}
