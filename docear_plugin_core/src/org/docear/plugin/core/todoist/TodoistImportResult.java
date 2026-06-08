package org.docear.plugin.core.todoist;

final class TodoistImportResult {
	int totalFetched;
	int created;
	int updated;
	int skipped;
	int failed;
	String targetFile;
	String errorMessage;
	final java.util.List createdLines = new java.util.ArrayList();
	final java.util.List updatedLines = new java.util.ArrayList();
	final java.util.List skippedLines = new java.util.ArrayList();
	final java.util.List failedLines = new java.util.ArrayList();

	void addCreated(String line) {
		created++;
		createdLines.add(line);
	}

	void addUpdated(String line) {
		updated++;
		updatedLines.add(line);
	}

	void addSkipped(String line) {
		skipped++;
		skippedLines.add(line);
	}

	void addFailed(String line) {
		failed++;
		failedLines.add(line);
	}
}
