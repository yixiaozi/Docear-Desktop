package org.freeplane.view.swing.features.time.mindmapmode;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.freeplane.core.util.TextUtils;

/**
 * Chinese-friendly reminder datetime formatting.
 */
final class ReminderDateTimeFormatter {
	private static final SimpleDateFormat MAIN_DISPLAY = new SimpleDateFormat("yyyy\u5e74M\u6708d\u65e5  EEEE  HH:mm",
			Locale.CHINA);
	private static final SimpleDateFormat INPUT_DISPLAY = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
	private static final SimpleDateFormat CURRENT_REMINDER = new SimpleDateFormat(
			"yyyy\u5e74M\u6708d\u65e5\uff08EEEE\uff09HH:mm", Locale.CHINA);
	private static final SimpleDateFormat INLINE_NODE = new SimpleDateFormat("M\u6708d\u65e5 HH:mm", Locale.CHINA);

	private ReminderDateTimeFormatter() {
	}

	static String formatMainDisplay(final Date date) {
		if (date == null) {
			return "";
		}
		synchronized (MAIN_DISPLAY) {
			return MAIN_DISPLAY.format(date);
		}
	}

	static String formatInputValue(final Date date) {
		if (date == null) {
			return "";
		}
		synchronized (INPUT_DISPLAY) {
			return INPUT_DISPLAY.format(date);
		}
	}

	static String formatCurrentReminderLine(final Date date) {
		if (date == null) {
			return TextUtils.getText("plugins/TimeManagement.xml_noReminder");
		}
		final String formatted;
		synchronized (CURRENT_REMINDER) {
			formatted = CURRENT_REMINDER.format(date);
		}
		return TextUtils.format("plugins/TimeManagement.xml_currentReminderAt", formatted);
	}

	static String formatCyclePreviewLine(final ReminderCycleAttributes.CycleConfig config) {
		final String cycleLabel = ReminderCycleTypeFormatter.format(config == null ? ReminderCycleAttributes.CycleConfig
				.oneTime() : config);
		return TextUtils.format("plugins/TimeManagement.xml_currentReminderCycle", cycleLabel);
	}

	/** Compact label shown on the mind map node after the clock icon. */
	static String formatInlineNodePrefix(final Date date, final ReminderCycleAttributes.CycleConfig config) {
		if (date == null) {
			return "";
		}
		final String datePart;
		synchronized (INLINE_NODE) {
			datePart = INLINE_NODE.format(date);
		}
		final StringBuilder sb = new StringBuilder(datePart);
		if (config != null && config.isRecurring()) {
			sb.append(" [").append(ReminderCycleTypeFormatter.format(config)).append(']');
		}
		return sb.toString();
	}

	static Calendar toCalendar(final Date date) {
		final Calendar calendar = Calendar.getInstance(Locale.CHINA);
		if (date != null) {
			calendar.setTime(date);
		}
		normalizeSeconds(calendar);
		return calendar;
	}

	static Date fromCalendar(final Calendar calendar) {
		if (calendar == null) {
			return null;
		}
		normalizeSeconds(calendar);
		return calendar.getTime();
	}

	static void normalizeSeconds(final Calendar calendar) {
		if (calendar == null) {
			return;
		}
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
	}
}
