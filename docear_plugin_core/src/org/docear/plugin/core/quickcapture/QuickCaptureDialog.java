package org.docear.plugin.core.quickcapture;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

class QuickCaptureDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static QuickCaptureDialog current;

	private final JTextArea textArea = new JTextArea(8, 50);

	QuickCaptureDialog(final Frame owner) {
		super(owner, "\u7075\u611f\u7b14\u8bb0", true);
		buildUi();
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setSize(new Dimension(600, 320));
		setMinimumSize(new Dimension(500, 240));
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent e) {
				centerOnScreen();
				focusInput();
			}
		});
	}

	private void buildUi() {
		final JPanel content = new JPanel(new BorderLayout(8, 8));
		content.setBorder(new javax.swing.border.EmptyBorder(10, 10, 10, 10));

		final JPanel top = new JPanel(new BorderLayout(4, 4));
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		top.add(new JScrollPane(textArea), BorderLayout.CENTER);
		content.add(top, BorderLayout.CENTER);

		final JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 0));
		final JButton cancelButton = new JButton("\u53d6\u6d88");
		final JButton okButton = new JButton("\u6dfb\u52a0");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				submit();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		});
		buttons.add(cancelButton);
		buttons.add(okButton);
		content.add(buttons, BorderLayout.SOUTH);

		setContentPane(content);
		getRootPane().setDefaultButton(okButton);

		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
		        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "quickcapture.cancel");
		getRootPane().getActionMap().put("quickcapture.cancel", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent e) {
				dispose();
			}
		});
		textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
		        "quickcapture.submit");
		textArea.getActionMap().put("quickcapture.submit", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(final ActionEvent e) {
				submit();
			}
		});
	}

	private void centerOnScreen() {
		final Rectangle bounds = getBounds();
		final java.awt.Point mouseLocation = java.awt.MouseInfo.getPointerInfo().getLocation();
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final java.awt.GraphicsDevice[] screens = ge.getScreenDevices();
		
		Rectangle screenBounds = ge.getMaximumWindowBounds();
		for (java.awt.GraphicsDevice screen : screens) {
			Rectangle screenRect = screen.getDefaultConfiguration().getBounds();
			if (screenRect.contains(mouseLocation)) {
				screenBounds = screenRect;
				break;
			}
		}
		
		setLocation(screenBounds.x + (screenBounds.width - bounds.width) / 2, 
		            screenBounds.y + (screenBounds.height - bounds.height) / 2);
	}

	private void focusInput() {
		textArea.selectAll();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				textArea.requestFocusInWindow();
			}
		});
	}

	private void submit() {
		final String text = textArea.getText();
		if (text == null || text.trim().length() == 0) {
			return;
		}
		final boolean ok = QuickCaptureController.capture(text);
		if (ok) {
			setVisible(false);
			dispose();
		}
	}

	static void openDialog() {
		if (current != null && current.isDisplayable()) {
			current.textArea.setText("");
			current.setVisible(true);
			current.toFront();
			current.centerOnScreen();
			current.focusInput();
			return;
		}
		current = new QuickCaptureDialog(null);
		current.setVisible(true);
	}
}
