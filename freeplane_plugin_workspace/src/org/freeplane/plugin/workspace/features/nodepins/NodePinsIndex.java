package org.freeplane.plugin.workspace.features.nodepins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.SwingWorker;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.DetailTextModel;

public final class NodePinsIndex {

	private static NodePinsIndex instance;

	private final List entries = new ArrayList();
	private final List changeListeners = new ArrayList();
	private SwingWorker activeWorker;
	private boolean rescanRequested;

	private NodePinsIndex() {
	}

	public static synchronized NodePinsIndex getInstance() {
		if (instance == null) {
			instance = new NodePinsIndex();
		}
		return instance;
	}

	public void scheduleRescan() {
		rescan();
	}

	public void updateFromNode(final NodeModel node) {
		if (node == null) {
			return;
		}
		final String key = NodePinKeyUtils.fromNode(node);
		if (key == null) {
			return;
		}
		final String detailsHtml = DetailTextModel.getDetailTextText(node);
		synchronized (entries) {
			if (!NodeDetailsTagUtils.hasAnyManagedTag(detailsHtml)) {
				for (final Iterator it = entries.iterator(); it.hasNext();) {
					if (key.equals(((NodePinEntry) it.next()).getKey())) {
						it.remove();
						break;
					}
				}
			}
			else {
				final Set allTags = NodeDetailsTagUtils.parseAllTags(detailsHtml);
				final boolean pinned = allTags.contains(NodeDetailsTagUtils.PIN_TAG);
				final LinkedHashSet userTags = new LinkedHashSet(allTags);
				userTags.remove(NodeDetailsTagUtils.PIN_TAG);
				final String label = NodeMindMapActionUtils.getNodePlainText(node);
				final NodePinEntry newEntry = new NodePinEntry(key, userTags, pinned, label);
				boolean found = false;
				for (int i = 0; i < entries.size(); i++) {
					if (key.equals(((NodePinEntry) entries.get(i)).getKey())) {
						entries.set(i, newEntry);
						found = true;
						break;
					}
				}
				if (!found) {
					entries.add(newEntry);
				}
			}
		}
		fireChanged();
	}

	public void rescan() {
		if (activeWorker != null) {
			rescanRequested = true;
			return;
		}
		rescanRequested = false;
		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				return NodeDetailsTagScanner.scanAllProjects();
			}

			protected void done() {
				try {
					final Object result = get();
					if (result instanceof List) {
						synchronized (entries) {
							entries.clear();
							entries.addAll((List) result);
						}
						fireChanged();
					}
				}
				catch (final Exception e) {
					// ignore
				}
				activeWorker = null;
				if (rescanRequested) {
					rescan();
				}
			}
		};
		activeWorker.execute();
	}

	public List getDisplayEntries(final boolean pinsMode, final String tagFilter) {
		synchronized (entries) {
			if (pinsMode) {
				final List result = new ArrayList();
				for (int i = 0; i < entries.size(); i++) {
					final NodePinEntry entry = (NodePinEntry) entries.get(i);
					if (entry.isPinned() && !entry.getTags().contains(NodeDetailsTagUtils.TAG_ARCHIVED)) {
						result.add(entry);
					}
				}
				return result;
			}
			if (tagFilter == null || tagFilter.length() == 0) {
				return new ArrayList(entries);
			}
			final List result = new ArrayList();
			for (int i = 0; i < entries.size(); i++) {
				final NodePinEntry entry = (NodePinEntry) entries.get(i);
				if (entry.getTags().contains(tagFilter)) {
					result.add(entry);
				}
			}
			Collections.sort(result, new Comparator() {
				public int compare(final Object o1, final Object o2) {
					final NodePinEntry a = (NodePinEntry) o1;
					final NodePinEntry b = (NodePinEntry) o2;
					final int mapCompare = a.getMapDisplayName().compareTo(b.getMapDisplayName());
					if (mapCompare != 0) {
						return mapCompare;
					}
					return a.getListNodeLabel().compareTo(b.getListNodeLabel());
				}
			});
			return result;
		}
	}

	public Set getQuickSelectTags() {
		final LinkedHashSet quickTags = new LinkedHashSet();
		for (int i = 0; i < NodeDetailsTagUtils.PRESET_TAGS.length; i++) {
			quickTags.add(NodeDetailsTagUtils.PRESET_TAGS[i]);
		}
		synchronized (entries) {
			for (int i = 0; i < entries.size(); i++) {
				final NodePinEntry entry = (NodePinEntry) entries.get(i);
				for (final Iterator it = entry.getTags().iterator(); it.hasNext();) {
					final String tag = (String) it.next();
					if (NodeDetailsTagUtils.isValidTagName(tag)) {
						quickTags.add(tag);
					}
				}
			}
		}
		quickTags.remove(NodeDetailsTagUtils.PIN_TAG);
		return quickTags;
	}

	public int countPinned() {
		int count = 0;
		synchronized (entries) {
			for (int i = 0; i < entries.size(); i++) {
				final NodePinEntry entry = (NodePinEntry) entries.get(i);
				if (entry.isPinned() && !entry.getTags().contains(NodeDetailsTagUtils.TAG_ARCHIVED)) {
					count++;
				}
			}
		}
		return count;
	}

	public int countWithTag(final String tag) {
		if (tag == null || tag.length() == 0) {
			return countPinned();
		}
		int count = 0;
		synchronized (entries) {
			for (int i = 0; i < entries.size(); i++) {
				final NodePinEntry entry = (NodePinEntry) entries.get(i);
				if (entry.getTags().contains(tag)) {
					count++;
				}
			}
		}
		return count;
	}

	public void addChangeListener(final Runnable listener) {
		if (listener != null && !changeListeners.contains(listener)) {
			changeListeners.add(listener);
		}
	}

	public void removeChangeListener(final Runnable listener) {
		changeListeners.remove(listener);
	}

	private void fireChanged() {
		for (int i = 0; i < changeListeners.size(); i++) {
			((Runnable) changeListeners.get(i)).run();
		}
	}
}
