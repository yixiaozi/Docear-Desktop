package org.docear.plugin.core.todoist;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;

public final class TodoistSyncService {
	private TodoistSyncService() {
	}

	public static TodoistSyncResult syncAllReminders() {
		return syncAllReminders(null);
	}

	public static TodoistSyncResult syncAllReminders(TodoistSyncProgressCallback callback) {
		final TodoistSyncResult result = new TodoistSyncResult();
		final String token = TodoistConfig.getApiToken();
		if (token == null || token.trim().length() == 0) {
			result.failed = 1;
			result.errorMessage = "Todoist API token is not configured.";
			result.failedLines.add(result.errorMessage);
			notifyFailed(callback, result.errorMessage);
			finish(callback, result);
			return result;
		}
		status(callback, TextUtils.getText("todoist.sync.status.connecting"));
		final TodoistApiClient client = new TodoistApiClient(token.trim());
		final TodoistMappingStore store = new TodoistMappingStore();
		final TodoistSectionStore sectionStore = new TodoistSectionStore();
		final String projectName = TodoistConfig.getProjectName();
		result.projectName = projectName;
		try {
			status(callback, TextUtils.format("todoist.sync.status.project", new Object[] { projectName }));
			result.projectId = client.ensureProject(projectName);
		}
		catch (Exception e) {
			result.failed = 1;
			result.errorMessage = "Could not access Todoist project: " + e.getMessage();
			result.failedLines.add(result.errorMessage);
			notifyFailed(callback, result.errorMessage);
			LogUtils.warn("Todoist project setup failed", e);
			finish(callback, result);
			return result;
		}
		status(callback, TextUtils.getText("todoist.sync.status.scanning"));
		final List reminders = new MindMapReminderScanner().scanAllReminders();
		result.totalScanned = reminders.size();
		status(callback, TextUtils.format("todoist.sync.status.found", new Object[] { Integer.valueOf(reminders.size()) }));
		final Set activeKeys = new HashSet();
		final int total = reminders.size();
		for (int i = 0; i < reminders.size(); i++) {
			TodoistReminderRecord record = (TodoistReminderRecord) reminders.get(i);
			String key = record.syncKey();
			activeKeys.add(key);
			String sectionName = TodoistApiClient.sectionNameForFile(record.file);
			String hash = contentHash(record, sectionName);
			String taskId = store.getTaskIdOnly(key);
			String storedHash = store.getStoredContentHash(key);
			String line = TodoistSyncResult.formatLine(sectionName, record);
			progress(callback, i + 1, total);
			status(callback, TextUtils.format("todoist.sync.status.item", new Object[] { sectionName, record.nodeText }));
			try {
				String sectionId = client.ensureSection(result.projectId, sectionName, sectionStore);
				if (taskId != null && taskId.length() > 0) {
					TodoistTaskLocation location = client.getTaskLocation(taskId);
					boolean needsRelocate = !client.isTaskInLocation(location, result.projectId, sectionId);
					if (hash.equals(storedHash) && !needsRelocate) {
						result.addSkipped(record, sectionName);
						if (callback != null) {
							callback.onSkipped(line);
						}
					}
					else if (location.exists) {
						boolean needsContentUpdate = !hash.equals(storedHash);
						if (needsRelocate) {
							client.relocateTaskTo(taskId, result.projectId, sectionId);
						}
						if (needsContentUpdate) {
							client.updateTaskContent(taskId, record);
						}
						store.putMapping(key, taskId, record.remindAt, hash);
						if (needsRelocate) {
							result.addMoved(record, sectionName);
							if (callback != null) {
								callback.onUpdated(TextUtils.getText("todoist.sync.live.moved") + " " + line);
							}
						}
						else {
							result.addUpdated(record, sectionName);
							if (callback != null) {
								callback.onUpdated(line);
							}
						}
					}
					else {
						taskId = client.createTask(record, result.projectId, sectionId);
						store.putMapping(key, taskId, record.remindAt, hash);
						result.addCreated(record, sectionName);
						if (callback != null) {
							callback.onCreated(line);
						}
					}
				}
				else {
					taskId = client.createTask(record, result.projectId, sectionId);
					store.putMapping(key, taskId, record.remindAt, hash);
					result.addCreated(record, sectionName);
					if (callback != null) {
						callback.onCreated(line);
					}
				}
			}
			catch (Exception e) {
				String failedLine = line + " — " + e.getMessage();
				result.addFailed(record, sectionName, e.getMessage());
				if (callback != null) {
					callback.onFailed(failedLine);
				}
				LogUtils.warn("Todoist sync failed for " + key, e);
			}
		}
		cleanupMisplacedTasks(client, result, store, callback, activeKeys);
		status(callback, TextUtils.getText("todoist.sync.status.cleanup"));
		for (Iterator it = store.keySet().iterator(); it.hasNext();) {
			String key = (String) it.next();
			if (!activeKeys.contains(key)) {
				String taskId = store.getTaskIdOnly(key);
				if (taskId != null && taskId.length() > 0) {
					try {
						client.closeTask(taskId);
						result.addClosed(key);
						if (callback != null) {
							callback.onClosed(key);
						}
					}
					catch (Exception e) {
						result.failed++;
						String failedLine = "Close " + taskId + ": " + e.getMessage();
						result.failedLines.add(failedLine);
						if (callback != null) {
							callback.onFailed(failedLine);
						}
						LogUtils.warn("Todoist close failed for " + key, e);
						continue;
					}
				}
				store.removeMapping(key);
			}
		}
		store.save();
		sectionStore.save();
		finish(callback, result);
		return result;
	}

	private static void cleanupMisplacedTasks(TodoistApiClient client, TodoistSyncResult result,
			TodoistMappingStore store, TodoistSyncProgressCallback callback, Set activeKeys) {
		status(callback, TextUtils.getText("todoist.sync.status.repair"));
		final Set mappedTaskIds = store.getAllMappedTaskIds();
		try {
			closeOrphansInWrongProjects(client, result, callback, mappedTaskIds);
			closeDuplicateTasksInProject(client, result.projectId, activeKeys, mappedTaskIds, result, callback);
		}
		catch (Exception e) {
			result.failed++;
			String failedLine = TextUtils.getText("todoist.sync.repair.failed") + ": " + e.getMessage();
			result.failedLines.add(failedLine);
			if (callback != null) {
				callback.onFailed(failedLine);
			}
			LogUtils.warn("Todoist repair pass failed", e);
		}
	}

	private static void closeOrphansInWrongProjects(TodoistApiClient client, TodoistSyncResult result,
			TodoistSyncProgressCallback callback, Set mappedTaskIds) throws IOException {
		final List projectIds = client.findAllProjectIdsByName(result.projectName);
		for (int p = 0; p < projectIds.size(); p++) {
			String projectId = (String) projectIds.get(p);
			if (result.projectId.equals(projectId)) {
				continue;
			}
			closeOrphanTasks(client, projectId, result.projectName, mappedTaskIds, result, callback, false, null);
		}
	}

	private static void closeDuplicateTasksInProject(TodoistApiClient client, String projectId, Set activeKeys,
			Set mappedTaskIds, TodoistSyncResult result, TodoistSyncProgressCallback callback) throws IOException {
		closeOrphanTasks(client, projectId, result.projectName, mappedTaskIds, result, callback, true, activeKeys);
	}

	private static void closeOrphanTasks(TodoistApiClient client, String projectId, String projectName,
			Set mappedTaskIds, TodoistSyncResult result, TodoistSyncProgressCallback callback,
			boolean onlyActiveDuplicates, Set activeKeys) throws IOException {
		final List orphanTaskIds = client.listDocearTaskIdsInProject(projectId);
		for (int t = 0; t < orphanTaskIds.size(); t++) {
			String orphanTaskId = (String) orphanTaskIds.get(t);
			if (mappedTaskIds.contains(orphanTaskId)) {
				continue;
			}
			if (onlyActiveDuplicates) {
				String description = client.getTaskDescription(orphanTaskId);
				String syncKey = TodoistApiClient.syncKeyFromDescription(description);
				if (syncKey == null || !activeKeys.contains(syncKey)) {
					continue;
				}
			}
			try {
				client.closeTask(orphanTaskId);
				String line = TextUtils.format("todoist.sync.repair.closed_orphan",
						new Object[] { projectName, orphanTaskId });
				result.addClosed(line);
				if (callback != null) {
					callback.onClosed(TextUtils.getText("todoist.sync.live.repaired") + " " + line);
				}
			}
			catch (Exception e) {
				result.failed++;
				String failedLine = "Close orphan " + orphanTaskId + ": " + e.getMessage();
				result.failedLines.add(failedLine);
				if (callback != null) {
					callback.onFailed(failedLine);
				}
				LogUtils.warn("Todoist orphan cleanup failed for " + orphanTaskId, e);
			}
		}
	}

	private static void status(TodoistSyncProgressCallback callback, String message) {
		if (callback != null) {
			callback.onStatus(message);
		}
	}

	private static void progress(TodoistSyncProgressCallback callback, int current, int total) {
		if (callback != null) {
			callback.onProgress(current, total);
		}
	}

	private static void notifyFailed(TodoistSyncProgressCallback callback, String message) {
		if (callback != null) {
			callback.onFailed(message);
		}
	}

	private static void finish(TodoistSyncProgressCallback callback, TodoistSyncResult result) {
		if (callback != null) {
			callback.onFinished(result);
		}
	}

	private static String contentHash(TodoistReminderRecord record, String sectionName) {
		StringBuilder sb = new StringBuilder();
		sb.append(sectionName).append('|');
		sb.append(record.nodeText).append('|');
		sb.append(record.remindAt).append('|');
		sb.append(record.recurring).append('|');
		sb.append(record.period).append('|');
		sb.append(record.periodUnit);
		return Integer.toString(sb.toString().hashCode());
	}
}
