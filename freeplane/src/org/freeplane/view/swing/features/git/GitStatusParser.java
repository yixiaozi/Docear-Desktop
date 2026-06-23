package org.freeplane.view.swing.features.git;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

final class GitStatusParser {
	static final class Entry {
		final String status;
		final String path;

		Entry(final String status, final String path) {
			this.status = status;
			this.path = path;
		}
	}

	private GitStatusParser() {
	}

	static List<Entry> parsePorcelain(final File repository) {
		final GitCommand.Result result = GitCommand.run(repository, "status", "--porcelain", "-z");
		if (result.rawText == null || result.rawText.length() == 0) {
			return parseLinePorcelain(result.output);
		}
		return parsePorcelainZ(result.rawText);
	}

	private static List<Entry> parseLinePorcelain(final List<String> lines) {
		final List<Entry> entries = new ArrayList<Entry>();
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i);
			if (line.length() < 4) {
				continue;
			}
			String path = line.substring(3);
			if (path.startsWith("\"") && path.endsWith("\"")) {
				path = path.substring(1, path.length() - 1);
			}
			entries.add(new Entry(line.substring(0, 2), path));
		}
		return entries;
	}

	private static List<Entry> parsePorcelainZ(final String text) {
		final List<Entry> entries = new ArrayList<Entry>();
		int index = 0;
		while (index + 2 < text.length()) {
			final String status = text.substring(index, index + 2);
			index += 2;
			if (index < text.length() && text.charAt(index) == ' ') {
				index++;
			}
			final int pathEnd = text.indexOf('\0', index);
			if (pathEnd < 0) {
				break;
			}
			final String path = text.substring(index, pathEnd);
			index = pathEnd + 1;
			entries.add(new Entry(status, path));
			if (isRenameOrCopy(status)) {
				final int newPathEnd = text.indexOf('\0', index);
				if (newPathEnd < 0) {
					break;
				}
				index = newPathEnd + 1;
			}
		}
		return entries;
	}

	private static boolean isRenameOrCopy(final String status) {
		return status.indexOf('R') >= 0 || status.indexOf('C') >= 0;
	}
}
