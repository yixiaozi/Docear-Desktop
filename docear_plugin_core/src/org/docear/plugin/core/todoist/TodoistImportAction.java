package org.docear.plugin.core.todoist;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.TextUtils;

public class TodoistImportAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "TodoistImportAction";

	public TodoistImportAction() {
		super(KEY);
	}

	public void actionPerformed(ActionEvent e) {
		if (TodoistConfig.getApiToken().length() == 0) {
			int option = JOptionPane.showConfirmDialog(TodoistSyncProgressDialog.resolveOwnerFrame(),
					TextUtils.getText("todoist.sync.token_missing"), TextUtils.getText("todoist.import.title"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (option == JOptionPane.OK_OPTION) {
				TodoistSettingsAction.showSettingsDialog();
			}
			if (TodoistConfig.getApiToken().length() == 0) {
				return;
			}
		}
		final Frame owner = TodoistSyncProgressDialog.resolveOwnerFrame();
		final TodoistSyncProgressDialog dialog = TodoistSyncProgressDialog.openImport(owner);
		new SwingWorker() {
			protected Object doInBackground() throws Exception {
				return TodoistImportService.importAllTasks(dialog);
			}

			protected void done() {
				try {
					get();
				}
				catch (InterruptedException ex) {
					dialog.onFailed(ex.getMessage());
					dialog.onFinished(new TodoistSyncResult());
				}
				catch (ExecutionException ex) {
					Throwable cause = ex.getCause();
					dialog.onFailed(cause != null ? cause.getMessage() : ex.getMessage());
					dialog.onFinished(new TodoistSyncResult());
				}
				catch (Exception ex) {
					dialog.onFailed(ex.getMessage());
					dialog.onFinished(new TodoistSyncResult());
				}
			}
		}.execute();
	}
}
