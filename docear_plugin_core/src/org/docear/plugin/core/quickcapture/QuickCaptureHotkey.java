package org.docear.plugin.core.quickcapture;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.LogUtils;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

/**
 * Registers Ctrl+Shift+Space as a system-wide hotkey on Windows (Docear must be running).
 * RegisterHotKey and the message loop run on the same dedicated thread (required by Win32).
 */
final class QuickCaptureHotkey {
	private static final int HOTKEY_ID = 0x7CE0;
	private static final int MOD_NOREPEAT = 0x4000;
	private static final int MODIFIERS = WinUser.MOD_CONTROL | WinUser.MOD_SHIFT | MOD_NOREPEAT;
	private static final int VK_SPACE = 0x20;
	private static final int PM_REMOVE = 0x0001;

	private static volatile boolean running;
	private static volatile boolean registered;
	private static volatile boolean threadStarted;
	private static volatile boolean shutdownHookAdded;

	private QuickCaptureHotkey() {
	}

	static synchronized void registerWhenReady() {
		if (!Compat.isWindowsOS() || threadStarted) {
			return;
		}
		threadStarted = true;
		addShutdownHook();
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				registerOnThread();
			}
		}, "QuickCapture-Hotkey");
		thread.setDaemon(true);
		thread.start();
	}

	private static void addShutdownHook() {
		if (shutdownHookAdded) {
			return;
		}
		shutdownHookAdded = true;
		try {
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					shutdown();
				}
			}, "QuickCapture-Hotkey-Shutdown"));
		}
		catch (Exception e) {
			LogUtils.warn("QuickCapture: could not add shutdown hook.", e);
		}
	}

	private static void registerOnThread() {
		try {
			// hWnd=null: WM_HOTKEY goes to this thread's queue (MSDN RegisterHotKey).
			if (!User32.INSTANCE.RegisterHotKey(null, HOTKEY_ID, MODIFIERS, VK_SPACE)) {
				final int err = Kernel32.INSTANCE.GetLastError();
				LogUtils.warn("QuickCapture: RegisterHotKey failed (Win32 error " + err
				        + "). Ctrl+Shift+Space may be used by another program.");
				return;
			}
			registered = true;
			running = true;
			LogUtils.info("QuickCapture: global hotkey Ctrl+Shift+Space registered.");
			final WinUser.MSG msg = new WinUser.MSG();
			while (running) {
				while (User32.INSTANCE.PeekMessage(msg, null, 0, 0, PM_REMOVE)) {
					if (msg.message == WinUser.WM_QUIT) {
						running = false;
						break;
					}
					if (msg.message == WinUser.WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID) {
						QuickCaptureService.showCaptureDialog();
					}
					User32.INSTANCE.TranslateMessage(msg);
					User32.INSTANCE.DispatchMessage(msg);
				}
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
					running = false;
					Thread.currentThread().interrupt();
				}
			}
		}
		catch (Throwable t) {
			LogUtils.warn("QuickCapture: could not register global hotkey.", t);
		}
	}

	static synchronized void shutdown() {
		running = false;
		unregister();
		threadStarted = false;
	}

	private static void unregister() {
		if (registered) {
			User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
			registered = false;
			LogUtils.info("QuickCapture: global hotkey unregistered.");
		}
	}
}
