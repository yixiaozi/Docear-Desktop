package org.freeplane.plugin.workspace.features.nodepins;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class NodePinEntry {

	private final String key;
	private final Set tags;
	private final boolean pinned;
	private String nodeLabel;
	private boolean exists = true;

	public NodePinEntry(final String key, final Set tags, final boolean pinned, final String nodeLabel) {
		this.key = key;
		this.tags = tags == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(new LinkedHashSet(tags));
		this.pinned = pinned;
		this.nodeLabel = nodeLabel == null ? "" : nodeLabel;
	}

	public String getKey() {
		return key;
	}

	public Set getTags() {
		return tags;
	}

	public boolean isPinned() {
		return pinned;
	}

	public String getNodeLabel() {
		return nodeLabel;
	}

	public void setNodeLabel(final String nodeLabel) {
		this.nodeLabel = nodeLabel == null ? "" : nodeLabel;
	}

	public boolean exists() {
		return exists;
	}

	public void setExists(final boolean exists) {
		this.exists = exists;
	}

	public File getMapFile() {
		return NodePinKeyUtils.resolveMapFile(key);
	}

	public String getNodeId() {
		return NodePinKeyUtils.parseNodeId(key);
	}

	public String getMapDisplayName() {
		final File file = getMapFile();
		if (file != null) {
			return file.getName();
		}
		final String mapUri = NodePinKeyUtils.parseMapUri(key);
		if (mapUri == null) {
			return "";
		}
		final int slash = Math.max(mapUri.lastIndexOf('/'), mapUri.lastIndexOf('\\'));
		return slash >= 0 ? mapUri.substring(slash + 1) : mapUri;
	}

	public String getListNodeLabel() {
		if (nodeLabel != null && nodeLabel.length() > 0) {
			return nodeLabel;
		}
		return getNodeId() == null ? "" : getNodeId();
	}
}
