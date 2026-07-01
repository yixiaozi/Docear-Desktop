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

	RecurringReminderEntry(final File file, final String nodeId, final String nodeText, final long remindAt,
			final ReminderCycleAttributes.CycleConfig cycleConfig) {
		this.file = file;
		this.nodeId = nodeId;
		this.nodeText = nodeText;
		this.remindAt = remindAt;
		this.cycleConfig = cycleConfig;
	}
}
