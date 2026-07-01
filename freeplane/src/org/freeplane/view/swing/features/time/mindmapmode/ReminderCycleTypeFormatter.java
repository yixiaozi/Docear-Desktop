package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.util.TextUtils;

/**
 * Human-readable labels for DocearReminder cycle types.
 */
final class ReminderCycleTypeFormatter {

	private ReminderCycleTypeFormatter() {
	}

	static String format(final ReminderCycleAttributes.CycleConfig config) {
		if (config == null || !config.isRecurring()) {
			return TextUtils.getText("plugins/TimeManagement.xml_cycleType_onetime");
		}
		final String type = config.remindType;
		final int interval = config.interval <= 0 ? 1 : config.interval;
		if (ReminderCycleAttributes.TYPE_HOUR.equals(type)) {
			return interval == 1 ? TextUtils.getText("plugins/TimeManagement.xml_cycleType_hour")
					: TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNHours", interval);
		}
		if (ReminderCycleAttributes.TYPE_DAY.equals(type)) {
			return interval == 1 ? TextUtils.getText("plugins/TimeManagement.xml_cycleType_day")
					: TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNDays", interval);
		}
		if (ReminderCycleAttributes.TYPE_WEEK.equals(type)) {
			final String weekdays = formatWeekDays(config.weekDays);
			if (interval == 1) {
				return weekdays.length() == 0 ? TextUtils.getText("plugins/TimeManagement.xml_cycleType_week")
						: TextUtils.format("plugins/TimeManagement.xml_cycleType_weekOn", weekdays);
			}
			return weekdays.length() == 0 ? TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNWeeks",
					interval) : TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNWeeksOn", interval,
					weekdays);
		}
		if (ReminderCycleAttributes.TYPE_MONTH.equals(type)) {
			return interval == 1 ? TextUtils.getText("plugins/TimeManagement.xml_cycleType_month")
					: TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNMonths", interval);
		}
		if (ReminderCycleAttributes.TYPE_YEAR.equals(type)) {
			return interval == 1 ? TextUtils.getText("plugins/TimeManagement.xml_cycleType_year")
					: TextUtils.format("plugins/TimeManagement.xml_cycleType_everyNYears", interval);
		}
		if (ReminderCycleAttributes.TYPE_EB.equals(type)) {
			return TextUtils.getText("plugins/TimeManagement.xml_cycleType_eb");
		}
		return type;
	}

	private static String formatWeekDays(final String weekDays) {
		if (weekDays == null || weekDays.length() == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < weekDays.length(); i++) {
			final char day = weekDays.charAt(i);
			final String label = weekDayLabel(day);
			if (label.length() > 0) {
				if (sb.length() > 0) {
					sb.append('\u3001');
				}
				sb.append(label);
			}
		}
		return sb.toString();
	}

	private static String weekDayLabel(final char day) {
		switch (day) {
			case '1':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_monday_label");
			case '2':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_tuesday_label");
			case '3':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_wednesday_label");
			case '4':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_thursday_label");
			case '5':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_friday_label");
			case '6':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_saturday_label");
			case '7':
				return TextUtils.getText("plugins/TimeManagement.xml_weekday_sunday_label");
			default:
				return "";
		}
	}
}
