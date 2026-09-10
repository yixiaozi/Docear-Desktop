package org.freeplane.view.swing.features.pomodoro;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * One completed focus segment (start → end). Pause spans are stored explicitly
 * when available; otherwise pause duration falls back to wall − focus.
 */
public final class PomodoroSessionRecord {
	public final long startMs;
	public final long endMs;
	public final long focusMs;
	/** Completed pause intervals inside this session; never null. */
	public final List pauseIntervals;
	/** Free-text remark for this session (feelings / misc notes); never null. */
	public final String note;

	public PomodoroSessionRecord(final long startMs, final long endMs, final long focusMs) {
		this(startMs, endMs, focusMs, Collections.EMPTY_LIST, null);
	}

	public PomodoroSessionRecord(final long startMs, final long endMs, final long focusMs,
			final List pauseIntervals) {
		this(startMs, endMs, focusMs, pauseIntervals, null);
	}

	public PomodoroSessionRecord(final long startMs, final long endMs, final long focusMs,
			final List pauseIntervals, final String note) {
		this.startMs = startMs;
		this.endMs = endMs;
		this.focusMs = Math.max(0L, focusMs);
		this.pauseIntervals = PomodoroPauseInterval.copyOf(pauseIntervals);
		this.note = normalizeNote(note);
	}

	public boolean hasNote() {
		return note.length() > 0;
	}

	/** Trim and drop control chars; keep inner spacing. Never returns null. */
	static String normalizeNote(final String raw) {
		if (raw == null) {
			return "";
		}
		return raw.trim();
	}

	public long pauseMs() {
		if (!pauseIntervals.isEmpty()) {
			return PomodoroPauseInterval.sumMs(pauseIntervals);
		}
		final long span = Math.max(0L, endMs - startMs);
		return Math.max(0L, span - focusMs);
	}

	/**
	 * Compact wire format: {@code start-end:focus} then optional
	 * {@code @p1s-p1e,p2s-p2e} pauses and optional {@code #urlEncodedNote}.
	 * The note is URL-encoded so it can safely hold {@code ; - : @ , #} and newlines.
	 */
	String encode() {
		final StringBuilder sb = new StringBuilder();
		sb.append(startMs).append('-').append(endMs).append(':').append(focusMs);
		if (!pauseIntervals.isEmpty()) {
			sb.append('@').append(PomodoroPauseInterval.encodeList(pauseIntervals));
		}
		if (note.length() > 0) {
			sb.append('#').append(encodeNote(note));
		}
		return sb.toString();
	}

	static PomodoroSessionRecord decode(final String token) {
		if (token == null || token.length() == 0) {
			return null;
		}
		try {
			// Note is always the last field, split it off first.
			final int hash = token.indexOf('#');
			final String noteRaw = hash >= 0 ? decodeNote(token.substring(hash + 1)) : "";
			final String body = hash >= 0 ? token.substring(0, hash) : token;
			final int at = body.indexOf('@');
			final String head = at >= 0 ? body.substring(0, at) : body;
			final String pauseRaw = at >= 0 ? body.substring(at + 1) : "";
			final int dash = head.indexOf('-');
			final int colon = head.indexOf(':');
			if (dash <= 0 || colon <= dash) {
				return null;
			}
			final long start = Long.parseLong(head.substring(0, dash));
			final long end = Long.parseLong(head.substring(dash + 1, colon));
			final long focus = Long.parseLong(head.substring(colon + 1));
			if (start <= 0 || end < start || focus < 0) {
				return null;
			}
			return new PomodoroSessionRecord(start, end, focus, PomodoroPauseInterval.decodeList(pauseRaw), noteRaw);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	private static String encodeNote(final String raw) {
		try {
			return java.net.URLEncoder.encode(raw, "UTF-8");
		}
		catch (java.io.UnsupportedEncodingException e) {
			return "";
		}
	}

	private static String decodeNote(final String raw) {
		if (raw == null || raw.length() == 0) {
			return "";
		}
		try {
			return java.net.URLDecoder.decode(raw, "UTF-8");
		}
		catch (Exception e) {
			return "";
		}
	}

	public String toDisplayLine() {
		final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
		fmt.setTimeZone(TimeZone.getDefault());
		final StringBuilder sb = new StringBuilder();
		sb.append(fmt.format(new Date(startMs))).append(" -> ").append(fmt.format(new Date(endMs)));
		sb.append("  ").append(PomodoroFormatter.formatDuration(focusMs));
		final long pause = pauseMs();
		if (pause > 0) {
			// Pause details always last so the focus duration column stays readable.
			sb.append("  / 暂停");
			final String ranges = PomodoroPauseInterval.formatRanges(pauseIntervals);
			if (ranges.length() > 0) {
				sb.append(' ').append(ranges);
			}
			sb.append('（').append(PomodoroFormatter.formatDuration(pause)).append('）');
		}
		if (note.length() > 0) {
			// Notes are single-lined here so the history preview stays one row per session.
			sb.append("  📝 ").append(note.replace('\n', ' ').replace('\r', ' '));
		}
		return sb.toString();
	}
}
