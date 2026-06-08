package org.docear.plugin.core.todoist;

final class TodoistImportTask {
	final String id;
	final String content;
	final String description;
	final String projectId;
	final String sectionId;
	final long dueAtMillis;
	final boolean recurring;
	final String dueString;

	TodoistImportTask(String id, String content, String description, String projectId, String sectionId,
			long dueAtMillis, boolean recurring, String dueString) {
		this.id = id;
		this.content = content == null ? "" : content;
		this.description = description == null ? "" : description;
		this.projectId = projectId;
		this.sectionId = sectionId;
		this.dueAtMillis = dueAtMillis;
		this.recurring = recurring;
		this.dueString = dueString;
	}
}
