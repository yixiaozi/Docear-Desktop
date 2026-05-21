package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.ui.IMapViewManager;

public class TodoTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String TODO_ICON_NAME = "hourglass";

	private static final class TodoRecord {
		private final NodeModel node;
		private final String nodeText;

		private TodoRecord(NodeModel node, String nodeText) {
			this.node = node;
			this.nodeText = nodeText;
		}
	}

	private static final class GroupLabel {
		private final String text;

		private GroupLabel(String text) {
			this.text = text;
		}
	}

	private final JTree tree = new JTree(new DefaultMutableTreeNode("\u5F85\u529E"));
	private final ModeController modeController;

	public TodoTabPanel(final ModeController modeController) {
		super(new BorderLayout(4, 4));
		this.modeController = modeController;

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		installArrowKeyNavigation();

		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;
			private final Color TODO_COLOR = new Color(0, 102, 204);

			public Component getTreeCellRendererComponent(JTree pTree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(pTree, value, sel, expanded, leaf, row, hasFocus);
				setOpenIcon(null);
				setClosedIcon(null);
				setLeafIcon(null);
				Object user = ((DefaultMutableTreeNode) value).getUserObject();
				if (user instanceof GroupLabel) {
					setText(((GroupLabel) user).text);
					if (!sel) {
						setForeground(null);
					}
				} else if (user instanceof TodoRecord) {
					TodoRecord record = (TodoRecord) user;
					String text = record.nodeText == null ? ""
							: HtmlUtils.removeHtmlTagsFromString(record.nodeText).replaceAll("\\s+", " ").trim();
					setText(text);
					if (!sel) {
						setForeground(TODO_COLOR);
					}
				}
				return this;
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 1) {
					openSelectedTodo();
				}
			}
		});

		add(new JScrollPane(tree), BorderLayout.CENTER);
		addListeners();
		reloadTodos();
	}

	private void addListeners() {
		final MapController mapController = modeController.getMapController();
		mapController.addNodeChangeListener(new INodeChangeListener() {
			public void nodeChanged(NodeChangeEvent event) {
				reloadTodos();
			}
		});
		mapController.addMapChangeListener(new IMapChangeListener() {
			public void mapChanged(MapChangeEvent event) {
				reloadTodos();
			}

			public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
				reloadTodos();
			}

			public void onNodeDeleted(NodeModel parent, NodeModel child, int index) {
				reloadTodos();
			}

			public void onNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child,
					int newIndex) {
				reloadTodos();
			}

			public void onPreNodeDelete(NodeModel oldParent, NodeModel selectedNode, int index) {
			}

			public void onPreNodeMoved(NodeModel oldParent, int oldIndex, NodeModel newParent, NodeModel child,
					int newIndex) {
			}
		});
		final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
		mapViewManager.addMapSelectionListener(new IMapSelectionListener() {
			public void beforeMapChange(MapModel oldMap, MapModel newMap) {
			}

			public void afterMapChange(MapModel oldMap, MapModel newMap) {
				reloadTodos();
			}
		});
	}

	private void openSelectedTodo() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return;
		}
		Object nodeObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(nodeObj instanceof TodoRecord)) {
			return;
		}
		TodoRecord record = (TodoRecord) nodeObj;
		Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(record.node);
		Controller.getCurrentModeController().getMapController().centerNode(record.node);
		tree.requestFocusInWindow();
	}

	private void installArrowKeyNavigation() {
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0), "todo.up");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0), "todo.down");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0), "todo.left");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0), "todo.right");

		tree.getActionMap().put("todo.up", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				int row = tree.getLeadSelectionRow();
				if (row <= 0) {
					return;
				}
				tree.setSelectionRow(row - 1);
				tree.scrollRowToVisible(row - 1);
			}
		});

		tree.getActionMap().put("todo.down", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				int row = tree.getLeadSelectionRow();
				if (row < 0) {
					row = 0;
				}
				if (row >= tree.getRowCount() - 1) {
					return;
				}
				tree.setSelectionRow(row + 1);
				tree.scrollRowToVisible(row + 1);
			}
		});

		tree.getActionMap().put("todo.left", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				TreePath path = tree.getSelectionPath();
				if (path == null) {
					return;
				}
				if (tree.isExpanded(path)) {
					tree.collapsePath(path);
				} else if (path.getParentPath() != null) {
					tree.setSelectionPath(path.getParentPath());
				}
			}
		});

		tree.getActionMap().put("todo.right", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(java.awt.event.ActionEvent e) {
				TreePath path = tree.getSelectionPath();
				if (path == null) {
					return;
				}
				if (!tree.isExpanded(path)) {
					tree.expandPath(path);
				}
			}
		});
	}

	private void reloadTodos() {
		MapModel map = Controller.getCurrentController().getMap();
		if (map == null || map.getRootNode() == null) {
			return;
		}

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u5F85\u529E");
		Set<NodeModel> addedNodes = new HashSet();
		collectTodoNodes(map.getRootNode(), root, addedNodes);
		tree.setModel(new DefaultTreeModel(root));

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	private void collectTodoNodes(NodeModel node, DefaultMutableTreeNode parentTreeNode, Set<NodeModel> addedNodes) {
		List<NodeModel> children = node.getChildren();
		for (NodeModel child : children) {
			boolean childHasTodo = hasHourglassIcon(child);
			boolean childHasTodoDescendant = hasTodoDescendant(child);
			
			if (!childHasTodo && !childHasTodoDescendant) {
				continue;
			}
			
			String childText = child.getText();
			String childLabel = childText == null ? "" : HtmlUtils.removeHtmlTagsFromString(childText).replaceAll("\\s+", " ").trim();
			
			if (childHasTodo && !"bin".equalsIgnoreCase(childLabel)) {
				if (addedNodes.contains(child)) {
					continue;
				}
				addedNodes.add(child);
				
				TodoRecord record = new TodoRecord(child, childLabel);
				DefaultMutableTreeNode todoNode = new DefaultMutableTreeNode(record, false);
				parentTreeNode.add(todoNode);
			} else if (childHasTodoDescendant) {
				DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(new GroupLabel(childLabel), true);
				collectTodoNodes(child, childTreeNode, addedNodes);
				
				if (childTreeNode.getChildCount() > 0) {
					parentTreeNode.add(childTreeNode);
				}
			}
		}
	}
	
	private boolean hasTodoDescendant(NodeModel node) {
		if (hasHourglassIcon(node)) {
			return true;
		}
		for (NodeModel child : node.getChildren()) {
			if (hasTodoDescendant(child)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasHourglassIcon(NodeModel node) {
		Collection<MindIcon> icons = IconController.getController().getIcons(node);
		for (MindIcon icon : icons) {
			if (TODO_ICON_NAME.equalsIgnoreCase(icon.getName())) {
				return true;
			}
		}
		return false;
	}
}