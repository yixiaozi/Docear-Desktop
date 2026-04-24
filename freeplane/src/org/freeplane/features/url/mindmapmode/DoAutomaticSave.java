/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.url.mindmapmode;

import java.awt.EventQueue;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.TimerTask;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.map.mindmapmode.MMapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.url.UrlManager;

public class DoAutomaticSave extends TimerTask {
	static final String AUTOSAVE_EXTENSION = "autosave";
	/**
	 * This value is compared with the result of
	 * getNumberOfChangesSinceLastSave(). If the values coincide, no further
	 * automatic saving is performed until the value changes again.
	 */
	private int changeState;
	final private boolean filesShouldBeDeletedAfterShutdown;
	final private MapModel model;
	final private int numberOfFiles;
	private final File singleBackupDirectory;
	static final String BACKUP_DIR = ".backup";

	public DoAutomaticSave(final MapModel model, final int numberOfTempFiles,
	                       final boolean filesShouldBeDeletedAfterShutdown, boolean useSingleBackupDirectory,
	                       final String singleBackupDirectory) {
		this.model = model;
		numberOfFiles = ((numberOfTempFiles > 0) ? numberOfTempFiles : 1);
		this.filesShouldBeDeletedAfterShutdown = filesShouldBeDeletedAfterShutdown;
		this.singleBackupDirectory = useSingleBackupDirectory ? new File(singleBackupDirectory) : null;
		changeState = model.getNumberOfChangesSinceLastSave();
	}

	@Override
	public void run() {
		final MMapModel mModel = (MMapModel) model;
		if (handleExternalChange(mModel)) {
			return;
		}
		/* Map is dirty enough? */
		if (model.getNumberOfChangesSinceLastSave() == changeState) {
			return;
		}
		changeState = model.getNumberOfChangesSinceLastSave();
		if (changeState == 0) {
			/* map was recently saved. */
			return;
		}
		try {
			Controller.getCurrentController().getViewController().invokeAndWait(new Runnable() {

				public void run() {
					/* Now, it is dirty, we save it. */
					try {
						final ModeController currentModeController = Controller.getCurrentModeController();
						if(!(currentModeController instanceof MModeController))
							return;
						MModeController modeController = ((MModeController) currentModeController);
						final File pathToStore;
						final URL url = model.getURL();
						final File file = new File(url != null ? url.getFile() //
						        : model.getTitle() + UrlManager.FREEPLANE_FILE_EXTENSION);
						if (url == null) {
							pathToStore = new File(ResourceController.getResourceController()
							    .getFreeplaneUserDirectory(), BACKUP_DIR);
						}
						else if (singleBackupDirectory != null) {
							pathToStore = singleBackupDirectory;
						}
						else {
							pathToStore = new File(file.getParent(), BACKUP_DIR);
						}
						pathToStore.mkdirs();
						final File tempFile = MFileManager.renameBackupFiles(pathToStore, file, numberOfFiles,
						    AUTOSAVE_EXTENSION);
						if (tempFile == null) {
							return;
						}
						final MFileManager fileManager = (MFileManager) UrlManager.getController();
						if (!mModel.isExternalModificationDetected()) {
							final File originalFile = mModel.getFile();
							if (originalFile != null && originalFile.exists() && originalFile.canWrite()
							        && fileManager.saveInternal(mModel, originalFile, true /*=internal call*/)) {
								modeController.getMapController().setSaved(mModel, true);
								mModel.setKnownFileTimestamp(originalFile.lastModified());
								mModel.setExternalModificationDetected(false);
								modeController.getController().getViewController()
								    .out(TextUtils.format("automatically_save_message", originalFile));
								return;
							}
						}
						if (filesShouldBeDeletedAfterShutdown) {
							tempFile.deleteOnExit();
						}
						fileManager.saveInternal(mModel, tempFile, true /*=internal call*/);
						modeController.getController().getViewController()
						    .out(TextUtils.format("automatically_save_message", tempFile));
					}
					catch (final Exception e) {
						LogUtils.severe("Error in automatic MapModel.save(): ", e);
					}
				}
			});
		}
		catch (final Exception e) {
			LogUtils.severe(e);
		}
	}

	private boolean handleExternalChange(final MMapModel mModel) {
		try {
			final File file = mModel.getFile();
			if (file == null) {
				return false;
			}
			if (!file.exists()) {
				return false;
			}
			final long actualTimestamp = file.lastModified();
			final long knownTimestamp = mModel.getKnownFileTimestamp();
			if (knownTimestamp <= 0L) {
				mModel.setKnownFileTimestamp(actualTimestamp);
				return false;
			}
			if (actualTimestamp <= knownTimestamp) {
				return false;
			}
			if (mModel.isExternalModificationDetected()) {
				return true;
			}
			if (model.getNumberOfChangesSinceLastSave() == 0 && Controller.getCurrentController().getMap() == model) {
				mModel.setExternalModificationDetected(true);
				mModel.setKnownFileTimestamp(actualTimestamp);
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							final ModeController currentModeController = Controller.getCurrentModeController();
							if (currentModeController instanceof MModeController) {
								((MMapController) ((MModeController) currentModeController).getMapController()).restoreCurrentMapPreservingSelection();
							}
						}
						catch (Exception e) {
							LogUtils.warn(e);
						}
						finally {
							mModel.setExternalModificationDetected(false);
						}
					}
				});
				return true;
			}
			if (!mModel.isExternalModificationDetected()) {
				Controller.getCurrentController().getViewController().out(
				    TextUtils.getText("external_map_change_detected_skip_auto_save"));
				mModel.setExternalModificationDetected(true);
			}
			return true;
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
		return false;
	}
}
