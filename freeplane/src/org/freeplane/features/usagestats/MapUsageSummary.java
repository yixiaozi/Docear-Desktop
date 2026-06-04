package org.freeplane.features.usagestats;

import java.io.File;

public class MapUsageSummary {
	private final String mapPath;
	private final String displayName;
	private int sessionCount;
	private long totalDurationMs;
	private long effectiveDurationMs;
	private long idleDurationMs;
	private long lastEndTime;

	public MapUsageSummary(String mapPath) {
		this.mapPath = mapPath == null ? "" : mapPath;
		final File file = mapPath.isEmpty() ? null : new File(mapPath);
		this.displayName = file != null ? file.getName() : mapPath;
	}

	public void addRecord(UsageRecord record) {
		if (record == null || !UsageStatsManager.isSignificantSession(record)) {
			return;
		}
		sessionCount++;
		totalDurationMs += record.getTotalDurationMs();
		effectiveDurationMs += record.getEffectiveDurationMs();
		idleDurationMs += record.getIdleDurationMs();
		if (record.getEndTime() > lastEndTime) {
			lastEndTime = record.getEndTime();
		}
	}

	public String getMapPath() {
		return mapPath;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getSessionCount() {
		return sessionCount;
	}

	public long getTotalDurationMs() {
		return totalDurationMs;
	}

	public long getEffectiveDurationMs() {
		return effectiveDurationMs;
	}

	public long getIdleDurationMs() {
		return idleDurationMs;
	}

	public long getLastEndTime() {
		return lastEndTime;
	}

	public boolean matchesPath(String path) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		if (mapPath.equals(path)) {
			return true;
		}
		try {
			return new File(mapPath).getCanonicalPath().equals(new File(path).getCanonicalPath());
		}
		catch (Exception e) {
			return mapPath.equalsIgnoreCase(path);
		}
	}
}
