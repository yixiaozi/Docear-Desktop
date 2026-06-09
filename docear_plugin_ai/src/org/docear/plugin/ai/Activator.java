package org.docear.plugin.ai;

import java.util.Hashtable;

import org.freeplane.core.util.LogUtils;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.main.osgi.IModeControllerExtensionProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        LogUtils.info("Docear AI plugin starting...");

        final Hashtable<String, String[]> props = new Hashtable<String, String[]>();
        props.put("mode", new String[] { MModeController.MODENAME });

        context.registerService(IModeControllerExtensionProvider.class.getName(),
            new IModeControllerExtensionProvider() {
                @Override
                public void installExtension(ModeController modeController) {
                    DocearAiController.install(modeController);
                    LogUtils.info("Docear AI controller installed.");
                }
            }, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LogUtils.info("Docear AI plugin stopped.");
    }
}
