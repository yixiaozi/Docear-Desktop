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

	public static final class ItemEntry {
		public final File mapFile;
		public final String nodeId;
		public final String nodeText;

		public ItemEntry(File mapFile, String nodeId, String nodeText) {
			this.mapFile = mapFile;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
		}
	}

	public static final class ModifiedEntry {
		public final File mapFile;
		public final String nodeId;
		public final String nodeText;
		public final long modifiedAt;

		public ModifiedEntry(File mapFile, String nodeId, String nodeText, long modifiedAt) {
			this.mapFile = mapFile;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.modifiedAt = modifiedAt;
		}
	}

	private final List oneTimeReminders;
	private final List recurringReminders;
	private final List todos;
	private final List pinnedEntries;
	private final List publishedEntries;
	private final List recentlyModifiedEntries;

	public WorkspaceSideTabSnapshot(List oneTimeReminders, List recurringReminders, List todos) {
		this(oneTimeReminders, recurringReminders, todos, Collections.EMPTY_LIST);
	}

	public WorkspaceSideTabSnapshot(List oneTimeReminders, List recurringReminders, List todos, List pinnedEntries) {
		this(oneTimeReminders, recurringReminders, todos, pinnedEntries, Collections.EMPTY_LIST,
				Collections.EMPTY_LIST);
	}

	public WorkspaceSideTabSnapshot(List oneTimeReminders, List recurringReminders, List todos, List pinnedEntries,
			List publishedEntries, List recentlyModifiedEntries) {
		this.oneTimeReminders = oneTimeReminders != null ? oneTimeReminders : Collections.EMPTY_LIST;
		this.recurringReminders = recurringReminders != null ? recurringReminders : Collections.EMPTY_LIST;
		this.todos = todos != null ? todos : Collections.EMPTY_LIST;
		this.pinnedEntries = pinnedEntries != null ? pinnedEntries : Collections.EMPTY_LIST;
		this.publishedEntries = publishedEntries != null ? publishedEntries : Collections.EMPTY_LIST;
		this.recentlyModifiedEntries = recentlyModifiedEntries != null ? recentlyModifiedEntries
				: Collections.EMPTY_LIST;
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

	public List getPublishedEntries() {
		return publishedEntries;
	}

	public List getRecentlyModifiedEntries() {
		return recentlyModifiedEntries;
	}

	public boolean hasAnyItems() {
		return !oneTimeReminders.isEmpty() || !recurringReminders.isEmpty() || !todos.isEmpty()
				|| !pinnedEntries.isEmpty() || !publishedEntries.isEmpty() || !recentlyModifiedEntries.isEmpty();
	}

	public static WorkspaceSideTabSnapshot empty() {
		return new WorkspaceSideTabSnapshot(new ArrayList(), new ArrayList(), new ArrayList());
	}
}
