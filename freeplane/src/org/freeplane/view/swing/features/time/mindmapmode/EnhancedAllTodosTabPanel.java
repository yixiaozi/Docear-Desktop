package org.freeplane.view.swing.features.time.mindmapmode;

public class EnhancedAllTodosTabPanel extends AbstractAllItemsTabPanel {
	private static final long serialVersionUID = 1L;
	private static final String TODO_ICON_NAME = "hourglass";

	@Override
	protected String getIconName() {
		return TODO_ICON_NAME;
	}

	@Override
	protected String getRootLabel() {
		return "\u5168\u90e8\u5f85\u529e";
	}

	@Override
	protected String getStatusLabelPrefix() {
		return "\u5f85\u529e\u603b\u6570";
	}
}
