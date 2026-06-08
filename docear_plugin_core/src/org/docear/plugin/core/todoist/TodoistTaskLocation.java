package org.docear.plugin.core.todoist;

final class TodoistTaskLocation {
	final boolean exists;
	final String projectId;
	final String sectionId;

	private TodoistTaskLocation(boolean exists, String projectId, String sectionId) {
		this.exists = exists;
		this.projectId = projectId;
		this.sectionId = sectionId;
	}

	static TodoistTaskLocation found(String projectId, String sectionId) {
		return new TodoistTaskLocation(true, projectId, sectionId);
	}

	static TodoistTaskLocation notFound() {
		return new TodoistTaskLocation(false, null, null);
	}
}
