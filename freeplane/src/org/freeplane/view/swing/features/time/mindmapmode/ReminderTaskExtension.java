package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

/**
 * DocearReminder-compatible task metadata on a mind map node.
 */
final class ReminderTaskExtension implements IExtension {
	private int taskTime;
	private int taskLevel;
	private int jinji;

	static ReminderTaskExtension getExtension(final NodeModel node) {
		return node == null ? null : (ReminderTaskExtension) node.getExtension(ReminderTaskExtension.class);
	}

	static ReminderTaskExtension getOrCreateExtension(final NodeModel node) {
		ReminderTaskExtension extension = getExtension(node);
		if (extension == null) {
			extension = new ReminderTaskExtension();
			node.addExtension(extension);
		}
		return extension;
	}

	ReminderTaskAttributes.TaskConfig toConfig() {
		return new ReminderTaskAttributes.TaskConfig(taskTime, taskLevel, jinji);
	}

	void applyConfig(final ReminderTaskAttributes.TaskConfig config) {
		if (config == null) {
			taskTime = 0;
			taskLevel = 0;
			jinji = 0;
			return;
		}
		taskTime = config.taskTime;
		taskLevel = config.taskLevel;
		jinji = config.jinji;
	}

	void clear() {
		taskTime = 0;
		taskLevel = 0;
		jinji = 0;
	}

	boolean isEmpty() {
		return taskTime == 0 && taskLevel == 0 && jinji == 0;
	}

	int getTaskTime() {
		return taskTime;
	}

	void setTaskTime(final int taskTime) {
		this.taskTime = Math.max(0, taskTime);
	}

	int getTaskLevel() {
		return taskLevel;
	}

	void setTaskLevel(final int taskLevel) {
		this.taskLevel = taskLevel;
	}

	int getJinji() {
		return jinji;
	}

	void setJinji(final int jinji) {
		this.jinji = jinji;
	}
}
