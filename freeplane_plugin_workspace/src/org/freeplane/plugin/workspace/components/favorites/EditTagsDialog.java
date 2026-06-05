package org.freeplane.plugin.workspace.components.favorites;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.TextUtils;
import org.freeplane.plugin.workspace.features.favorites.FavoritesAndTagsStore;

public final class EditTagsDialog {

	private EditTagsDialog() {
	}

	public static void showForUri(final String uri) {
		if (uri == null) {
			return;
		}
		final FavoritesAndTagsStore store = FavoritesAndTagsStore.getInstance();
		final JDialog dialog = new JDialog(UITools.getFrame(), TextUtils.getText("workspace.action.favorites.edit.tags.label"), true);
		dialog.setLayout(new BorderLayout(8, 8));
		final JPanel content = new JPanel(new BorderLayout(4, 4));
		content.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
		final JTextField tagsField = new JTextField(joinTags(store.getTags(uri)), 28);
		content.add(new JLabel(TextUtils.getText("workspace.favorites.tags.input.label")), BorderLayout.NORTH);
		content.add(tagsField, BorderLayout.CENTER);
		final JPanel presetsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
		for (final Iterator it = store.getQuickSelectTags().iterator(); it.hasNext();) {
			final String tag = (String) it.next();
			final JButton presetButton = new JButton(tag);
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
				store.setTags(uri, parseTags(tagsField.getText()));
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

	private static void appendTag(final JTextField field, final String tag) {
		final Set tags = parseTags(field.getText());
		tags.add(tag);
		field.setText(joinTags(tags));
	}

	private static Set parseTags(final String value) {
		final LinkedHashSet tags = new LinkedHashSet();
		if (value == null) {
			return tags;
		}
		final String[] parts = value.split(",");
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
