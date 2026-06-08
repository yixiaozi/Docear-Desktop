package org.docear.plugin.core.todoist;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

public class TodoistSettingsAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "TodoistSettingsAction";

	public TodoistSettingsAction() {
		super(KEY);
	}

	public void actionPerformed(ActionEvent e) {
		showSettingsDialog();
	}

	static void showSettingsDialog() {
		final JTextField projectField = new JTextField(TodoistConfig.getProjectName(), 32);
		final JPasswordField tokenField = new JPasswordField(TodoistConfig.getApiToken(), 32);
		final JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 4, 4, 4);
		panel.add(new JLabel(TextUtils.getText("todoist.settings.token")), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		panel.add(tokenField, c);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		panel.add(new JLabel(TextUtils.getText("todoist.settings.project")), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		panel.add(projectField, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		panel.add(new JLabel(TextUtils.getText("todoist.settings.hint")), c);

		int option = JOptionPane.showConfirmDialog(Controller.getCurrentController().getViewController().getFrame(),
				panel, TextUtils.getText("todoist.settings.title"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (option != JOptionPane.OK_OPTION) {
			return;
		}
		TodoistConfig.setApiToken(new String(tokenField.getPassword()));
		TodoistConfig.setProjectName(projectField.getText());
	}
}
