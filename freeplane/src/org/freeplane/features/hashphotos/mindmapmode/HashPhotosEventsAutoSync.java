package org.freeplane.features.hashphotos.mindmapmode;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.MindMapDataRootResolver;
import org.freeplane.features.map.IMapLifeCycleListener;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;

/**
 * Hash Photos CSV 自动同步：
 * <ul>
 * <li>打开 / 切换到 Hash Photos.mm 时检查最新 CSV</li>
 * <li>监听 .files/Hash Photos 目录，导出文件变化后防抖同步</li>
 * <li>导图已打开时每 5 分钟轮询一次（防止监听遗漏）</li>
 * </ul>
 */
public final class HashPhotosEventsAutoSync {

	private static HashPhotosEventsAutoSync instance;

	private final IMapLifeCycleListener mapLifeCycleListener;
	private final IMapSelectionListener mapSelectionListener;
	private Timer debounceTimer;
	private Timer pollTimer;
	private WatchService watchService;
	private Thread watchThread;
	private volatile boolean syncRunning;

	private HashPhotosEventsAutoSync() {
		mapLifeCycleListener = new IMapLifeCycleListener() {
			public void onCreate(final MapModel map) {
				requestSyncForMap(map);
			}

			public void onRemove(final MapModel map) {
			}

			public void onSavedAs(final MapModel map) {
			}

			public void onSaved(final MapModel map) {
			}
		};
		mapSelectionListener = new IMapSelectionListener() {
			public void beforeMapChange(final MapModel oldMap, final MapModel newMap) {
			}

			public void afterMapChange(final MapModel oldMap, final MapModel newMap) {
				requestSyncForMap(newMap);
			}
		};
	}

	public static synchronized void install(final MModeController modeController) {
		if (instance != null) {
			return;
		}
		instance = new HashPhotosEventsAutoSync();
		modeController.getMapController().addMapLifeCycleListener(instance.mapLifeCycleListener);
		Controller.getCurrentController().getMapViewManager().addMapSelectionListener(instance.mapSelectionListener);
		instance.startDirectoryWatcher();
		instance.startPollTimer();
		instance.scheduleInitialSync();
		LogUtils.info("Hash Photos events auto-sync installed.");
	}

	private void scheduleInitialSync() {
		Timer timer = new Timer(8000, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				syncAllOpenHashPhotosMaps();
			}
		});
		timer.setRepeats(false);
		timer.start();
	}

	private void startPollTimer() {
		pollTimer = new Timer(5 * 60 * 1000, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				syncAllOpenHashPhotosMaps();
			}
		});
		pollTimer.start();
	}

	private void startDirectoryWatcher() {
		watchThread = new Thread(new Runnable() {
			public void run() {
				runWatchLoop();
			}
		}, "HashPhotosCsvWatch");
		watchThread.setDaemon(true);
		watchThread.start();
	}

	private void runWatchLoop() {
		try {
			watchService = FileSystems.getDefault().newWatchService();
			registerKnownExportDirectories();
			while (!Thread.currentThread().isInterrupted()) {
				final WatchKey key = watchService.take();
				boolean relevant = false;
				for (WatchEvent event : key.pollEvents()) {
					if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					final Path path = (Path) event.context();
					if (path == null) {
						continue;
					}
					final String name = path.getFileName().toString().toLowerCase();
					if (name.endsWith(".csv")) {
						relevant = true;
					}
				}
				key.reset();
				if (relevant) {
					scheduleSyncAllOpenMaps();
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			LogUtils.warn("Hash Photos directory watch failed: " + e.getMessage());
		}
	}

	private void registerKnownExportDirectories() {
		try {
			final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			final Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (Object mapObj : maps.values()) {
				registerExportDirectory(HashPhotosEventsImporter.getExportDir(((MapModel) mapObj).getFile()));
			}
			registerExportDirectory(HashPhotosEventsImporter.getExportDir(resolveDefaultHashPhotosMapFile()));
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private File resolveDefaultHashPhotosMapFile() {
		final File scanRoot = MindMapDataRootResolver.getPrimaryScanRoot();
		if (scanRoot == null) {
			return null;
		}
		return new File(new File(scanRoot, "07" + "\u6761\u4e0d\u7eb9\u6709\u6761" + File.separator + "02\u65f6\u95f4\u7ba1\u7406"),
		        HashPhotosEventsImporter.MAP_FILE_NAME);
	}

	private void registerExportDirectory(final File exportDir) {
		if (exportDir == null || !exportDir.isDirectory() || watchService == null) {
			return;
		}
		try {
			exportDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
			        StandardWatchEventKinds.ENTRY_MODIFY);
		}
		catch (Exception e) {
			LogUtils.warn("Hash Photos watch register failed for " + exportDir + ": " + e.getMessage());
		}
	}

	private void requestSyncForMap(final MapModel map) {
		if (!HashPhotosEventsImporter.isHashPhotosMap(map)) {
			return;
		}
		registerExportDirectory(HashPhotosEventsImporter.getExportDir(map.getFile()));
		scheduleSyncAllOpenMaps();
	}

	private void scheduleSyncAllOpenMaps() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (debounceTimer == null) {
					debounceTimer = new Timer(1500, new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							syncAllOpenHashPhotosMaps();
						}
					});
					debounceTimer.setRepeats(false);
				}
				debounceTimer.restart();
			}
		});
	}

	private void syncAllOpenHashPhotosMaps() {
		syncAllOpenHashPhotosMaps(false);
	}

	private void syncAllOpenHashPhotosMaps(final boolean force) {
		if (syncRunning) {
			scheduleSyncAllOpenMaps();
			return;
		}
		syncRunning = true;
		try {
			final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			final Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (Object mapObj : maps.values()) {
				final MapModel map = (MapModel) mapObj;
				if (HashPhotosEventsImporter.isHashPhotosMap(map)) {
					if (force) {
						HashPhotosEventsImporter.syncMapForce(map);
					}
					else {
						HashPhotosEventsImporter.syncMap(map);
					}
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn("Hash Photos auto-sync failed: " + e.getMessage(), e);
		}
		finally {
			syncRunning = false;
		}
	}
}
