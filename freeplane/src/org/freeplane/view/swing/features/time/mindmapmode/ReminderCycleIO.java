package org.freeplane.view.swing.features.time.mindmapmode;

import org.freeplane.core.io.IAttributeHandler;
import org.freeplane.core.io.IAttributeWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

/**
 * Registers read/write of DocearReminder cycle attributes on {@code <node>} elements.
 */
final class ReminderCycleIO {
	private ReminderCycleIO() {
	}

	static void install(final ModeController modeController) {
		final MapController mapController = modeController.getMapController();
		registerReadHandler(mapController, ReminderCycleAttributes.REMINDERTYPE, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRemindType(value);
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RHOUR, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRHour(parseInt(value, 1));
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RDAYS, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRDays(parseInt(value, 1));
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RWEEK, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRWeek(parseInt(value, 1));
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RWEEKS, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRWeeks(value);
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RMONTH, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRMonth(parseInt(value, 1));
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.RYEAR, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setRYear(parseInt(value, 1));
			}
		});
		registerReadHandler(mapController, ReminderCycleAttributes.EBSTRING, new AttributeSetter() {
			void set(final ReminderCycleExtension extension, final String value) {
				extension.setEbString(parseInt(value, 0));
			}
		});
		mapController.getWriteManager().addAttributeWriter(NodeBuilder.XML_NODE, new IAttributeWriter() {
			public void writeAttributes(final ITreeWriter writer, final Object userObject, final String tag) {
				if (!NodeBuilder.XML_NODE.equals(tag)) {
					return;
				}
				final NodeModel node = (NodeModel) userObject;
				final ReminderCycleExtension extension = ReminderCycleExtension.getExtension(node);
				if (extension == null) {
					return;
				}
				final ReminderCycleAttributes.CycleConfig config = extension.toConfig();
				if (!config.isRecurring()) {
					return;
				}
				writer.addAttribute(ReminderCycleAttributes.REMINDERTYPE, config.remindType);
				final int interval = config.interval <= 0 ? 1 : config.interval;
				if (ReminderCycleAttributes.TYPE_HOUR.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.RHOUR, Integer.toString(interval));
				}
				else if (ReminderCycleAttributes.TYPE_DAY.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.RDAYS, Integer.toString(interval));
				}
				else if (ReminderCycleAttributes.TYPE_WEEK.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.RWEEK, Integer.toString(interval));
					writer.addAttribute(ReminderCycleAttributes.RWEEKS, config.weekDays);
				}
				else if (ReminderCycleAttributes.TYPE_MONTH.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.RMONTH, Integer.toString(interval));
				}
				else if (ReminderCycleAttributes.TYPE_YEAR.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.RYEAR, Integer.toString(interval));
				}
				else if (ReminderCycleAttributes.TYPE_EB.equals(config.remindType)) {
					writer.addAttribute(ReminderCycleAttributes.EBSTRING, Integer.toString(config.ebString));
				}
			}
		});
	}

	private static void registerReadHandler(final MapController mapController, final String attributeName,
			final AttributeSetter setter) {
		mapController.getReadManager().addAttributeHandler(NodeBuilder.XML_NODE, attributeName, new IAttributeHandler() {
			public void setAttribute(final Object userObject, final String value) {
				final ReminderCycleExtension extension = ReminderCycleExtension
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
		abstract void set(ReminderCycleExtension extension, String value);
	}
}
