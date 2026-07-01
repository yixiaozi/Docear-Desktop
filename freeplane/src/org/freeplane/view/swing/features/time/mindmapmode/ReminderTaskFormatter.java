package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.util.TextUtils;

/**
 * Formatting helpers for DocearReminder task metadata display.
 */
final class ReminderTaskFormatter {

	private ReminderTaskFormatter() {
	}

	static String formatDurationMinutes(final int taskTimeMinutes) {
		if (taskTimeMinutes <= 0) {
			return "";
		}
		return Integer.toString(taskTimeMinutes) + TextUtils.getText("plugins/TimeManagement.xml_taskDurationUnit");
	}

	static String formatDurationPadding(final int taskTimeMinutes, final int width) {
		if (taskTimeMinutes <= 0) {
			return padLeft("", width);
		}
		return padLeft(Integer.toString(taskTimeMinutes), width);
	}

	static String formatLevel(final int taskLevel) {
		return taskLevel == 0 ? "" : Integer.toString(taskLevel);
	}

	static String formatUrgency(final int jinji) {
		return jinji == 0 ? "" : Integer.toString(jinji);
	}

	static String formatTooltipLines(final ReminderTaskAttributes.TaskConfig config) {
		if (config == null || config.isEmpty()) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		if (config.taskTime > 0) {
			sb.append(TextUtils.getText("plugins/TimeManagement.xml_taskDurationLabel")).append(": ")
					.append(formatDurationMinutes(config.taskTime));
		}
		if (config.taskLevel != 0) {
			if (sb.length() > 0) {
				sb.append("  ");
			}
			sb.append(TextUtils.getText("plugins/TimeManagement.xml_taskLevelLabel")).append(": ")
					.append(config.taskLevel);
		}
		if (config.jinji != 0) {
			if (sb.length() > 0) {
				sb.append("  ");
			}
			sb.append(TextUtils.getText("plugins/TimeManagement.xml_taskUrgencyLabel")).append(": ").append(config.jinji);
		}
		return sb.toString();
	}

	static String appendInlineDuration(final String prefix, final ReminderTaskAttributes.TaskConfig config) {
		if (prefix == null || prefix.length() == 0 || config == null || config.taskTime <= 0) {
			return prefix == null ? "" : prefix;
		}
		return prefix + " " + formatDurationPadding(config.taskTime, 4);
	}

	private static String padLeft(final String value, final int width) {
		if (value.length() >= width) {
			return value;
		}
		final StringBuilder sb = new StringBuilder(width);
		for (int i = value.length(); i < width; i++) {
			sb.append(' ');
		}
		sb.append(value);
		return sb.toString();
	}
}
