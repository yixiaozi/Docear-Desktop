package org.freeplane.view.swing.features.time.mindmapmode;

import java.io.File;

/**
 * Unified reminder reference for calendar and timeline views.
 */
final class ReminderCalendarEntry {
	final File file;
	final String nodeId;
	final String nodeText;
	final long remindAt;
	final boolean recurring;
	final ReminderCycleAttributes.CycleConfig cycleConfig;
	final int taskTime;
	final int taskLevel;
	final int jinji;

	ReminderCalendarEntry(final File file, final String nodeId, final String nodeText, final long remindAt,
			final boolean recurring, final ReminderCycleAttributes.CycleConfig cycleConfig, final int taskTime,
			final int taskLevel, final int jinji) {
		this.file = file;
		this.nodeId = nodeId;
		this.nodeText = nodeText;
		this.remindAt = remindAt;
		this.recurring = recurring;
		this.cycleConfig = cycleConfig;
		this.taskTime = taskTime;
		this.taskLevel = taskLevel;
		this.jinji = jinji;
	}

	static ReminderCalendarEntry fromRecurring(final RecurringReminderEntry entry) {
		if (entry == null) {
			return null;
		}
		return new ReminderCalendarEntry(entry.file, entry.nodeId, entry.nodeText, entry.remindAt, true,
				entry.cycleConfig, entry.taskTime, entry.taskLevel, entry.jinji);
	}

	RecurringReminderEntry toRecurringEntry() {
		if (!recurring || cycleConfig == null) {
			return null;
		}
		return new RecurringReminderEntry(file, nodeId, nodeText, remindAt, cycleConfig, taskTime, taskLevel, jinji);
	}
}
