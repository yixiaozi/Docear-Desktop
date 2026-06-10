package org.freeplane.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 保存侧栏 Tab 已扫描结果的内存快照，避免 AI 等功能重复全量扫描 .mm 文件。
 */
public final class WorkspaceSideTabSnapshotRegistry {

	private static volatile List oneTimeReminders = Collections.EMPTY_LIST;
	private static volatile List recurringReminders = Collections.EMPTY_LIST;
	private static volatile List todos = Collections.EMPTY_LIST;
	private static volatile List pinnedEntries = Collections.EMPTY_LIST;
	private static volatile long lastUpdatedAt;

	private WorkspaceSideTabSnapshotRegistry() {
	}

	public static void updateOneTimeReminders(List entries) {
		oneTimeReminders = copyReminderList(entries);
		lastUpdatedAt = System.currentTimeMillis();
	}

	public static void updateRecurringReminders(List entries) {
		recurringReminders = copyReminderList(entries);
		lastUpdatedAt = System.currentTimeMillis();
	}

	public static void updateTodos(List entries) {
		todos = copyTodoList(entries);
		lastUpdatedAt = System.currentTimeMillis();
	}

	public static void updatePinnedEntries(List entries) {
		pinnedEntries = copyPinnedList(entries);
		lastUpdatedAt = System.currentTimeMillis();
	}

	public static WorkspaceSideTabSnapshot getSnapshot() {
		return new WorkspaceSideTabSnapshot(oneTimeReminders, recurringReminders, todos, pinnedEntries);
	}

	public static boolean hasSnapshotData() {
		return !oneTimeReminders.isEmpty() || !recurringReminders.isEmpty() || !todos.isEmpty()
				|| !pinnedEntries.isEmpty();
	}

	public static long getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	private static List copyReminderList(List entries) {
		if (entries == null || entries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(new ArrayList(entries));
	}

	private static List copyTodoList(List entries) {
		if (entries == null || entries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(new ArrayList(entries));
	}

	private static List copyPinnedList(List entries) {
		if (entries == null || entries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(new ArrayList(entries));
	}
}
