package org.freeplane.features.usagestats;

import java.io.Serializable;
import java.util.UUID;

public class UsageRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String recordId;
    private String fileHash;
    private String deviceId;
    private long startTime;
    private long endTime;
    private long totalDurationMs;
    private long idleDurationMs;
    private long effectiveDurationMs;
    private String eventType;
    private String mapPath;
    private String appVersion;
    
    public UsageRecord() {
        this.recordId = UUID.randomUUID().toString();
    }
    
    public UsageRecord(String recordId) {
        this.recordId = recordId;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getTotalDurationMs() {
        return totalDurationMs;
    }

    public void setTotalDurationMs(long totalDurationMs) {
        this.totalDurationMs = totalDurationMs;
    }

    public long getIdleDurationMs() {
        return idleDurationMs;
    }

    public void setIdleDurationMs(long idleDurationMs) {
        this.idleDurationMs = idleDurationMs;
    }

    public long getEffectiveDurationMs() {
        return effectiveDurationMs;
    }

    public void setEffectiveDurationMs(long effectiveDurationMs) {
        this.effectiveDurationMs = effectiveDurationMs;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMapPath() {
        return mapPath;
    }

    public void setMapPath(String mapPath) {
        this.mapPath = mapPath;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void calculateDurations() {
        if (startTime > 0 && endTime > 0) {
            totalDurationMs = endTime - startTime;
            effectiveDurationMs = totalDurationMs - idleDurationMs;
        }
    }
}
