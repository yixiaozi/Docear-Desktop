package org.docear.plugin.core.quickcapture;

import java.awt.event.ActionEvent;

import org.freeplane.core.ui.AFreeplaneAction;

public class QuickCaptureAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "QuickCaptureAction";

	public QuickCaptureAction() {
		super(KEY);
	}

	public void actionPerformed(final ActionEvent e) {
		QuickCaptureService.showCaptureDialog();
	}
}
