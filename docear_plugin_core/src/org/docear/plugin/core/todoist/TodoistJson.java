package org.docear.plugin.core.todoist;

final class TodoistJson {
	private TodoistJson() {
	}

	static String escape(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\\':
				sb.append("\\\\");
				break;
			case '"':
				sb.append("\\\"");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				}
				else {
					sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	static String extractStringField(String json, String fieldName) {
		if (json == null) {
			return null;
		}
		String needle = "\"" + fieldName + "\":";
		int idx = json.indexOf(needle);
		if (idx < 0) {
			return null;
		}
		idx += needle.length();
		while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) {
			idx++;
		}
		if (idx >= json.length()) {
			return null;
		}
		char c = json.charAt(idx);
		if (c == '"') {
			return readQuotedString(json, idx + 1);
		}
		if (c == 'n') {
			if (json.startsWith("null", idx)) {
				return null;
			}
		}
		return null;
	}

	static String findIdByExactName(String json, String name) {
		if (json == null || name == null) {
			return null;
		}
		String needle = "\"name\":\"" + escapeForSearch(name) + "\"";
		int idx = 0;
		while (idx >= 0) {
			idx = json.indexOf(needle, idx);
			if (idx < 0) {
				return null;
			}
			String object = extractObjectAround(json, idx);
			if (object != null) {
				String id = extractStringField(object, "id");
				if (id != null && id.length() > 0) {
					return id;
				}
			}
			idx += needle.length();
		}
		return null;
	}

	static String extractNextCursor(String json) {
		String cursor = extractStringField(json, "next_cursor");
		if (cursor == null || cursor.length() == 0 || "null".equals(cursor)) {
			return null;
		}
		return cursor;
	}

	private static String escapeForSearch(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String readQuotedString(String json, int start) {
		StringBuilder sb = new StringBuilder();
		boolean escaped = false;
		for (int idx = start; idx < json.length(); idx++) {
			char c = json.charAt(idx);
			if (escaped) {
				sb.append(c);
				escaped = false;
				continue;
			}
			if (c == '\\') {
				escaped = true;
				continue;
			}
			if (c == '"') {
				return sb.toString();
			}
			sb.append(c);
		}
		return null;
	}

	private static String extractObjectAround(String json, int positionInObject) {
		int start = positionInObject;
		while (start > 0 && json.charAt(start) != '{') {
			start--;
		}
		if (start < 0 || json.charAt(start) != '{') {
			return null;
		}
		int depth = 0;
		for (int i = start; i < json.length(); i++) {
			char c = json.charAt(i);
			if (c == '{') {
				depth++;
			}
			else if (c == '}') {
				depth--;
				if (depth == 0) {
					return json.substring(start, i + 1);
				}
			}
		}
		return null;
	}
}
