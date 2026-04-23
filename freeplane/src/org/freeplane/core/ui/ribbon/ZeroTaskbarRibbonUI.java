package org.freeplane.core.ui.ribbon;

import org.pushingpixels.flamingo.internal.ui.ribbon.BasicRibbonUI;

/**
 * Removes the fixed top taskbar strip in Flamingo ribbon.
 */
public class ZeroTaskbarRibbonUI extends BasicRibbonUI {
	@Override
	public int getTaskbarHeight() {
		return 0;
	}
}

