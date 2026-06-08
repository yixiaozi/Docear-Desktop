package org.docear.plugin.core.todoist;

final class TodoistContentParser {
	final String nodeText;
	final String linkUri;

	private TodoistContentParser(String nodeText, String linkUri) {
		this.nodeText = nodeText == null ? "" : nodeText;
		this.linkUri = linkUri;
	}

	static TodoistContentParser parse(String content) {
		if (content == null) {
			return new TodoistContentParser("", null);
		}
		String trimmed = content.trim();
		if (trimmed.length() == 0) {
			return new TodoistContentParser("", null);
		}
		TodoistContentParser markdown = parseMarkdownLink(trimmed);
		if (markdown != null) {
			return markdown;
		}
		String url = findFirstUrl(trimmed);
		if (url != null) {
			String text = trimmed.replace(url, "").trim();
			if (text.endsWith("：") || text.endsWith(":")) {
				text = text.substring(0, text.length() - 1).trim();
			}
			if (text.length() == 0) {
				text = url;
			}
			return new TodoistContentParser(text, url);
		}
		return new TodoistContentParser(trimmed, null);
	}

	private static TodoistContentParser parseMarkdownLink(String content) {
		int openBracket = content.indexOf('[');
		if (openBracket < 0) {
			return null;
		}
		int closeBracket = content.indexOf(']', openBracket + 1);
		if (closeBracket <= openBracket) {
			return null;
		}
		if (closeBracket + 1 >= content.length() || content.charAt(closeBracket + 1) != '(') {
			return null;
		}
		int closeParen = content.indexOf(')', closeBracket + 2);
		if (closeParen <= closeBracket + 1) {
			return null;
		}
		String text = content.substring(openBracket + 1, closeBracket).trim();
		String url = content.substring(closeBracket + 2, closeParen).trim();
		if (text.length() == 0 || !isHttpUrl(url)) {
			return null;
		}
		return new TodoistContentParser(text, url);
	}

	private static String findFirstUrl(String text) {
		int http = text.indexOf("http://");
		int https = text.indexOf("https://");
		int start;
		if (http < 0 && https < 0) {
			return null;
		}
		if (http < 0) {
			start = https;
		}
		else if (https < 0) {
			start = http;
		}
		else {
			start = Math.min(http, https);
		}
		int end = start;
		while (end < text.length()) {
			char c = text.charAt(end);
			if (Character.isWhitespace(c) || c == ')' || c == ']' || c == '>' || c == '"') {
				break;
			}
			end++;
		}
		String url = text.substring(start, end);
		return isHttpUrl(url) ? url : null;
	}

	private static boolean isHttpUrl(String url) {
		if (url == null) {
			return false;
		}
		String lower = url.toLowerCase();
		return lower.startsWith("http://") || lower.startsWith("https://");
	}
}
