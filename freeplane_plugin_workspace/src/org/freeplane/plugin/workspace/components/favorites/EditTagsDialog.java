package org.freeplane.plugin.workspace.components.favorites;

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

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.features.favorites.FavoritesAndTagsStore;
import org.freeplane.plugin.workspace.features.favorites.TagTextUtils;

public final class EditTagsDialog {

	private static final Dimension INPUT_SIZE = new Dimension(420, 120);

	private EditTagsDialog() {
	}

	public static void showForUri(final String uri) {
		if (uri == null) {
			return;
		}
		final FavoritesAndTagsStore store = FavoritesAndTagsStore.getInstance();
		final JDialog dialog = new JDialog(UITools.getFrame(), TextUtils.getText("workspace.action.favorites.edit.tags.label"), true);
		dialog.setLayout(new BorderLayout(8, 8));
		final JPanel content = new JPanel(new BorderLayout(6, 6));
		content.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final JTextArea tagsField = new JTextArea(joinTags(store.getTags(uri)));
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
		content.add(new JLabel(TextUtils.getText("workspace.favorites.tags.input.label")), BorderLayout.NORTH);
		content.add(inputScroll, BorderLayout.CENTER);
		final JPanel presetsPanel = new JPanel(new WrapFlowLayout());
		for (final Iterator it = store.getQuickSelectTags().iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			final JButton presetButton = TagChipFactory.createPresetChip(tag);
			presetButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					appendTag(tagsField, tag);
				}
			});
			presetsPanel.add(presetButton);
		}
		content.add(presetsPanel, BorderLayout.SOUTH);
		dialog.add(content, BorderLayout.CENTER);
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton okButton = new JButton(TextUtils.getText("ok"));
		final JButton cancelButton = new JButton(TextUtils.getText("cancel"));
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				if (!store.isFavorite(uri)) {
					store.addFavorite(uri);
				}
				store.setTags(uri, parseTags(TagTextUtils.normalizeSeparators(tagsField.getText())));
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

	private static void appendTag(final JTextArea field, final String tag) {
		final Set tags = parseTags(field.getText());
		tags.add(tag);
		field.setText(joinTags(tags));
	}

	private static Set parseTags(final String value) {
		final LinkedHashSet tags = new LinkedHashSet();
		if (value == null) {
			return tags;
		}
		final String[] parts = TagTextUtils.normalizeSeparators(value).split(",");
		for (int i = 0; i < parts.length; i++) {
			final String trimmed = parts[i].trim();
			if (trimmed.length() > 0) {
				tags.add(trimmed);
			}
		}
		return tags;
	}

	private static String joinTags(final Set tags) {
		final StringBuilder builder = new StringBuilder();
		for (final Iterator it = tags.iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(tag);
		}
		return builder.toString();
	}
}
