package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

/**
 * Persistent reminder cycle settings on a mind map node (DocearReminder-compatible).
 */
final class ReminderCycleExtension implements IExtension {
	private String remindType = ReminderCycleAttributes.TYPE_ONETIME;
	private int rHour = 1;
	private int rDays = 1;
	private int rWeek = 1;
	private int rMonth = 1;
	private int rYear = 1;
	private String rWeeks = "1";
	private int ebString = 0;

	static ReminderCycleExtension getExtension(final NodeModel node) {
		return node == null ? null : (ReminderCycleExtension) node.getExtension(ReminderCycleExtension.class);
	}

	static ReminderCycleExtension getOrCreateExtension(final NodeModel node) {
		ReminderCycleExtension extension = getExtension(node);
		if (extension == null) {
			extension = new ReminderCycleExtension();
			node.addExtension(extension);
		}
		return extension;
	}

	ReminderCycleAttributes.CycleConfig toConfig() {
		final int interval;
		if (ReminderCycleAttributes.TYPE_HOUR.equals(remindType)) {
			interval = rHour;
		}
		else if (ReminderCycleAttributes.TYPE_DAY.equals(remindType)) {
			interval = rDays;
		}
		else if (ReminderCycleAttributes.TYPE_WEEK.equals(remindType)) {
			interval = rWeek;
		}
		else if (ReminderCycleAttributes.TYPE_MONTH.equals(remindType)) {
			interval = rMonth;
		}
		else if (ReminderCycleAttributes.TYPE_YEAR.equals(remindType)) {
			interval = rYear;
		}
		else {
			interval = 1;
		}
		return new ReminderCycleAttributes.CycleConfig(remindType, interval, rWeeks, ebString);
	}

	void applyConfig(final ReminderCycleAttributes.CycleConfig config) {
		if (config == null || config.remindType == null || config.remindType.length() == 0
				|| ReminderCycleAttributes.TYPE_ONETIME.equalsIgnoreCase(config.remindType)) {
			remindType = ReminderCycleAttributes.TYPE_ONETIME;
			return;
		}
		final int interval = config.interval <= 0 ? 1 : config.interval;
		remindType = config.remindType;
		rHour = interval;
		rDays = interval;
		rWeek = interval;
		rMonth = interval;
		rYear = interval;
		rWeeks = config.weekDays == null || config.weekDays.length() == 0 ? "1" : config.weekDays;
		ebString = config.ebString;
	}

	void clear() {
		remindType = ReminderCycleAttributes.TYPE_ONETIME;
		rHour = 1;
		rDays = 1;
		rWeek = 1;
		rMonth = 1;
		rYear = 1;
		rWeeks = "1";
		ebString = 0;
	}

	String getRemindType() {
		return remindType;
	}

	void setRemindType(final String remindType) {
		this.remindType = remindType == null ? ReminderCycleAttributes.TYPE_ONETIME : remindType;
	}

	int getRHour() {
		return rHour;
	}

	void setRHour(final int rHour) {
		this.rHour = rHour <= 0 ? 1 : rHour;
	}

	int getRDays() {
		return rDays;
	}

	void setRDays(final int rDays) {
		this.rDays = rDays <= 0 ? 1 : rDays;
	}

	int getRWeek() {
		return rWeek;
	}

	void setRWeek(final int rWeek) {
		this.rWeek = rWeek <= 0 ? 1 : rWeek;
	}

	int getRMonth() {
		return rMonth;
	}

	void setRMonth(final int rMonth) {
		this.rMonth = rMonth <= 0 ? 1 : rMonth;
	}

	int getRYear() {
		return rYear;
	}

	void setRYear(final int rYear) {
		this.rYear = rYear <= 0 ? 1 : rYear;
	}

	String getRWeeks() {
		return rWeeks;
	}

	void setRWeeks(final String rWeeks) {
		this.rWeeks = rWeeks == null || rWeeks.length() == 0 ? "1" : rWeeks;
	}

	int getEbString() {
		return ebString;
	}

	void setEbString(final int ebString) {
		this.ebString = ebString;
	}
}
