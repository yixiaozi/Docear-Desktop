package org.freeplane.view.swing.features.time.mindmapmode;

import java.io.File;

/**
 * Lightweight recurring reminder reference for list, calendar and postpone actions.
 */
final class RecurringReminderEntry {
	final File file;
	final String nodeId;
	final String nodeText;
	final long remindAt;
	final ReminderCycleAttributes.CycleConfig cycleConfig;
	final int taskTime;
	final int taskLevel;
	final int jinji;

	RecurringReminderEntry(final File file, final String nodeId, final String nodeText, final long remindAt,
			final ReminderCycleAttributes.CycleConfig cycleConfig, final int taskTime, final int taskLevel,
			final int jinji) {
		this.file = file;
		this.nodeId = nodeId;
		this.nodeText = nodeText;
		this.remindAt = remindAt;
		this.cycleConfig = cycleConfig;
		this.taskTime = taskTime;
		this.taskLevel = taskLevel;
		this.jinji = jinji;
	}

	RecurringReminderEntry(final File file, final String nodeId, final String nodeText, final long remindAt,
			final ReminderCycleAttributes.CycleConfig cycleConfig) {
		this(file, nodeId, nodeText, remindAt, cycleConfig, 0, 0, 0);
	}
}
