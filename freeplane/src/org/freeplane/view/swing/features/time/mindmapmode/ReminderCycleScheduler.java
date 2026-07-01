package org.freeplane.view.swing.features.time.mindmapmode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Computes next occurrences for DocearReminder-compatible cycle types.
 */
final class ReminderCycleScheduler {

	private static final int MAX_ADVANCE_STEPS = 10000;
	private static final int MAX_OCCURRENCES_IN_RANGE = 366;

	private ReminderCycleScheduler() {
	}

	static long computeNextAfter(final long remindAt, final ReminderCycleAttributes.CycleConfig config,
			final long afterMillis) {
		if (config == null || !config.isRecurring()) {
			return remindAt;
		}
		long candidate = remindAt;
		int steps = 0;
		while (candidate <= afterMillis && steps++ < MAX_ADVANCE_STEPS) {
			final long next = advanceOnePeriod(candidate, config);
			if (next <= candidate) {
				break;
			}
			candidate = next;
		}
		return candidate;
	}

	static long computeNextAfterNow(final long remindAt, final ReminderCycleAttributes.CycleConfig config) {
		return computeNextAfter(remindAt, config, System.currentTimeMillis());
	}

	static List enumerateOccurrencesInRange(final long remindAt, final ReminderCycleAttributes.CycleConfig config,
			final long rangeStartInclusive, final long rangeEndExclusive) {
		final List result = new ArrayList();
		if (config == null || !config.isRecurring() || rangeEndExclusive <= rangeStartInclusive) {
			return result;
		}
		long candidate = remindAt;
		int steps = 0;
		while (candidate < rangeStartInclusive && steps++ < MAX_ADVANCE_STEPS) {
			final long next = advanceOnePeriod(candidate, config);
			if (next <= candidate) {
				return result;
			}
			candidate = next;
		}
		steps = 0;
		while (candidate < rangeEndExclusive && steps++ < MAX_OCCURRENCES_IN_RANGE) {
			if (candidate >= rangeStartInclusive) {
				result.add(Long.valueOf(candidate));
			}
			final long next = advanceOnePeriod(candidate, config);
			if (next <= candidate) {
				break;
			}
			candidate = next;
		}
		return result;
	}

	static long advanceOnePeriod(final long remindAt, final ReminderCycleAttributes.CycleConfig config) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(remindAt);
		final int interval = config.interval <= 0 ? 1 : config.interval;
		final String type = config.remindType;
		if (ReminderCycleAttributes.TYPE_HOUR.equals(type)) {
			cal.add(Calendar.HOUR_OF_DAY, interval);
		}
		else if (ReminderCycleAttributes.TYPE_DAY.equals(type)) {
			cal.add(Calendar.DAY_OF_MONTH, interval);
		}
		else if (ReminderCycleAttributes.TYPE_WEEK.equals(type)) {
			advanceWeekly(cal, config);
		}
		else if (ReminderCycleAttributes.TYPE_MONTH.equals(type)) {
			cal.add(Calendar.MONTH, interval);
		}
		else if (ReminderCycleAttributes.TYPE_YEAR.equals(type)) {
			cal.add(Calendar.YEAR, interval);
		}
		else if (ReminderCycleAttributes.TYPE_EB.equals(type)) {
			advanceBusinessDay(cal);
		}
		else {
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
		ReminderDateTimeFormatter.normalizeSeconds(cal);
		return cal.getTimeInMillis();
	}

	private static void advanceWeekly(final Calendar cal, final ReminderCycleAttributes.CycleConfig config) {
		final int interval = config.interval <= 0 ? 1 : config.interval;
		final int[] weekDays = parseWeekDays(config.weekDays);
		if (weekDays.length == 0) {
			cal.add(Calendar.WEEK_OF_YEAR, interval);
			return;
		}
		if (weekDays.length == 1) {
			cal.add(Calendar.WEEK_OF_YEAR, interval);
			return;
		}
		final int current = cal.get(Calendar.DAY_OF_WEEK);
		for (int i = 0; i < weekDays.length; i++) {
			if (weekDays[i] > current) {
				addDaysUntil(cal, weekDays[i]);
				return;
			}
		}
		cal.add(Calendar.DAY_OF_MONTH, 1);
		addDaysUntil(cal, weekDays[0]);
		if (interval > 1) {
			cal.add(Calendar.WEEK_OF_YEAR, interval - 1);
		}
	}

	private static void addDaysUntil(final Calendar cal, final int targetDayOfWeek) {
		int safety = 0;
		while (cal.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek && safety++ < 8) {
			cal.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	private static void advanceBusinessDay(final Calendar cal) {
		do {
			cal.add(Calendar.DAY_OF_MONTH, 1);
		} while (isWeekend(cal));
	}

	private static boolean isWeekend(final Calendar cal) {
		final int day = cal.get(Calendar.DAY_OF_WEEK);
		return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
	}

	private static int[] parseWeekDays(final String weekDays) {
		if (weekDays == null || weekDays.length() == 0) {
			return new int[] { Calendar.MONDAY };
		}
		final List days = new ArrayList();
		for (int i = 0; i < weekDays.length(); i++) {
			final int mapped = mapWeekDayChar(weekDays.charAt(i));
			if (mapped > 0) {
				days.add(Integer.valueOf(mapped));
			}
		}
		if (days.isEmpty()) {
			return new int[] { Calendar.MONDAY };
		}
		Collections.sort(days, new Comparator() {
			public int compare(final Object o1, final Object o2) {
				return ((Integer) o1).intValue() - ((Integer) o2).intValue();
			}
		});
		final int[] result = new int[days.size()];
		for (int i = 0; i < days.size(); i++) {
			result[i] = ((Integer) days.get(i)).intValue();
		}
		return result;
	}

	private static int mapWeekDayChar(final char day) {
		switch (day) {
			case '1':
				return Calendar.MONDAY;
			case '2':
				return Calendar.TUESDAY;
			case '3':
				return Calendar.WEDNESDAY;
			case '4':
				return Calendar.THURSDAY;
			case '5':
				return Calendar.FRIDAY;
			case '6':
				return Calendar.SATURDAY;
			case '7':
				return Calendar.SUNDAY;
			default:
				return -1;
		}
	}

	static long startOfDay(final long millis) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(millis);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	static long startOfWeek(final long millis) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(millis);
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		return startOfDay(cal.getTimeInMillis());
	}

	static long addDays(final long millis, final int days) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(millis);
		cal.add(Calendar.DAY_OF_MONTH, days);
		return startOfDay(cal.getTimeInMillis());
	}

	static long addMonths(final long millis, final int months) {
		final Calendar cal = Calendar.getInstance(Locale.CHINA);
		cal.setTimeInMillis(millis);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		cal.add(Calendar.MONTH, months);
		return startOfDay(cal.getTimeInMillis());
	}
}
