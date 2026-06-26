package org.freeplane.features.hashphotos.mindmapmode;

import org.freeplane.core.util.HtmlUtils;

/**
 * 合并 CSV 活动标题与用户在节点上追加的手写内容。
 */
public final class HashPhotosTitleMerge {

	private HashPhotosTitleMerge() {
	}

	public static String mergeTitle(final String csvTitle, final String fullText, final String syncedTitle) {
		final String csv = normalize(csvTitle);
		final String full = normalize(fullText);
		if (csv.length() == 0) {
			return full;
		}
		if (full.length() == 0) {
			return csv;
		}
		if (full.equals(csv)) {
			return full;
		}
		final String synced = normalize(syncedTitle);
		if (synced.length() > 0) {
			if (full.equals(synced)) {
				return csv;
			}
			if (full.startsWith(synced)) {
				return csv + full.substring(synced.length());
			}
			return csv;
		}
		if (full.startsWith(csv)) {
			return full;
		}
		return full;
	}

	public static boolean needsTextUpdate(final String csvTitle, final String fullText, final String syncedTitle) {
		final String merged = mergeTitle(csvTitle, fullText, syncedTitle);
		return !merged.equals(normalize(fullText));
	}

	public static String normalize(final String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}
}
