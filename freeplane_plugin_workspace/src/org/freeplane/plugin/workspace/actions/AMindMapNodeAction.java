package org.freeplane.plugin.workspace.actions;

import javax.swing.Icon;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.TextUtils;

public abstract class AMindMapNodeAction extends AFreeplaneAction {

	private static final long serialVersionUID = 1L;

	public AMindMapNodeAction(final String key) {
		super(key, TextUtils.getRawText(key + ".label"), null);
	}

	public AMindMapNodeAction(final String key, final String title, final Icon icon) {
		super(key, title, icon);
	}

	public String getTextKey() {
		return getKey() + ".label";
	}
}
