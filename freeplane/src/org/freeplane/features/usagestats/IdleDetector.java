package org.freeplane.features.usagestats;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IdleDetector {
    public static final int DEFAULT_IDLE_THRESHOLD_MS = 2 * 60 * 1000; // 2 minutes
    private static final int CHECK_INTERVAL_MS = 1000; // 1 second
    
    private long idleThresholdMs = DEFAULT_IDLE_THRESHOLD_MS;
    private volatile long lastActivityTime;
    private volatile boolean isIdle;
    private volatile boolean isRunning;
    private ScheduledExecutorService scheduler;
    private AWTEventListener awtEventListener;
    private IdleListener idleListener;
    
    public interface IdleListener {
        void onIdleDetected(long idleTimeMs);
        void onUserActivity(long idleTimeMs);
    }
    
    public IdleDetector() {
        this.lastActivityTime = System.currentTimeMillis();
        this.isIdle = false;
    }
    
    public void setIdleListener(IdleListener listener) {
        this.idleListener = listener;
    }
    
    public void setIdleThresholdMs(long thresholdMs) {
        this.idleThresholdMs = thresholdMs;
    }
    
    public synchronized void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        
        startAWTEventListener();
        startScheduler();
    }
    
    public synchronized void stop() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        
        stopScheduler();
        stopAWTEventListener();
    }
    
    private void startAWTEventListener() {
        awtEventListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent event) {
                if (isUserActivity(event)) {
                    onUserActivityDetected();
                }
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(awtEventListener, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }
    
    private void stopAWTEventListener() {
        if (awtEventListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(awtEventListener);
            awtEventListener = null;
        }
    }
    
    private void startScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                checkIdleStatus();
            }
        }, CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    private void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
    
    private boolean isUserActivity(AWTEvent event) {
        if (event instanceof KeyEvent) {
            return event.getID() == KeyEvent.KEY_PRESSED;
        }
        if (event instanceof MouseEvent) {
            int id = event.getID();
            return id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED || id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_MOVED;
        }
        return false;
    }
    
    private void onUserActivityDetected() {
        long currentTime = System.currentTimeMillis();
        long previousActivityTime = lastActivityTime;
        lastActivityTime = currentTime;
        
        if (isIdle) {
            isIdle = false;
            long idleTime = currentTime - previousActivityTime;
            notifyUserActivity(idleTime);
        }
    }
    
    private void checkIdleStatus() {
        long currentTime = System.currentTimeMillis();
        long idleTime = currentTime - lastActivityTime;
        
        if (!isIdle && idleTime >= idleThresholdMs) {
            isIdle = true;
            notifyIdleDetected(idleTime);
        }
    }
    
    private void notifyIdleDetected(long idleTime) {
        if (idleListener != null) {
            try {
                idleListener.onIdleDetected(idleTime);
            } catch (Exception e) {
                // Ignore listener exceptions
            }
        }
    }
    
    private void notifyUserActivity(long idleTime) {
        if (idleListener != null) {
            try {
                idleListener.onUserActivity(idleTime);
            } catch (Exception e) {
                // Ignore listener exceptions
            }
        }
    }
    
    public long getIdleTimeMs() {
        return System.currentTimeMillis() - lastActivityTime;
    }
    
    public boolean isIdle() {
        return isIdle;
    }
    
    public void markActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }
}
