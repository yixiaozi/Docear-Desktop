package org.freeplane.view.swing.features.time.mindmapmode;

public class EnhancedAllPublishTabPanel extends AbstractAllItemsTabPanel {
	private static final long serialVersionUID = 1L;
	private static final String PUBLISH_ICON_NAME = "internet";

	@Override
	protected String getIconName() {
		return PUBLISH_ICON_NAME;
	}

	@Override
	protected String getRootLabel() {
		return "\u5168\u90e8\u53d1\u5e03";
	}

	@Override
	protected String getStatusLabelPrefix() {
		return "\u53d1\u5e03\u603b\u6570";
	}
}