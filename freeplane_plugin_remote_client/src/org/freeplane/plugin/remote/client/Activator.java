package org.freeplane.plugin.remote.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.docear.messages.models.MapIdentifier;
import org.freeplane.core.ui.IMenuContributor;
import org.freeplane.core.ui.MenuBuilder;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private ClientController clientController;
	
	@Override
	public void start(BundleContext context) {
		registerToFreeplaneStart(context);
	}

	private void registerToFreeplaneStart(final BundleContext context) {
		final Hashtable<String, String[]> props = new Hashtable<String, String[]>();

		props.put("mode", new String[] { MModeController.MODENAME });
		context.registerService(IModeControllerExtensionProvider.class.getName(), new IModeControllerExtensionProvider() {
			public void installExtension(ModeController modeController) {
				clientController =  new ClientController();
				createDebugButton();
			}
		}, props);
	}

	private void createDebugButton() {
		final Controller controller = Controller.getCurrentController();

		final JMenuItem button = new JMenuItem("start listening");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ClientController.getClientController().startListeningForMap(new User("Julius", "Julius-token"), new MapIdentifier("-1", "5"));
			}
		});

		final JMenuBar bar = new JMenuBar();
		bar.add(button);
		controller.getModeController().addMenuContributor(new IMenuContributor() {

			@Override
			public void updateMenus(ModeController modeController, MenuBuilder builder) {
				builder.addMenuItem("/menu_bar/help", button, "test", 1);
			}
		});
	}
	
	@Override
	public void stop(BundleContext context) {
		clientController.stop();
	}
}