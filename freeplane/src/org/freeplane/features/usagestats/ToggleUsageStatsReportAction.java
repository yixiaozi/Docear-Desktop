package org.freeplane.features.usagestats;

import java.awt.event.ActionEvent;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.SelectableAction;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;

@SelectableAction(checkOnPopup = true)
public class ToggleUsageStatsReportAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "ToggleUsageStatsReportAction";
	public static final String VISIBLE_PROPERTY = "usageStatsReportVisible";

	public ToggleUsageStatsReportAction() {
		super(KEY);
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final ResourceController resourceController = ResourceController.getResourceController();
		final boolean visible = !resourceController.getBooleanProperty(VISIBLE_PROPERTY);
		resourceController.setProperty(VISIBLE_PROPERTY, visible);
		final UsageStatsReportService service = getService();
		if (service != null) {
			service.setReportVisible(visible);
		}
		setSelected(visible);
	}

	@Override
	public void setSelected() {
		setSelected(ResourceController.getResourceController().getBooleanProperty(VISIBLE_PROPERTY));
	}

	private UsageStatsReportService getService() {
		final Controller controller = Controller.getCurrentController();
		if (!(controller.getModeController() instanceof MModeController)) {
			return null;
		}
		return controller.getModeController().getExtension(UsageStatsReportService.class);
	}
}
