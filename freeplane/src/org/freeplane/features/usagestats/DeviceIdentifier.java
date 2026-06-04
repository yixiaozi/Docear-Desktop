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
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.UUID;

public class DeviceIdentifier {
    private static final String DEVICE_ID_FILE = ".docear_stats/.device.id";
    private static String cachedDeviceId = null;
    
    public static synchronized String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }
        
        File dataDir = UsageStatsManager.getStatsDataDir();
        if (dataDir != null) {
            File idFile = new File(dataDir, DEVICE_ID_FILE);
            if (idFile.exists()) {
                cachedDeviceId = readDeviceIdFromFile(idFile);
                if (cachedDeviceId != null) {
                    return cachedDeviceId;
                }
            }
            
            cachedDeviceId = generateDeviceId();
            if (cachedDeviceId != null) {
                saveDeviceIdToFile(idFile, cachedDeviceId);
            }
        }
        
        if (cachedDeviceId == null) {
            cachedDeviceId = UUID.randomUUID().toString();
        }
        
        return cachedDeviceId;
    }
    
    private static String readDeviceIdFromFile(File file) {
        Reader reader = null;
        BufferedReader br = null;
        try {
            reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            br = new BufferedReader(reader);
            String id = br.readLine();
            if (id != null && !id.trim().isEmpty()) {
                return id.trim();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            if (br != null) {
                try { br.close(); } catch (IOException e) { }
            }
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { }
            }
        }
        return null;
    }
    
    private static void saveDeviceIdToFile(File file, String deviceId) {
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        Writer writer = null;
        BufferedWriter bw = null;
        try {
            writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            bw = new BufferedWriter(writer);
            bw.write(deviceId);
        } catch (IOException e) {
            // Ignore
        } finally {
            if (bw != null) {
                try { bw.close(); } catch (IOException e) { }
            }
            if (writer != null) {
                try { writer.close(); } catch (IOException e) { }
            }
        }
    }
    
    private static String generateDeviceId() {
        String macAddress = getMacAddress();
        if (macAddress != null && !macAddress.isEmpty()) {
            return hashString(macAddress);
        }
        return UUID.randomUUID().toString();
    }
    
    private static String getMacAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                byte[] mac = iface.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }
    
    private static String hashString(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString();
        }
    }
    
    public static String getDeviceName() {
        String computerName = System.getenv("COMPUTERNAME");
        if (computerName == null) {
            computerName = System.getenv("HOSTNAME");
        }
        if (computerName == null) {
            computerName = "Unknown";
        }
        return computerName;
    }
    
    public static String getPlatform() {
        return System.getProperty("os.name", "Unknown");
    }
}
