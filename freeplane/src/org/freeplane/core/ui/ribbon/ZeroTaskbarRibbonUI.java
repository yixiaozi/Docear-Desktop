package org.freeplane.core.ui.ribbon;

import org.pushingpixels.flamingo.internal.ui.ribbon.BasicRibbonUI;

/**
 * Removes the fixed top taskbar strip in Flamingo ribbon.
 * This is a low-level customization to eliminate the empty space
 * left by removed taskbar icons and application menu button.
 */
public class ZeroTaskbarRibbonUI extends BasicRibbonUI {

	@Override
	public int getTaskbarHeight() {
		return 0;
	}

	@Override
	protected boolean isUsingTitlePane() {
		return true;
	}

	@Override
	protected void installComponents() {
		super.installComponents();
		// Remove taskbar panel and application menu button from the component tree
		// so they don't participate in layout calculations
		if (taskBarPanel != null) {
			ribbon.remove(taskBarPanel);
		}
		if (applicationMenuButton != null) {
			ribbon.remove(applicationMenuButton);
		}
		// Ensure the client property is set so layout/paint logic skips taskbar area
		ribbon.putClientProperty(IS_USING_TITLE_PANE, Boolean.TRUE);
	}
}

