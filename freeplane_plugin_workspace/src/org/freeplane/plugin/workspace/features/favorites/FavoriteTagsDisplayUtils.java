package org.freeplane.plugin.workspace.features.favorites;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

public final class FavoriteTagsDisplayUtils {

	private FavoriteTagsDisplayUtils() {
	}

	public static Set getFavoriteTags(final AWorkspaceTreeNode node) {
		if (!WorkspaceMindMapUtils.isWorkspaceFileNode(node)) {
			return Collections.EMPTY_SET;
		}
		final String uri = WorkspaceMindMapUtils.getWorkspaceFileUri(node);
		if (uri == null) {
			return Collections.EMPTY_SET;
		}
		final Set tags = FavoritesAndTagsStore.getInstance().getTags(uri);
		return tags == null ? Collections.EMPTY_SET : tags;
	}

	public static String formatTagsSuffixHtml(final AWorkspaceTreeNode node, final boolean selected) {
		final Set tags = getFavoriteTags(node);
		if (tags.isEmpty()) {
			return "";
		}
		final String tagsText = joinTags(tags);
		if (selected) {
			return "  [" + escapeHtml(tagsText) + "]";
		}
		return "  <font color='#666666'>[" + escapeHtml(tagsText) + "]</font>";
	}

	private static String joinTags(final Set tags) {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator it = tags.iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(tag);
		}
		return builder.toString();
	}

	private static String escapeHtml(final String text) {
		if (text == null) {
			return "";
		}
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
