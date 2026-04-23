package org.docear.plugin.services.features.payment;

import org.docear.plugin.services.ADocearServiceFeature;
import org.docear.plugin.services.features.payment.view.PayPalBanner;
import org.freeplane.features.mode.ModeController;

public class DocearPaymentController extends ADocearServiceFeature {
	private PayPalBanner payPalBanner;
	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	
	@Override
	protected void installDefaults(ModeController modeController) {
		// Disable donation banner in customized build.
		payPalBanner = null;
		
	}

	
	
	@Override
	public void shutdown() {
	}
}
