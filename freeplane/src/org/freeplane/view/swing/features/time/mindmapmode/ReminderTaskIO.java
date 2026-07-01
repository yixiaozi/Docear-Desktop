package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.io.IAttributeHandler;
import org.freeplane.core.io.IAttributeWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

/**
 * Registers read/write of DocearReminder task attributes on {@code <node>} elements.
 */
final class ReminderTaskIO {
	private ReminderTaskIO() {
	}

	static void install(final ModeController modeController) {
		final MapController mapController = modeController.getMapController();
		registerReadHandler(mapController, ReminderTaskAttributes.TASKTIME, new AttributeSetter() {
			void set(final ReminderTaskExtension extension, final String value) {
				extension.setTaskTime(parseInt(value, 0));
			}
		});
		registerReadHandler(mapController, ReminderTaskAttributes.TASKLEVEL, new AttributeSetter() {
			void set(final ReminderTaskExtension extension, final String value) {
				extension.setTaskLevel(parseInt(value, 0));
			}
		});
		registerReadHandler(mapController, ReminderTaskAttributes.JINJI, new AttributeSetter() {
			void set(final ReminderTaskExtension extension, final String value) {
				extension.setJinji(parseInt(value, 0));
			}
		});
		mapController.getWriteManager().addAttributeWriter(NodeBuilder.XML_NODE, new IAttributeWriter() {
			public void writeAttributes(final ITreeWriter writer, final Object userObject, final String tag) {
				if (!NodeBuilder.XML_NODE.equals(tag)) {
					return;
				}
				final NodeModel node = (NodeModel) userObject;
				final ReminderTaskExtension extension = ReminderTaskExtension.getExtension(node);
				if (extension == null || extension.isEmpty()) {
					return;
				}
				writer.addAttribute(ReminderTaskAttributes.TASKTIME, Integer.toString(extension.getTaskTime()));
				writer.addAttribute(ReminderTaskAttributes.TASKLEVEL, Integer.toString(extension.getTaskLevel()));
				writer.addAttribute(ReminderTaskAttributes.JINJI, Integer.toString(extension.getJinji()));
			}
		});
	}

	private static void registerReadHandler(final MapController mapController, final String attributeName,
			final AttributeSetter setter) {
		mapController.getReadManager().addAttributeHandler(NodeBuilder.XML_NODE, attributeName, new IAttributeHandler() {
			public void setAttribute(final Object userObject, final String value) {
				final ReminderTaskExtension extension = ReminderTaskExtension
						.getOrCreateExtension((NodeModel) userObject);
				setter.set(extension, value);
			}
		});
	}

	private static int parseInt(final String value, final int defaultValue) {
		if (value == null || value.trim().length() == 0) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value.trim());
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private abstract static class AttributeSetter {
		abstract void set(ReminderTaskExtension extension, String value);
	}
}
