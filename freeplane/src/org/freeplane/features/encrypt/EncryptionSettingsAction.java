package org.freeplane.features.encrypt;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

public class EncryptionSettingsAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;
	public static final String KEY = "EncryptionSettingsAction";

	public EncryptionSettingsAction() {
		super(KEY);
	}

	public void actionPerformed(ActionEvent e) {
		showSettingsDialog();
	}

	static void showSettingsDialog() {
		final JPasswordField passwordField = new JPasswordField(EncryptionConfig.getPassword(), 32);
		final JPasswordField confirmField = new JPasswordField(EncryptionConfig.getPassword(), 32);
		final JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(4, 4, 4, 4);
		panel.add(new JLabel(TextUtils.getText("encryption.settings.password")), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		panel.add(passwordField, c);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		panel.add(new JLabel(TextUtils.getText("encryption.settings.confirm")), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		panel.add(confirmField, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		panel.add(new JLabel(TextUtils.getText("encryption.settings.hint")), c);

		int option = JOptionPane.showConfirmDialog(Controller.getCurrentController().getViewController().getFrame(),
				panel, TextUtils.getText("encryption.settings.title"), JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
		if (option != JOptionPane.OK_OPTION) {
			return;
		}

		String password = new String(passwordField.getPassword());
		String confirm = new String(confirmField.getPassword());

		if (!password.equals(confirm)) {
			JOptionPane.showMessageDialog(Controller.getCurrentController().getViewController().getContentPane(),
					TextUtils.getText("encryption.settings.mismatch"), "Freeplane", JOptionPane.ERROR_MESSAGE);
			return;
		}

		EncryptionConfig.setPassword(password);
	}
}