package org.freeplane.plugin.workspace.components.favorites;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * FlowLayout variant that wraps components onto multiple rows.
 */
public class WrapFlowLayout extends FlowLayout {

	private static final long serialVersionUID = 1L;

	public WrapFlowLayout() {
		super(FlowLayout.LEFT, 4, 4);
	}

	public Dimension preferredLayoutSize(final Container target) {
		synchronized (target.getTreeLock()) {
			final Insets insets = target.getInsets();
			int targetWidth = target.getWidth();
			if (targetWidth <= 0) {
				targetWidth = 220;
			}
			final int maxWidth = targetWidth - insets.left - insets.right;
			int x = 0;
			int y = 0;
			int rowHeight = 0;
			final int hgap = getHgap();
			final int vgap = getVgap();
			for (int i = 0; i < target.getComponentCount(); i++) {
				final Component comp = target.getComponent(i);
				if (!comp.isVisible()) {
					continue;
				}
				final Dimension size = comp.getPreferredSize();
				if (x > 0 && x + size.width > maxWidth) {
					x = 0;
					y += rowHeight + vgap;
					rowHeight = 0;
				}
				rowHeight = Math.max(rowHeight, size.height);
				x += size.width + hgap;
			}
			return new Dimension(targetWidth, y + rowHeight + insets.top + insets.bottom);
		}
	}

	public void layoutContainer(final Container target) {
		synchronized (target.getTreeLock()) {
			final Insets insets = target.getInsets();
			final int maxWidth = target.getWidth() - insets.left - insets.right;
			int x = insets.left;
			int y = insets.top;
			int rowHeight = 0;
			final int hgap = getHgap();
			final int vgap = getVgap();
			for (int i = 0; i < target.getComponentCount(); i++) {
				final Component comp = target.getComponent(i);
				if (!comp.isVisible()) {
					continue;
				}
				final Dimension size = comp.getPreferredSize();
				if (x > insets.left && x + size.width > insets.left + maxWidth) {
					x = insets.left;
					y += rowHeight + vgap;
					rowHeight = 0;
				}
				comp.setBounds(x, y, size.width, size.height);
				rowHeight = Math.max(rowHeight, size.height);
				x += size.width + hgap;
			}
		}
	}
}
