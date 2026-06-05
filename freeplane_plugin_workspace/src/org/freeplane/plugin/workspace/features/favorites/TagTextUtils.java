package org.freeplane.plugin.workspace.features.favorites;

public final class TagTextUtils {

	private TagTextUtils() {
	}

	public static String normalizeSeparators(final String value) {
		if (value == null) {
			return "";
		}
		return value.replace('\uff0c', ',').replace('\u3001', ',');
	}
}
