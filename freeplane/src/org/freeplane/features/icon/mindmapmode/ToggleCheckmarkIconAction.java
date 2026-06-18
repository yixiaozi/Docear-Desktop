package org.freeplane.features.icon.mindmapmode;

/** Toggles {@code button_ok} (对号 marker in terst.mm). Alt+R */
public class ToggleCheckmarkIconAction extends ToggleBuiltinIconAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "ToggleCheckmarkIconAction";

	public ToggleCheckmarkIconAction() {
		super(KEY, "button_ok");
	}
}
