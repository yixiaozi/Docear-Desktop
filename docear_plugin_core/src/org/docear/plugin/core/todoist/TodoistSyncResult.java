package org.docear.plugin.core.todoist;

import java.util.ArrayList;
import java.util.List;

final class TodoistSyncResult {
	int totalScanned;
	int created;
	int updated;
	int skipped;
	int closed;
	int failed;
	String projectName;
	String projectId;
	String errorMessage;
	final List createdLines = new ArrayList();
	final List updatedLines = new ArrayList();
	final List skippedLines = new ArrayList();
	final List failedLines = new ArrayList();
	final List closedLines = new ArrayList();

	void appendError(String message) {
		if (errorMessage == null) {
			errorMessage = message;
		}
		else {
			errorMessage = errorMessage + "\n" + message;
		}
	}

	void addCreated(TodoistReminderRecord record, String sectionName) {
		created++;
		createdLines.add(formatLine(sectionName, record));
	}

	void addUpdated(TodoistReminderRecord record, String sectionName) {
		updated++;
		updatedLines.add(formatLine(sectionName, record));
	}

	void addSkipped(TodoistReminderRecord record, String sectionName) {
		skipped++;
		skippedLines.add(formatLine(sectionName, record));
	}

	void addFailed(TodoistReminderRecord record, String sectionName, String reason) {
		failed++;
		failedLines.add(formatLine(sectionName, record) + " — " + reason);
	}

	void addClosed(String line) {
		closed++;
		closedLines.add(line);
	}

	static String formatLine(String sectionName, TodoistReminderRecord record) {
		return "[" + sectionName + "] " + record.nodeText;
	}
}
