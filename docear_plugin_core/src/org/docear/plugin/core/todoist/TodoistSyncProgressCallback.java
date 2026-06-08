package org.docear.plugin.core.todoist;

interface TodoistSyncProgressCallback {
	void onStatus(String message);

	void onProgress(int current, int total);

	void onCreated(String line);

	void onSkipped(String line);

	void onUpdated(String line);

	void onFailed(String line);

	void onClosed(String line);

	void onFinished(TodoistSyncResult result);
}
