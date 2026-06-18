package org.freeplane.features.icon.mindmapmode;

/** Toggles {@code hourglass} (漏斗 marker in terst.mm). Alt+W */
public class ToggleFunnelIconAction extends ToggleBuiltinIconAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "ToggleFunnelIconAction";

	public ToggleFunnelIconAction() {
		super(KEY, "hourglass");
	}
}
