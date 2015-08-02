package org.docear.plugin.services.features.user;

import org.freeplane.plugin.workspace.WorkspaceController;

public class LoadWorkspaceEvent implements Runnable {

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/
	
	
	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
	public void run() {
		WorkspaceController.load();
	}
}