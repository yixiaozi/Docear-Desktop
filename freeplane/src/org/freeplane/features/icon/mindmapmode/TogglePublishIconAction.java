package org.freeplane.features.icon.mindmapmode;

/** Toggles {@code internet} (发布 marker in terst.mm). Alt+E */
public class TogglePublishIconAction extends ToggleBuiltinIconAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "TogglePublishIconAction";

	public TogglePublishIconAction() {
		super(KEY, "internet");
	}
}
