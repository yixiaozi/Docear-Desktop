package org.freeplane.plugin.workspace.features.nodepins;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.plugin.workspace.features.favorites.TagTextUtils;

public final class NodeDetailsTagUtils {

	public static final String PIN_TAG = "\u9489\u9009";
	public static final String TAG_ARCHIVED = "\u5df2\u5f52\u6863";
	public static final String[] PRESET_TAGS = { "\u5f85\u529e", "\u53c2\u8003", TAG_ARCHIVED, "\u9ad8\u4f18\u5148\u7ea7" };

	private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([^#\\s<]+)");
	private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("<p>(.*?)</p>", Pattern.CASE_INSENSITIVE
			| Pattern.DOTALL);
	private static final Pattern NUMERIC_ENTITY_FRAGMENT = Pattern.compile("^\\d+;?$");

	private NodeDetailsTagUtils() {
	}

	public static Set parseAllTags(final String detailsHtml) {
		final LinkedHashSet tags = new LinkedHashSet();
		if (detailsHtml == null || detailsHtml.trim().length() == 0) {
			return tags;
		}
		final String plain = toPlainDetailsText(detailsHtml);
		final String[] lines = plain.split("\n");
		for (int i = 0; i < lines.length; i++) {
			final String line = lines[i].trim();
			if (line.length() == 0) {
				continue;
			}
			if (isTagOnlyLine(line)) {
				collectHashtags(line, tags);
			}
		}
		return tags;
	}

	public static Set parseUserTags(final String detailsHtml) {
		final LinkedHashSet tags = new LinkedHashSet(parseAllTags(detailsHtml));
		tags.remove(PIN_TAG);
		return tags;
	}

	public static boolean isPinnedInDetails(final String detailsHtml) {
		return parseAllTags(detailsHtml).contains(PIN_TAG);
	}

	public static boolean hasAnyManagedTag(final String detailsHtml) {
		return !parseAllTags(detailsHtml).isEmpty();
	}

	public static String extractUserContentHtml(final String detailsHtml) {
		if (detailsHtml == null || detailsHtml.trim().length() == 0) {
			return "";
		}
		String body = stripHtmlWrapper(detailsHtml);
		body = removeTagOnlyParagraphs(body);
		return body.trim();
	}

	public static String buildDetailsHtml(final String userContentHtml, final Set tags) {
		final StringBuilder body = new StringBuilder();
		final String userPart = userContentHtml == null ? "" : userContentHtml.trim();
		if (userPart.length() > 0) {
			if (userPart.toLowerCase().indexOf("<html") >= 0) {
				body.append(extractUserContentHtml(userPart));
			}
			else if (userPart.toLowerCase().indexOf("<p") >= 0) {
				body.append(userPart);
			}
			else {
				body.append("<p>").append(escapeHtml(userPart)).append("</p>");
			}
		}
		if (tags != null && !tags.isEmpty()) {
			body.append("<p>").append(formatHashtagLine(tags)).append("</p>");
		}
		if (body.length() == 0) {
			return null;
		}
		return "<html><head></head><body>" + body.toString() + "</body></html>";
	}

	public static String formatHashtagLine(final Set tags) {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator it = tags.iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			if (tag == null || tag.trim().length() == 0 || !isValidTagName(tag)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(' ');
			}
			builder.append('#').append(tag.trim());
		}
		return builder.toString();
	}

	public static Set parseTagNamesFromText(final String value) {
		final LinkedHashSet tags = new LinkedHashSet();
		if (value == null) {
			return tags;
		}
		final String[] parts = TagTextUtils.normalizeSeparators(value).split(",");
		for (int i = 0; i < parts.length; i++) {
			String trimmed = parts[i].trim();
			if (trimmed.startsWith("#")) {
				trimmed = trimmed.substring(1).trim();
			}
			trimmed = normalizeTagName(trimmed);
			if (isValidTagName(trimmed)) {
				tags.add(trimmed);
			}
		}
		return tags;
	}

	public static String joinTagNames(final Set tags) {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator it = tags.iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			if (PIN_TAG.equals(tag) || !isValidTagName(tag)) {
				continue;
			}
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(tag);
		}
		return builder.toString();
	}

	public static boolean isValidTagName(final String tag) {
		if (tag == null) {
			return false;
		}
		final String trimmed = tag.trim();
		if (trimmed.length() == 0) {
			return false;
		}
		if (NUMERIC_ENTITY_FRAGMENT.matcher(trimmed).matches()) {
			return false;
		}
		if (trimmed.indexOf('&') >= 0) {
			return false;
		}
		if (trimmed.indexOf(';') >= 0 && trimmed.indexOf(';') == trimmed.length() - 1
				&& trimmed.substring(0, trimmed.length() - 1).matches("\\d+")) {
			return false;
		}
		return true;
	}

	private static boolean isTagOnlyLine(final String line) {
		if (line.indexOf('#') < 0) {
			return false;
		}
		final String decoded = HtmlUtils.unescapeHTMLUnicodeEntity(line);
		final String[] tokens = decoded.split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			final String token = tokens[i].trim();
			if (token.length() == 0) {
				continue;
			}
			if (!token.startsWith("#") || token.length() <= 1) {
				return false;
			}
		}
		return true;
	}

	private static void collectHashtags(final String text, final Set tags) {
		final String decoded = HtmlUtils.unescapeHTMLUnicodeEntity(text);
		final Matcher matcher = HASHTAG_PATTERN.matcher(decoded);
		while (matcher.find()) {
			final String tag = normalizeTagName(matcher.group(1));
			if (isValidTagName(tag)) {
				tags.add(tag);
			}
		}
	}

	static String normalizeTagName(final String raw) {
		if (raw == null) {
			return "";
		}
		return HtmlUtils.unescapeHTMLUnicodeEntity(raw.trim());
	}

	private static String toPlainDetailsText(final String detailsHtml) {
		String text = stripHtmlWrapper(detailsHtml);
		text = text.replaceAll("(?i)</p>\\s*", "\n");
		text = text.replaceAll("(?i)<p[^>]*>\\s*", "");
		text = text.replaceAll("<[^>]+>", "");
		text = HtmlUtils.unescapeHTMLUnicodeEntity(text);
		return text.replace('\r', '\n');
	}

	private static String stripHtmlWrapper(final String detailsHtml) {
		String result = detailsHtml;
		result = result.replaceAll("(?i)</body>\\s*</html>\\s*$", "");
		result = result.replaceAll("(?i)^\\s*<html>\\s*<head>\\s*</head>\\s*<body>\\s*", "");
		return result;
	}

	private static String removeTagOnlyParagraphs(final String html) {
		final Matcher matcher = PARAGRAPH_PATTERN.matcher(html);
		final StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			final String paragraphHtml = matcher.group(1);
			final String plain = toPlainDetailsText("<p>" + paragraphHtml + "</p>").trim();
			if (!isTagOnlyLine(plain)) {
				matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
			}
			else {
				matcher.appendReplacement(buffer, "");
			}
		}
		matcher.appendTail(buffer);
		return buffer.toString();
	}

	private static String escapeHtml(final String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
