package org.freeplane.plugin.workspace.components;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JTabbedPane;

/**
 * JTabbedPane that supports reordering tabs by dragging tab headers.
 */
public class DraggableTabbedPane extends JTabbedPane {

	private static final long serialVersionUID = 1L;

	public interface TabReorderListener {
		void tabReordered(int fromIndex, int toIndex);
	}

	private int dragTabIndex = -1;
	private TabReorderListener reorderListener;

	public DraggableTabbedPane() {
		addMouseListener(new MouseAdapter() {
			public void mousePressed(final MouseEvent e) {
				if (!isEnabled()) {
					return;
				}
				dragTabIndex = indexAtLocation(e.getX(), e.getY());
			}

			public void mouseReleased(final MouseEvent e) {
				if (dragTabIndex < 0) {
					return;
				}
				final int dropIndex = indexAtLocation(e.getX(), e.getY());
				if (dropIndex >= 0 && dropIndex != dragTabIndex) {
					reorderTab(dragTabIndex, dropIndex);
				}
				dragTabIndex = -1;
			}
		});
	}

	public void setTabReorderListener(final TabReorderListener listener) {
		this.reorderListener = listener;
	}

	private void reorderTab(int fromIndex, int toIndex) {
		if (fromIndex < 0 || toIndex < 0 || fromIndex >= getTabCount() || toIndex >= getTabCount()) {
			return;
		}
		if (fromIndex == toIndex) {
			return;
		}
		final Component component = getComponentAt(fromIndex);
		final String title = getTitleAt(fromIndex);
		final Icon icon = getIconAt(fromIndex);
		final String tip = getToolTipTextAt(fromIndex);
		final boolean enabled = isEnabledAt(fromIndex);
		final int selectedIndex = getSelectedIndex();

		remove(fromIndex);
		if (fromIndex < toIndex) {
			toIndex--;
		}
		insertTab(title, icon, component, tip, toIndex);
		setEnabledAt(toIndex, enabled);

		if (selectedIndex == fromIndex) {
			setSelectedIndex(toIndex);
		}
		else if (fromIndex < selectedIndex && toIndex >= selectedIndex) {
			setSelectedIndex(selectedIndex - 1);
		}
		else if (fromIndex > selectedIndex && toIndex <= selectedIndex) {
			setSelectedIndex(selectedIndex + 1);
		}

		if (reorderListener != null) {
			reorderListener.tabReordered(fromIndex, toIndex);
		}
	}
}
