package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

/**
 * Compact reminder editor: one datetime field, keyboard-first workflow.
 */
class SimpleReminderDialogPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private static final String INPUT_PATTERN = "yyyy-MM-dd HH:mm";
	private static final String[] PARSE_PATTERNS = { "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm", "yyyy-M-d H:m",
			"yyyyMMdd HHmm", "yyyy-MM-dd" };

	private final ReminderHook reminderHook;
	private final Runnable onClose;
	private final SimpleDateFormat displayFormat;
	private final JTextField dateTimeField;
	private final JLabel currentReminderValueLabel;
	private final JButton okButton;
	private final JButton removeButton;

	SimpleReminderDialogPanel(final ReminderHook reminderHook, final Runnable onClose) {
		super();
		this.reminderHook = reminderHook;
		this.onClose = onClose;
		this.displayFormat = new SimpleDateFormat(INPUT_PATTERN);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

		final JLabel inputLabel = new JLabel(getText("plugins/TimeManagement.xml_reminderDateTimeLabel"));
		inputLabel.setAlignmentX(LEFT_ALIGNMENT);
		add(inputLabel);

		dateTimeField = new JTextField(18);
		dateTimeField.setMaximumSize(dateTimeField.getPreferredSize());
		dateTimeField.setAlignmentX(LEFT_ALIGNMENT);
		dateTimeField.setToolTipText(getText("plugins/TimeManagement.xml_reminderDateTimeHint"));
		dateTimeField.addKeyListener(new KeyAdapter() {
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					applyReminder();
				}
			}
		});
		add(dateTimeField);
		add(Box.createVerticalStrut(8));

		final JPanel shortcuts = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		shortcuts.setAlignmentX(LEFT_ALIGNMENT);
		shortcuts.add(createShortcutButton("plugins/TimeManagement.xml_todayButton", new Runnable() {
			public void run() {
				setDateTime(Calendar.getInstance().getTime());
			}
		}));
		shortcuts.add(createShortcutButton("plugins/TimeManagement.xml_inOneHourButton", new Runnable() {
			public void run() {
				final Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.HOUR_OF_DAY, 1);
				setDateTime(calendar.getTime());
			}
		}));
		shortcuts.add(createShortcutButton("plugins/TimeManagement.xml_tomorrowButton", new Runnable() {
			public void run() {
				final Calendar calendar = Calendar.getInstance();
				calendar.add(Calendar.DAY_OF_MONTH, 1);
				setDateTime(calendar.getTime());
			}
		}));
		add(shortcuts);
		add(Box.createVerticalStrut(6));

		final JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		statusPanel.setAlignmentX(LEFT_ALIGNMENT);
		statusPanel.add(new JLabel(getText("plugins/TimeManagement.xml_currentReminderLabel") + ":"));
		currentReminderValueLabel = new JLabel();
		statusPanel.add(currentReminderValueLabel);
		add(statusPanel);
		add(Box.createVerticalStrut(10));

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		okButton = new JButton(getText("plugins/TimeManagement.xml_confirmButton"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				applyReminder();
			}
		});
		removeButton = new JButton(getText("plugins/TimeManagement.xml_removeReminderButton"));
		removeButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				removeReminder();
				if (onClose != null) {
					onClose.run();
				}
			}
		});
		final JButton cancelButton = new JButton(getText("plugins/TimeManagement.xml_cancelButton"));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (onClose != null) {
					onClose.run();
				}
			}
		});
		buttons.add(okButton);
		buttons.add(removeButton);
		buttons.add(cancelButton);
		add(buttons);

		loadFromSelectedNode();
	}

	JButton getOkButton() {
		return okButton;
	}

	void focusInput() {
		dateTimeField.requestFocusInWindow();
		dateTimeField.selectAll();
	}

	void loadFromSelectedNode() {
		final NodeModel node = getSelectedNode();
		if (node == null) {
			setDateTime(defaultDateTime());
			updateCurrentReminderLabel(null);
			okButton.setEnabled(false);
			removeButton.setEnabled(false);
			return;
		}
		okButton.setEnabled(true);
		final ReminderExtension reminder = ReminderExtension.getExtension(node);
		if (reminder != null) {
			setDateTime(new Date(reminder.getRemindUserAt()));
			updateCurrentReminderLabel(reminder);
			removeButton.setEnabled(true);
		}
		else {
			setDateTime(defaultDateTime());
			updateCurrentReminderLabel(null);
			removeButton.setEnabled(false);
		}
	}

	private Date defaultDateTime() {
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.HOUR_OF_DAY, 1);
		return calendar.getTime();
	}

	private void setDateTime(final Date date) {
		if (date == null) {
			dateTimeField.setText("");
			return;
		}
		synchronized (displayFormat) {
			dateTimeField.setText(displayFormat.format(date));
		}
		dateTimeField.requestFocusInWindow();
		dateTimeField.selectAll();
	}

	private void updateCurrentReminderLabel(final ReminderExtension reminder) {
		if (reminder == null) {
			currentReminderValueLabel.setText(getText("plugins/TimeManagement.xml_noReminder"));
			return;
		}
		final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		currentReminderValueLabel.setText(dateTimeFormat.format(new Date(reminder.getRemindUserAt())));
	}

	private void applyReminder() {
		final Date date = parseDateTime(dateTimeField.getText());
		if (date == null) {
			JOptionPane.showMessageDialog(this, getText("plugins/TimeManagement.xml_invalidDateTime"),
					getText("plugins/TimeManagement.xml_WindowTitle"), JOptionPane.WARNING_MESSAGE);
			dateTimeField.requestFocusInWindow();
			dateTimeField.selectAll();
			return;
		}
		final Collection nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		if (nodes.isEmpty()) {
			return;
		}
		for (final Iterator it = nodes.iterator(); it.hasNext();) {
			final NodeModel node = (NodeModel) it.next();
			final ReminderExtension existing = ReminderExtension.getExtension(node);
			if (existing != null) {
				reminderHook.undoableToggleHook(node);
			}
			final ReminderExtension reminderExtension = new ReminderExtension(node);
			reminderExtension.setRemindUserAt(date.getTime());
			reminderExtension.setPeriodUnit(PeriodUnit.DAY);
			reminderExtension.setPeriod(1);
			reminderExtension.setScript(null);
			reminderHook.undoableActivateHook(node, reminderExtension);
		}
		if (onClose != null) {
			onClose.run();
		}
	}

	private void removeReminder() {
		final Collection nodes = Controller.getCurrentModeController().getMapController().getSelectedNodes();
		for (final Iterator it = nodes.iterator(); it.hasNext();) {
			final NodeModel node = (NodeModel) it.next();
			if (ReminderExtension.getExtension(node) != null) {
				reminderHook.undoableToggleHook(node);
			}
		}
	}

	private Date parseDateTime(final String value) {
		if (value == null) {
			return null;
		}
		final String trimmed = value.trim();
		if (trimmed.length() == 0) {
			return null;
		}
		for (int i = 0; i < PARSE_PATTERNS.length; i++) {
			try {
				final SimpleDateFormat format = new SimpleDateFormat(PARSE_PATTERNS[i]);
				format.setLenient(false);
				final Date parsed = format.parse(trimmed);
				if (PARSE_PATTERNS[i].equals("yyyy-MM-dd")) {
					final Calendar calendar = Calendar.getInstance();
					calendar.setTime(parsed);
					calendar.set(Calendar.HOUR_OF_DAY, 9);
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MILLISECOND, 0);
					return calendar.getTime();
				}
				return parsed;
			}
			catch (final ParseException e) {
				// try next pattern
			}
		}
		return null;
	}

	private JButton createShortcutButton(final String textKey, final Runnable action) {
		final JButton button = new JButton(getText(textKey));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				action.run();
			}
		});
		return button;
	}

	private NodeModel getSelectedNode() {
		return Controller.getCurrentModeController().getMapController().getSelectedNode();
	}

	private String getText(final String key) {
		return TextUtils.getText(key);
	}
}
