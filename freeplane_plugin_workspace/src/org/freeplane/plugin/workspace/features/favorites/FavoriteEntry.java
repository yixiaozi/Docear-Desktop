package org.freeplane.plugin.workspace.features.favorites;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class FavoriteEntry {

	private final String uri;
	private final Set<String> tags;

	public FavoriteEntry(final String uri, final Set<String> tags) {
		this.uri = uri;
		this.tags = tags == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(new LinkedHashSet<String>(tags));
	}

	public String getUri() {
		return uri;
	}

	public Set<String> getTags() {
		return tags;
	}

	public File getFile() {
		return FavoriteUriUtils.resolveToFile(uri);
	}

	public String getDisplayName() {
		final File file = getFile();
		if (file != null) {
			return file.getName();
		}
		final int slash = Math.max(uri.lastIndexOf('/'), uri.lastIndexOf('\\'));
		return slash >= 0 ? uri.substring(slash + 1) : uri;
	}

	public boolean exists() {
		final File file = getFile();
		return file != null && file.exists();
	}
}
