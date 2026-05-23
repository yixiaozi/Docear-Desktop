package org.freeplane.features.usagestats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UsageStatsManager {
    private static final String STATS_ROOT_DIR = ".docear_stats";
    private static final String DATA_DIR = "data";
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String FILE_EXTENSION = ".json";
    
    private static UsageStatsManager instance;
    private IdleDetector idleDetector;
    private UsageRecord currentRecord;
    private String currentMapPath;
    private long idleTimeAccumulator = 0;
    private long lastIdleStart = 0;
    private boolean isWindowActive = false;
    
    private UsageStatsManager() {
        idleDetector = new IdleDetector();
        idleDetector.setIdleListener(new IdleDetector.IdleListener() {
            @Override
            public void onIdleDetected(long idleTimeMs) {
                handleIdleDetected(idleTimeMs);
            }
            
            @Override
            public void onUserActivity(long idleTimeMs) {
                handleUserActivity(idleTimeMs);
            }
        });
    }
    
    public static synchronized UsageStatsManager getInstance() {
        if (instance == null) {
            instance = new UsageStatsManager();
        }
        return instance;
    }
    
    public static File getStatsDataDir() {
        try {
            File baseDir = new File("E:\\yixiaozi");
            if (!baseDir.exists()) {
                baseDir.mkdirs();
            }
            File docearStatsDir = new File(baseDir, STATS_ROOT_DIR);
            if (!docearStatsDir.exists()) {
                docearStatsDir.mkdirs();
            }
            File dataDir = new File(docearStatsDir, DATA_DIR);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            return dataDir;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void start() {
        idleDetector.start();
    }
    
    public void stop() {
        if (currentRecord != null) {
            endCurrentRecord();
        }
        idleDetector.stop();
    }
    
    public void onWindowActivated() {
        isWindowActive = true;
        if (currentMapPath != null && !currentMapPath.isEmpty()) {
            startNewRecord();
        }
    }
    
    public void onWindowDeactivated() {
        isWindowActive = false;
        if (currentRecord != null) {
            endCurrentRecord();
        }
    }
    
    public void onMapOpened(String mapPath) {
        this.currentMapPath = mapPath;
        if (isWindowActive) {
            startNewRecord();
        }
    }
    
    public void onMapClosed(String mapPath) {
        if (mapPath != null && mapPath.equals(currentMapPath)) {
            if (currentRecord != null) {
                endCurrentRecord();
            }
            this.currentMapPath = null;
        }
    }
    
    private void startNewRecord() {
        if (currentRecord != null) {
            endCurrentRecord();
        }
        
        if (currentMapPath == null || currentMapPath.isEmpty()) {
            return;
        }
        
        currentRecord = new UsageRecord();
        currentRecord.setDeviceId(DeviceIdentifier.getDeviceId());
        currentRecord.setStartTime(System.currentTimeMillis());
        currentRecord.setEventType("activated");
        currentRecord.setMapPath(currentMapPath);
        currentRecord.setFileHash(calculateDcrId(currentMapPath));
        idleTimeAccumulator = 0;
    }
    
    private void endCurrentRecord() {
        if (currentRecord == null) {
            return;
        }
        
        long now = System.currentTimeMillis();
        currentRecord.setEndTime(now);
        currentRecord.setIdleDurationMs(idleTimeAccumulator);
        currentRecord.calculateDurations();
        
        saveRecord(currentRecord);
        
        currentRecord = null;
    }
    
    private void handleIdleDetected(long idleTimeMs) {
        if (currentRecord != null) {
            lastIdleStart = System.currentTimeMillis() - idleTimeMs;
        }
    }
    
    private void handleUserActivity(long idleTimeMs) {
        if (currentRecord != null && lastIdleStart > 0) {
            idleTimeAccumulator += idleTimeMs;
            lastIdleStart = 0;
        }
    }
    
    private String calculateDcrId(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.canRead()) {
                return "";
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String firstLine = reader.readLine();
                if (firstLine != null && firstLine.contains("dcr_id=")) {
                    int startIndex = firstLine.indexOf("dcr_id=\"");
                    if (startIndex >= 0) {
                        startIndex += 8;
                        int endIndex = firstLine.indexOf("\"", startIndex);
                        if (endIndex > startIndex) {
                            return firstLine.substring(startIndex, endIndex);
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    private synchronized void saveRecord(UsageRecord record) {
        try {
            File dataDir = getStatsDataDir();
            if (dataDir == null) {
                return;
            }
            
            String deviceId = record.getDeviceId();
            File deviceDir = new File(dataDir, deviceId);
            if (!deviceDir.exists()) {
                deviceDir.mkdirs();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            String dateStr = sdf.format(new Date());
            File dateFile = new File(deviceDir, dateStr + FILE_EXTENSION);
            
            List<UsageRecord> records = loadRecordsFromFile(dateFile);
            records.add(record);
            saveRecordsToFile(dateFile, records);
        } catch (Exception e) {
            // Ignore save errors
        }
    }
    
    private List<UsageRecord> loadRecordsFromFile(File file) {
        List<UsageRecord> records = new ArrayList<UsageRecord>();
        
        if (!file.exists()) {
            return records;
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            
            String content = sb.toString();
            if (!content.trim().isEmpty()) {
                parseRecordsFromContent(content, records);
            }
        } catch (Exception e) {
            // Ignore loading errors
        }
        
        return records;
    }
    
    private void parseRecordsFromContent(String content, List<UsageRecord> records) {
        // Very simple JSON parsing to avoid adding dependencies
        int startIndex = 0;
        while (true) {
            int objStart = content.indexOf('{', startIndex);
            if (objStart < 0) {
                break;
            }
            
            int braceCount = 1;
            int objEnd = objStart + 1;
            while (objEnd < content.length() && braceCount > 0) {
                char c = content.charAt(objEnd);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                }
                objEnd++;
            }
            
            if (braceCount == 0) {
                UsageRecord record = parseRecord(content.substring(objStart, objEnd));
                if (record != null) {
                    records.add(record);
                }
            }
            
            startIndex = objEnd;
        }
    }
    
    private UsageRecord parseRecord(String json) {
        UsageRecord record = new UsageRecord();
        
        String[] pairs = json.replace('{', ' ').replace('}', ' ').trim().split(",\"");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.startsWith("\"")) {
                pair = pair.substring(1);
            }
            int colonIndex = pair.indexOf("\":");
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 2).trim();
                
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                
                try {
                    if ("recordId".equals(key)) {
                        // Skip, since we generate new
                    } else if ("fileHash".equals(key)) {
                        record.setFileHash(value);
                    } else if ("deviceId".equals(key)) {
                        record.setDeviceId(value);
                    } else if ("startTime".equals(key)) {
                        record.setStartTime(Long.parseLong(value));
                    } else if ("endTime".equals(key)) {
                        record.setEndTime(Long.parseLong(value));
                    } else if ("totalDurationMs".equals(key)) {
                        record.setTotalDurationMs(Long.parseLong(value));
                    } else if ("idleDurationMs".equals(key)) {
                        record.setIdleDurationMs(Long.parseLong(value));
                    } else if ("effectiveDurationMs".equals(key)) {
                        record.setEffectiveDurationMs(Long.parseLong(value));
                    } else if ("eventType".equals(key)) {
                        record.setEventType(value);
                    } else if ("mapPath".equals(key)) {
                        record.setMapPath(value);
                    } else if ("appVersion".equals(key)) {
                        record.setAppVersion(value);
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        return record;
    }
    
    private void saveRecordsToFile(File file, List<UsageRecord> records) {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
             BufferedWriter bw = new BufferedWriter(writer)) {
            
            bw.write("[");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) {
                    bw.write(",");
                }
                writeRecord(bw, records.get(i));
            }
            bw.write("]");
        } catch (IOException e) {
            // Ignore save errors
        }
    }
    
    private void writeRecord(BufferedWriter writer, UsageRecord record) throws IOException {
        writer.write("{");
        writeStringField(writer, "recordId", record.getRecordId(), true);
        writeStringField(writer, "dcrId", record.getFileHash(), true);
        writeStringField(writer, "deviceId", record.getDeviceId(), true);
        writeLongField(writer, "startTime", record.getStartTime(), true);
        writeLongField(writer, "endTime", record.getEndTime(), true);
        writeLongField(writer, "totalDurationMs", record.getTotalDurationMs(), true);
        writeLongField(writer, "idleDurationMs", record.getIdleDurationMs(), true);
        writeLongField(writer, "effectiveDurationMs", record.getEffectiveDurationMs(), true);
        writeStringField(writer, "eventType", record.getEventType(), true);
        writeStringField(writer, "mapPath", record.getMapPath(), true);
        writeStringField(writer, "appVersion", record.getAppVersion(), false);
        writer.write("}");
    }
    
    private void writeStringField(BufferedWriter writer, String name, String value, boolean addComma) throws IOException {
        if (value == null) {
            value = "";
        }
        writer.write("\"" + escapeJsonString(name) + "\":\"" + escapeJsonString(value) + "\"");
        if (addComma) {
            writer.write(",");
        }
    }
    
    private void writeLongField(BufferedWriter writer, String name, long value, boolean addComma) throws IOException {
        writer.write("\"" + escapeJsonString(name) + "\":" + value);
        if (addComma) {
            writer.write(",");
        }
    }
    
    private String escapeJsonString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}
