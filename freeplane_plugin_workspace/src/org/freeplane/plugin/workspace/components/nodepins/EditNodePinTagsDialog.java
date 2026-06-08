package org.freeplane.plugin.workspace.components.nodepins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.workspace.components.favorites.TagChipFactory;
import org.freeplane.plugin.workspace.components.favorites.WrapFlowLayout;
import org.freeplane.plugin.workspace.features.favorites.TagTextUtils;
import org.freeplane.plugin.workspace.features.nodepins.NodeDetailsTagService;
import org.freeplane.plugin.workspace.features.nodepins.NodeDetailsTagUtils;
import org.freeplane.plugin.workspace.features.nodepins.NodeMindMapActionUtils;

public final class EditNodePinTagsDialog {

	private static final Dimension INPUT_SIZE = new Dimension(420, 120);
	private static final String TAG_PROPERTY = "workspace.node.tag";

	private EditNodePinTagsDialog() {
	}

	public static void showForNode(final NodeModel node) {
		if (!NodeMindMapActionUtils.isSavedMapNode(node)) {
			return;
		}
		openDialog(node);
	}

	public static void showForKey(final String globalKey) {
		if (globalKey == null) {
			return;
		}
		NodeMindMapActionUtils.withNodeByKey(globalKey, new NodeMindMapActionUtils.NodeRunnable() {
			public void run(final NodeModel node) {
				openDialog(node);
			}
		});
	}

	private static void openDialog(final NodeModel node) {
		final JDialog dialog = new JDialog(UITools.getFrame(), TextUtils.getText("workspace.action.nodepins.edit.tags"), true);
		dialog.setLayout(new BorderLayout(8, 8));
		final JPanel content = new JPanel(new BorderLayout(6, 6));
		content.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final JTextArea tagsField = new JTextArea(NodeDetailsTagUtils.joinTagNames(NodeDetailsTagService.getUserTags(node)));
		tagsField.setLineWrap(true);
		tagsField.setWrapStyleWord(true);
		tagsField.addFocusListener(new FocusAdapter() {
			public void focusLost(final FocusEvent e) {
				final String normalized = TagTextUtils.normalizeSeparators(tagsField.getText());
				if (!normalized.equals(tagsField.getText())) {
					tagsField.setText(normalized);
				}
			}
		});
		final JScrollPane inputScroll = new JScrollPane(tagsField);
		inputScroll.setPreferredSize(INPUT_SIZE);
		content.add(new JLabel(TextUtils.getText("workspace.nodepins.tags.input.label")), BorderLayout.NORTH);
		content.add(inputScroll, BorderLayout.CENTER);
		final JPanel presetsPanel = new JPanel(new WrapFlowLayout());
		buildPresetButtons(presetsPanel, tagsField);
		tagsField.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(final DocumentEvent e) {
				syncPresetButtonStates(presetsPanel, tagsField);
			}

			public void removeUpdate(final DocumentEvent e) {
				syncPresetButtonStates(presetsPanel, tagsField);
			}

			public void changedUpdate(final DocumentEvent e) {
				syncPresetButtonStates(presetsPanel, tagsField);
			}
		});
		content.add(presetsPanel, BorderLayout.SOUTH);
		dialog.add(content, BorderLayout.CENTER);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okButton = new JButton(TextUtils.getText("ok"));
		final JButton cancelButton = new JButton(TextUtils.getText("cancel"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				NodeDetailsTagService.setUserTags(node, NodeDetailsTagUtils.parseTagNamesFromText(TagTextUtils
						.normalizeSeparators(tagsField.getText())));
				dialog.dispose();
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				dialog.dispose();
			}
		});
		buttons.add(okButton);
		buttons.add(cancelButton);
		dialog.add(buttons, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(UITools.getFrame());
		dialog.setVisible(true);
	}

	private static void buildPresetButtons(final JPanel presetsPanel, final JTextArea tagsField) {
		presetsPanel.removeAll();
		final Set currentTags = NodeDetailsTagUtils.parseTagNamesFromText(tagsField.getText());
		for (int i = 0; i < NodeDetailsTagUtils.PRESET_TAGS.length; i++) {
			final String tag = NodeDetailsTagUtils.PRESET_TAGS[i];
			final JToggleButton presetButton = TagChipFactory.createEditChip(tag, currentTags.contains(tag));
			presetButton.putClientProperty(TAG_PROPERTY, tag);
			presetButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					toggleTag(tagsField, tag);
					syncPresetButtonStates(presetsPanel, tagsField);
				}
			});
			presetsPanel.add(presetButton);
		}
		for (final Iterator it = collectExtraTags(currentTags).iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			final JToggleButton presetButton = TagChipFactory.createEditChip(tag, true);
			presetButton.putClientProperty(TAG_PROPERTY, tag);
			presetButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					toggleTag(tagsField, tag);
					syncPresetButtonStates(presetsPanel, tagsField);
				}
			});
			presetsPanel.add(presetButton);
		}
		presetsPanel.revalidate();
		presetsPanel.repaint();
	}

	private static Set collectExtraTags(final Set currentTags) {
		final LinkedHashSet extras = new LinkedHashSet();
		for (final Iterator it = currentTags.iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			boolean preset = false;
			for (int i = 0; i < NodeDetailsTagUtils.PRESET_TAGS.length; i++) {
				if (NodeDetailsTagUtils.PRESET_TAGS[i].equals(tag)) {
					preset = true;
					break;
				}
			}
			if (!preset && !NodeDetailsTagUtils.PIN_TAG.equals(tag)) {
				extras.add(tag);
			}
		}
		return extras;
	}

	private static void syncPresetButtonStates(final JPanel presetsPanel, final JTextArea tagsField) {
		final Set currentTags = NodeDetailsTagUtils.parseTagNamesFromText(tagsField.getText());
		for (int i = 0; i < presetsPanel.getComponentCount(); i++) {
			final JToggleButton button = (JToggleButton) presetsPanel.getComponent(i);
			final String tag = (String) button.getClientProperty(TAG_PROPERTY);
			if (tag != null) {
				button.setSelected(currentTags.contains(tag));
			}
		}
	}

	private static void toggleTag(final JTextArea field, final String tag) {
		final Set tags = NodeDetailsTagUtils.parseTagNamesFromText(field.getText());
		if (tags.contains(tag)) {
			tags.remove(tag);
		}
		else {
			tags.add(tag);
		}
		field.setText(NodeDetailsTagUtils.joinTagNames(tags));
	}
}
