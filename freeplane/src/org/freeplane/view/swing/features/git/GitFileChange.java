package org.freeplane.view.swing.features.git;

public class GitFileChange {
	public enum Status {
		ADDED, MODIFIED, DELETED
	}

	private final String relativePath;
	private final String displayName;
	private final Status status;
	private final long sizeBytes;
	private boolean selected = true;

	public GitFileChange(final String relativePath, final Status status, final long sizeBytes) {
		this.relativePath = relativePath;
		this.displayName = GitCommand.basename(relativePath);
		this.status = status;
		this.sizeBytes = sizeBytes;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Status getStatus() {
		return status;
	}

	public long getSizeBytes() {
		return sizeBytes;
	}

	public String getFormattedSize() {
		return GitCommand.formatFileSize(sizeBytes);
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(final boolean selected) {
		this.selected = selected;
	}
}
