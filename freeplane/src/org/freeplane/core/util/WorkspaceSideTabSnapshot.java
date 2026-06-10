package org.freeplane.core.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 侧栏 Tab（全部提醒 / 周期提醒 / 全部待办）导出的轻量快照，供 AI 等模块读取。
 */
public final class WorkspaceSideTabSnapshot {

	public static final class ReminderEntry {
		public final File mapFile;
		public final String nodeId;
		public final String nodeText;
		public final long remindAt;
		public final boolean recurring;
		public final String remindType;

		public ReminderEntry(File mapFile, String nodeId, String nodeText, long remindAt, boolean recurring,
				String remindType) {
			this.mapFile = mapFile;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.remindAt = remindAt;
			this.recurring = recurring;
			this.remindType = remindType;
		}
	}

	public static final class TodoEntry {
		public final File mapFile;
		public final String nodeId;
		public final String nodeText;

		public TodoEntry(File mapFile, String nodeId, String nodeText) {
			this.mapFile = mapFile;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
		}
	}

	public static final class PinnedEntry {
		public final File mapFile;
		public final String nodeId;
		public final String nodeText;
		public final String tags;

		public PinnedEntry(File mapFile, String nodeId, String nodeText, String tags) {
			this.mapFile = mapFile;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.tags = tags != null ? tags : "";
		}
	}

	private final List oneTimeReminders;
	private final List recurringReminders;
	private final List todos;
	private final List pinnedEntries;

	public WorkspaceSideTabSnapshot(List oneTimeReminders, List recurringReminders, List todos) {
		this(oneTimeReminders, recurringReminders, todos, Collections.EMPTY_LIST);
	}

	public WorkspaceSideTabSnapshot(List oneTimeReminders, List recurringReminders, List todos, List pinnedEntries) {
		this.oneTimeReminders = oneTimeReminders != null ? oneTimeReminders : Collections.EMPTY_LIST;
		this.recurringReminders = recurringReminders != null ? recurringReminders : Collections.EMPTY_LIST;
		this.todos = todos != null ? todos : Collections.EMPTY_LIST;
		this.pinnedEntries = pinnedEntries != null ? pinnedEntries : Collections.EMPTY_LIST;
	}

	public List getOneTimeReminders() {
		return oneTimeReminders;
	}

	public List getRecurringReminders() {
		return recurringReminders;
	}

	public List getTodos() {
		return todos;
	}

	public List getPinnedEntries() {
		return pinnedEntries;
	}

	public boolean hasAnyItems() {
		return !oneTimeReminders.isEmpty() || !recurringReminders.isEmpty() || !todos.isEmpty()
				|| !pinnedEntries.isEmpty();
	}

	public static WorkspaceSideTabSnapshot empty() {
		return new WorkspaceSideTabSnapshot(new ArrayList(), new ArrayList(), new ArrayList());
	}
}
