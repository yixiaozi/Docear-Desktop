package org.freeplane.plugin.workspace.components.favorites;

import java.awt.Insets;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JToggleButton;

public final class TagChipFactory {

	private static final Insets CHIP_MARGIN = new Insets(2, 8, 2, 8);

	private TagChipFactory() {
	}

	public static JToggleButton createFilterChip(final String tag, final String label, final boolean selected) {
		final JToggleButton button = new JToggleButton(label != null ? label : "");
		button.setSelected(selected);
		applyChipStyle(button);
		return button;
	}

	public static JButton createPresetChip(final String tag) {
		final JButton button = new JButton(tag != null ? tag : "");
		applyChipStyle(button);
		return button;
	}

	private static void applyChipStyle(final AbstractButton button) {
		button.setFocusPainted(false);
		button.setMargin(CHIP_MARGIN);
	}
}
