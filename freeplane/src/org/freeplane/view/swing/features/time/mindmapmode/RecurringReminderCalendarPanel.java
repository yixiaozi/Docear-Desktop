package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;

import org.freeplane.core.util.HtmlUtils;

/**
 * Month and week calendar for recurring reminder tasks.
 */
final class RecurringReminderCalendarPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String VIEW_MONTH = "month";
	private static final String VIEW_WEEK = "week";
	private static final String[] WEEK_HEADERS = { "\u5468\u4e00", "\u5468\u4e8c", "\u5468\u4e09", "\u5468\u56db", "\u5468\u4e94", "\u5468\u516d", "\u5468\u65e5" };

	interface NavigationHandler {
		void openEntry(RecurringReminderEntry entry);
	}

	interface CheckInHandler {
		void checkInEntry(RecurringReminderEntry entry, long occurrenceAt);
	}

	private final SimpleDateFormat monthTitleFormat = new SimpleDateFormat("yyyy\u5e74M\u6708", Locale.CHINA);
	private final SimpleDateFormat dayTitleFormat = new SimpleDateFormat("M\u6708d\u65e5 EEEE", Locale.CHINA);
	private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);

	private final JLabel titleLabel = new JLabel(" ");
	private final JLabel detailLabel = new JLabel("\u9009\u4e2d\u65e5\u671f\u7684\u4efb\u52a1\uff1a\u5355\u51fb\u8df3\u8f6c\u8282\u70b9\uff0c\u53cc\u51fb\u6253\u5361");
	private final DefaultListModel detailModel = new DefaultListModel();
	private final JList detailList = new JList(detailModel);
	private final JPanel monthGrid = new JPanel(new GridLayout(0, 7, 2, 2));
	private final JPanel weekGrid = new JPanel(new GridLayout(1, 7, 4, 0));
	private final CardLayout cardLayout = new CardLayout();
	private final JPanel calendarCard = new JPanel(cardLayout);
	private final List entries = new ArrayList();
	private long visibleMonthStart;
	private long selectedDayStart = ReminderCycleScheduler.startOfDay(System.currentTimeMillis());
	private boolean weekView;
	private NavigationHandler navigationHandler;
	private CheckInHandler checkInHandler;

	RecurringReminderCalendarPanel() {
		super(new BorderLayout(4, 4));
		setBorder(BorderFactory.createTitledBorder("\u4efb\u52a1\u65e5\u5386"));

		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		final JButton prevButton = new JButton("\u25C0");
		final JButton nextButton = new JButton("\u25B6");
		final JButton todayButton = new JButton("\u4eca\u5929");
		final JToggleButton monthButton = new JToggleButton("\u6708\u89c6\u56fe", true);
		final JToggleButton weekButton = new JToggleButton("\u5468\u89c6\u56fe");
		toolbar.add(prevButton);
		toolbar.add(nextButton);
		toolbar.add(todayButton);
		toolbar.add(titleLabel);
		toolbar.add(monthButton);
		toolbar.add(weekButton);
		add(toolbar, BorderLayout.NORTH);

		for (int i = 0; i < 7; i++) {
			monthGrid.add(createHeaderLabel(WEEK_HEADERS[i]));
		}
		calendarCard.add(new JScrollPane(monthGrid), VIEW_MONTH);
		calendarCard.add(new JScrollPane(weekGrid), VIEW_WEEK);
		add(calendarCard, BorderLayout.CENTER);

		detailList.setVisibleRowCount(4);
		detailList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		detailList.setCellRenderer(createOccurrenceRenderer());
		installOpenOnClick(detailList);
		final JPanel detailPanel = new JPanel(new BorderLayout(2, 2));
		detailPanel.add(detailLabel, BorderLayout.NORTH);
		detailPanel.add(new JScrollPane(detailList), BorderLayout.CENTER);
		detailPanel.setPreferredSize(new Dimension(0, 110));
		add(detailPanel, BorderLayout.SOUTH);

		prevButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (weekView) {
					visibleMonthStart = ReminderCycleScheduler.addDays(visibleMonthStart, -7);
				}
				else {
					visibleMonthStart = ReminderCycleScheduler.addMonths(visibleMonthStart, -1);
				}
				rebuildCalendar();
			}
		});
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (weekView) {
					visibleMonthStart = ReminderCycleScheduler.addDays(visibleMonthStart, 7);
				}
				else {
					visibleMonthStart = ReminderCycleScheduler.addMonths(visibleMonthStart, 1);
				}
				rebuildCalendar();
			}
		});
		todayButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				selectedDayStart = ReminderCycleScheduler.startOfDay(System.currentTimeMillis());
				visibleMonthStart = weekView ? ReminderCycleScheduler.startOfWeek(selectedDayStart)
						: startOfMonth(selectedDayStart);
				rebuildCalendar();
			}
		});
		monthButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				weekView = false;
				monthButton.setSelected(true);
				weekButton.setSelected(false);
				visibleMonthStart = startOfMonth(selectedDayStart);
				cardLayout.show(calendarCard, VIEW_MONTH);
				rebuildCalendar();
			}
		});
		weekButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				weekView = true;
				weekButton.setSelected(true);
				monthButton.setSelected(false);
				visibleMonthStart = ReminderCycleScheduler.startOfWeek(selectedDayStart);
				cardLayout.show(calendarCard, VIEW_WEEK);
				rebuildCalendar();
			}
		});

		visibleMonthStart = startOfMonth(System.currentTimeMillis());
		rebuildCalendar();
	}

	void setEntries(final List newEntries) {
		entries.clear();
		if (newEntries != null) {
			entries.addAll(newEntries);
		}
		rebuildCalendar();
	}

	void setNavigationHandler(final NavigationHandler navigationHandler) {
		this.navigationHandler = navigationHandler;
	}

	void setCheckInHandler(final CheckInHandler checkInHandler) {
		this.checkInHandler = checkInHandler;
	}

	private DefaultListCellRenderer createOccurrenceRenderer() {
		return new DefaultListCellRenderer() {
			private static final long serialVersionUID = 1L;

			public Component getListCellRendererComponent(final JList list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof OccurrenceItem) {
					setText(formatOccurrence((OccurrenceItem) value));
				}
				else if (value instanceof String) {
					setText((String) value);
				}
				return this;
			}
		};
	}

	private void installOpenOnClick(final JList list) {
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				final int index = list.locationToIndex(e.getPoint());
				if (index < 0) {
					return;
				}
				final Object value = list.getModel().getElementAt(index);
				if (!(value instanceof OccurrenceItem)) {
					return;
				}
				list.setSelectedIndex(index);
				final OccurrenceItem item = (OccurrenceItem) value;
				if (e.getClickCount() >= 2) {
					checkInEntry(item.entry, item.occurrenceAt);
				}
				else if (e.getClickCount() == 1) {
					openEntry(item.entry);
				}
			}
		});
	}

	private void checkInEntry(final RecurringReminderEntry entry, final long occurrenceAt) {
		if (checkInHandler != null && entry != null) {
			checkInHandler.checkInEntry(entry, occurrenceAt);
		}
	}

	private void openEntry(final RecurringReminderEntry entry) {
		if (navigationHandler != null && entry != null) {
			navigationHandler.openEntry(entry);
		}
	}

	private void selectDay(final long dayStart) {
		selectedDayStart = dayStart;
		rebuildCalendar();
	}

	private void rebuildCalendar() {
		if (weekView) {
			rebuildWeekView();
		}
		else {
			rebuildMonthView();
		}
		updateDetailList();
	}

	private void rebuildMonthView() {
		titleLabel.setText(monthTitleFormat.format(new Date(visibleMonthStart)));
		monthGrid.removeAll();
		for (int i = 0; i < 7; i++) {
			monthGrid.add(createHeaderLabel(WEEK_HEADERS[i]));
		}
		final long monthEnd = ReminderCycleScheduler.addMonths(visibleMonthStart, 1);
		final Map dayMap = buildOccurrenceMap(visibleMonthStart, monthEnd);
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(visibleMonthStart);
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		int leading = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7;
		for (int i = 0; i < leading; i++) {
			monthGrid.add(createEmptyCell());
		}
		cal.setTimeInMillis(visibleMonthStart);
		while (cal.getTimeInMillis() < monthEnd) {
			final long dayStart = ReminderCycleScheduler.startOfDay(cal.getTimeInMillis());
			final List dayEntries = (List) dayMap.get(Long.valueOf(dayStart));
			monthGrid.add(createDayCell(cal.get(Calendar.DAY_OF_MONTH), dayStart, dayEntries));
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		while (monthGrid.getComponentCount() % 7 != 0) {
			monthGrid.add(createEmptyCell());
		}
		monthGrid.revalidate();
		monthGrid.repaint();
	}

	private void rebuildWeekView() {
		final long weekEnd = ReminderCycleScheduler.addDays(visibleMonthStart, 7);
		titleLabel.setText(dayTitleFormat.format(new Date(visibleMonthStart)) + " - "
				+ dayTitleFormat.format(new Date(ReminderCycleScheduler.addDays(visibleMonthStart, 6))));
		weekGrid.removeAll();
		final Map dayMap = buildOccurrenceMap(visibleMonthStart, weekEnd);
		for (int i = 0; i < 7; i++) {
			final long dayStart = ReminderCycleScheduler.addDays(visibleMonthStart, i);
			final List dayEntries = (List) dayMap.get(Long.valueOf(dayStart));
			weekGrid.add(createWeekColumn(dayStart, dayEntries));
		}
		weekGrid.revalidate();
		weekGrid.repaint();
	}

	private JPanel createWeekColumn(final long dayStart, final List dayEntries) {
		final JPanel column = new JPanel(new BorderLayout(2, 2));
		column.setBorder(BorderFactory.createEtchedBorder());
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(dayStart);
		final JLabel header = new JLabel(WEEK_HEADERS[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7] + " "
				+ cal.get(Calendar.DAY_OF_MONTH), JLabel.CENTER);
		header.setFont(header.getFont().deriveFont(Font.BOLD));
		header.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		header.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				selectDay(dayStart);
			}
		});
		column.add(header, BorderLayout.NORTH);
		final DefaultListModel model = new DefaultListModel();
		if (dayEntries != null) {
			for (int i = 0; i < dayEntries.size(); i++) {
				model.addElement(dayEntries.get(i));
			}
		}
		final JList list = new JList(model);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(createOccurrenceRenderer());
		installOpenOnClick(list);
		column.add(new JScrollPane(list), BorderLayout.CENTER);
		column.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (e.getSource() == column) {
					selectDay(dayStart);
				}
			}
		});
		if (dayStart == selectedDayStart) {
			column.setBackground(new Color(220, 235, 255));
		}
		return column;
	}

	private JButton createDayCell(final int dayOfMonth, final long dayStart, final List dayEntries) {
		final int count = dayEntries == null ? 0 : dayEntries.size();
		final String text = count > 0 ? dayOfMonth + "\n(" + count + ")" : String.valueOf(dayOfMonth);
		final JButton button = new JButton(text);
		button.setMargin(new java.awt.Insets(2, 2, 2, 2));
		if (dayStart == selectedDayStart) {
			button.setBackground(new Color(180, 210, 255));
		}
		else if (count > 0) {
			button.setForeground(new Color(0, 102, 153));
		}
		if (dayStart == ReminderCycleScheduler.startOfDay(System.currentTimeMillis())) {
			button.setBorder(BorderFactory.createLineBorder(new Color(0, 120, 215), 2));
		}
		button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				selectDay(dayStart);
			}
		});
		return button;
	}

	private void updateDetailList() {
		final long dayEnd = ReminderCycleScheduler.addDays(selectedDayStart, 1);
		final Map dayMap = buildOccurrenceMap(selectedDayStart, dayEnd);
		final List dayEntries = (List) dayMap.get(Long.valueOf(selectedDayStart));
		detailModel.clear();
		detailLabel.setText(dayTitleFormat.format(new Date(selectedDayStart)) + "\u7684\u4efb\u52a1");
		if (dayEntries == null || dayEntries.isEmpty()) {
			detailModel.addElement("\u6682\u65e0\u5468\u671f\u4efb\u52a1");
			return;
		}
		for (int i = 0; i < dayEntries.size(); i++) {
			detailModel.addElement(dayEntries.get(i));
		}
	}

	private String formatOccurrence(final OccurrenceItem item) {
		return timeFormat.format(new Date(item.occurrenceAt)) + "  "
				+ normalizeTaskText(item.entry.nodeText) + "  [" + item.entry.file.getName() + "]";
	}

	private Map buildOccurrenceMap(final long rangeStart, final long rangeEnd) {
		final Map map = new HashMap();
		for (int i = 0; i < entries.size(); i++) {
			final RecurringReminderEntry entry = (RecurringReminderEntry) entries.get(i);
			final List occurrences = ReminderCycleScheduler.enumerateOccurrencesInRange(entry.remindAt,
					entry.cycleConfig, rangeStart, rangeEnd);
			for (int j = 0; j < occurrences.size(); j++) {
				final long occurrenceAt = ((Long) occurrences.get(j)).longValue();
				final long dayStart = ReminderCycleScheduler.startOfDay(occurrenceAt);
				List dayList = (List) map.get(Long.valueOf(dayStart));
				if (dayList == null) {
					dayList = new ArrayList();
					map.put(Long.valueOf(dayStart), dayList);
				}
				dayList.add(new OccurrenceItem(entry, occurrenceAt));
			}
		}
		for (final Iterator it = map.values().iterator(); it.hasNext();) {
			final List dayList = (List) it.next();
			Collections.sort(dayList, new Comparator() {
				public int compare(final Object o1, final Object o2) {
					final OccurrenceItem a = (OccurrenceItem) o1;
					final OccurrenceItem b = (OccurrenceItem) o2;
					return Long.compare(a.occurrenceAt, b.occurrenceAt);
				}
			});
		}
		return map;
	}

	private static JLabel createHeaderLabel(final String text) {
		final JLabel label = new JLabel(text, JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		return label;
	}

	private static JLabel createEmptyCell() {
		final JLabel label = new JLabel(" ");
		label.setEnabled(false);
		return label;
	}

	private static long startOfMonth(final long millis) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(millis);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		return ReminderCycleScheduler.startOfDay(cal.getTimeInMillis());
	}

	private static String normalizeTaskText(final String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}

	private static final class OccurrenceItem {
		private final RecurringReminderEntry entry;
		private final long occurrenceAt;

		private OccurrenceItem(final RecurringReminderEntry entry, final long occurrenceAt) {
			this.entry = entry;
			this.occurrenceAt = occurrenceAt;
		}
	}
}
