package org.freeplane.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * 保存侧栏 Tab 已扫描结果的内存快照，避免 AI 等功能重复全量扫描 .mm 文件。
 */
public final class WorkspaceSideTabSnapshotRegistry {

	private static volatile List oneTimeReminders = Collections.EMPTY_LIST;
	private static volatile List recurringReminders = Collections.EMPTY_LIST;
	private static volatile List todos = Collections.EMPTY_LIST;
	private static volatile List pinnedEntries = Collections.EMPTY_LIST;
	private static volatile List publishedEntries = Collections.EMPTY_LIST;
	private static volatile List recentlyModifiedEntries = Collections.EMPTY_LIST;
	private static volatile long lastUpdatedAt;
	private static final List changeListeners = Collections.synchronizedList(new ArrayList());

	private WorkspaceSideTabSnapshotRegistry() {
	}

	public static void addChangeListener(final Runnable listener) {
		if (listener != null && !changeListeners.contains(listener)) {
			changeListeners.add(listener);
		}
	}

	public static void removeChangeListener(final Runnable listener) {
		changeListeners.remove(listener);
	}

	public static void updateOneTimeReminders(List entries) {
		oneTimeReminders = copyReminderList(entries);
		fireChanged();
	}

	public static void updateRecurringReminders(List entries) {
		recurringReminders = copyReminderList(entries);
		fireChanged();
	}

	public static void updateTodos(List entries) {
		todos = copyTodoList(entries);
		fireChanged();
	}

	public static void updatePinnedEntries(List entries) {
		pinnedEntries = copyPinnedList(entries);
		fireChanged();
	}

	public static void updatePublishedEntries(List entries) {
		publishedEntries = copyItemList(entries);
		fireChanged();
	}

	public static void updateRecentlyModifiedEntries(List entries) {
		recentlyModifiedEntries = copyModifiedList(entries);
		fireChanged();
	}

	public static WorkspaceSideTabSnapshot getSnapshot() {
		return new WorkspaceSideTabSnapshot(oneTimeReminders, recurringReminders, todos, pinnedEntries,
				publishedEntries, recentlyModifiedEntries);
	}

	public static boolean hasSnapshotData() {
		return !oneTimeReminders.isEmpty() || !recurringReminders.isEmpty() || !todos.isEmpty()
				|| !pinnedEntries.isEmpty() || !publishedEntries.isEmpty() || !recentlyModifiedEntries.isEmpty();
	}

	public static long getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	private static void fireChanged() {
		lastUpdatedAt = System.currentTimeMillis();
		final Runnable[] listeners;
		synchronized (changeListeners) {
			listeners = (Runnable[]) changeListeners.toArray(new Runnable[changeListeners.size()]);
		}
		for (int i = 0; i < listeners.length; i++) {
			try {
				listeners[i].run();
			}
			catch (final Exception e) {
				LogUtils.warn("WorkspaceSideTabSnapshotRegistry listener failed: " + e.getMessage());
			}
		}
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

	private static List copyItemList(List entries) {
		if (entries == null || entries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(new ArrayList(entries));
	}

	private static List copyModifiedList(List entries) {
		if (entries == null || entries.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		return Collections.unmodifiableList(new ArrayList(entries));
	}
}
