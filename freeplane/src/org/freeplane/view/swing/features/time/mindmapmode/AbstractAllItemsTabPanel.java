package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.MindMapDataRootResolver;
import org.freeplane.core.util.WorkspaceSideTabSnapshot;
import org.freeplane.core.util.WorkspaceSideTabSnapshotRegistry;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractAllItemsTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	protected static final class ItemRecord {
		private final File file;
		private final String nodeId;
		private final String nodeText;
		private final String mapName;

		private ItemRecord(File file, String nodeId, String nodeText, String mapName) {
			this.file = file;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.mapName = mapName;
		}
	}

	protected static final class CachedFileResult {
		private final long modified;
		private final long length;
		private final List items;

		private CachedFileResult(long modified, long length, List items) {
			this.modified = modified;
			this.length = length;
			this.items = items;
		}
	}

	protected static final class GroupLabel {
		private final String text;

		private GroupLabel(String text) {
			this.text = text;
		}
	}

	protected static final class ScanChunk {
		private final String fileKey;
		private final List items;
		private final int scanned;
		private final int total;

		private ScanChunk(String fileKey, List items, int scanned, int total) {
			this.fileKey = fileKey;
			this.items = items;
			this.scanned = scanned;
			this.total = total;
		}
	}

	protected abstract String getIconName();
	protected abstract String getRootLabel();
	protected abstract String getStatusLabelPrefix();

	protected final JButton refreshButton = new JButton("\u5237\u65b0");
	protected final JLabel statusLabel = new JLabel();
	protected final JTree tree = new JTree();
	protected final DecimalFormat twoDigits = new DecimalFormat("00");
	protected final Map cacheByFile = new HashMap();
	protected final Map itemsByKey = new HashMap();
	protected final Map itemKeysByFile = new HashMap();
	protected SwingWorker activeWorker;
	protected boolean rescanRequested;
	protected final Timer autoRefreshTimer;

	public AbstractAllItemsTabPanel() {
		super(new BorderLayout(4, 4));
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.add(statusLabel, BorderLayout.CENTER);
		top.add(refreshButton, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);

		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		installArrowKeyNavigation();

		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;
			private final Color ITEM_COLOR = new Color(0, 102, 204);

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
				} else if (user instanceof ItemRecord) {
					ItemRecord record = (ItemRecord) user;
					String text = record.nodeText == null ? ""
							: HtmlUtils.removeHtmlTagsFromString(record.nodeText).replaceAll("\\s+", " ").trim();
					setText(text);
					if (!sel) {
						setForeground(ITEM_COLOR);
					}
				}
				return this;
			}
		});

		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
					showPopupMenu(e);
				} else if (e.getClickCount() >= 1) {
					openSelectedItem();
				}
			}
		});

		JScrollPane scrollPane = new JScrollPane(tree);
		add(scrollPane, BorderLayout.CENTER);

		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				triggerRescan();
			}
		});

		autoRefreshTimer = new Timer(5 * 60 * 1000, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				triggerRescan();
			}
		});
		autoRefreshTimer.start();

		triggerRescan();
	}

	protected void triggerRescan() {
		if (activeWorker != null) {
			rescanRequested = true;
			return;
		}

		rescanRequested = false;
		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				List files = collectAllMindmapFiles();
				for (int i = 0; i < files.size(); i++) {
					if (rescanRequested) {
						return null;
					}
					File file = (File) files.get(i);
					List items = getItemsForFile(file);
					publish(new ScanChunk(file.getAbsolutePath(), items, i + 1, files.size()));
				}
				return null;
			}

			protected void process(List chunks) {
				for (int i = 0; i < chunks.size(); i++) {
					ScanChunk chunk = (ScanChunk) chunks.get(i);
					mergeChunk(chunk);
					statusLabel.setText(getStatusLabelPrefix() + ": " + itemsByKey.size() + " (" + chunk.scanned + "/" + chunk.total + ")");
				}
			}

			protected void done() {
				activeWorker = null;
				rebuildTreeFromCache();
				statusLabel.setText(getStatusLabelPrefix() + ": " + itemsByKey.size());
				if (rescanRequested) {
					triggerRescan();
				}
			}
		};
		activeWorker.execute();
	}

	private void showPopupMenu(MouseEvent e) {
		final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path == null) {
			return;
		}
		Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		
		JPopupMenu menu = new JPopupMenu();

		if (userObject instanceof ItemRecord) {
			final ItemRecord record = (ItemRecord) userObject;

			JMenuItem openFolderItem = new JMenuItem("\u6253\u5F00\u6587\u4EF6\u5939");
			openFolderItem.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e1) {
					try {
						String filePath = record.file.getAbsolutePath();
						String cmd = "explorer.exe /select,\"" + filePath + "\"";
						Runtime.getRuntime().exec(cmd);
					} catch (Exception ex) {
						LogUtils.warn(ex);
					}
				}
			});
			menu.add(openFolderItem);

			JMenuItem copyItem = new JMenuItem("\u590D\u5236");
			copyItem.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e1) {
					String text = record.nodeText == null ? ""
							: HtmlUtils.removeHtmlTagsFromString(record.nodeText).replaceAll("\\s+", " ").trim();
					Toolkit.getDefaultToolkit().getSystemClipboard()
							.setContents(new StringSelection(text), null);
				}
			});
			menu.add(copyItem);
		} else if (userObject instanceof GroupLabel) {
			final GroupLabel label = (GroupLabel) userObject;
			
			JMenuItem openFolderItem = new JMenuItem("\u6253\u5F00\u6587\u4EF6\u5939");
			openFolderItem.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e1) {
					try {
						String folderPath = findFolderPathForLabel(label.text, path);
						if (folderPath != null) {
							Runtime.getRuntime().exec("explorer.exe \"" + folderPath + "\"");
						}
					} catch (Exception ex) {
						LogUtils.warn(ex);
					}
				}
			});
			menu.add(openFolderItem);
		}

		if (menu.getComponentCount() > 0) {
			menu.show(tree, e.getX(), e.getY());
		}
	}
	
	private String findFolderPathForLabel(String label, TreePath path) {
		if (label.toLowerCase().endsWith(".mm")) {
			for (Object key : itemsByKey.keySet()) {
				ItemRecord record = (ItemRecord) itemsByKey.get(key);
				if (record.mapName.equals(label)) {
					File parent = record.file.getParentFile();
					if (parent != null && parent.exists()) {
						return parent.getAbsolutePath();
					}
				}
			}
			return null;
		}
		
		Object[] pathComponents = path.getPath();
		final File scanRoot = MindMapDataRootResolver.getPrimaryScanRoot();
		if (scanRoot == null) {
			return null;
		}
		StringBuilder folderPath = new StringBuilder(scanRoot.getAbsolutePath());
		
		for (int i = 1; i < pathComponents.length; i++) {
			Object component = pathComponents[i];
			if (component instanceof DefaultMutableTreeNode) {
				Object userObj = ((DefaultMutableTreeNode) component).getUserObject();
				if (userObj instanceof GroupLabel) {
					String part = ((GroupLabel) userObj).text;
					if (part.toLowerCase().endsWith(".mm")) {
						continue;
					}
					folderPath.append(File.separator).append(part);
					if (part.equals(label)) {
						File folder = new File(folderPath.toString());
						if (folder.exists() && folder.isDirectory()) {
							return folder.getAbsolutePath();
						}
					}
				}
			}
		}
		
		return null;
	}

	private void openSelectedItem() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return;
		}
		Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(userObject instanceof ItemRecord)) {
			return;
		}

		ItemRecord record = (ItemRecord) userObject;
		try {
			IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			URL url = record.file.toURI().toURL();
			if (!mapViewManager.tryToChangeToMapView(url)) {
				Controller.getCurrentModeController().getMapController().newMap(url);
			}
			selectItemNodeWithRetry(record, 0);
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private void selectItemNodeWithRetry(final ItemRecord record, final int attempt) {
		final int maxAttempts = 12;
		if (record == null || attempt > maxAttempts) {
			return;
		}
		try {
			IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			java.util.Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (Object mapObj : maps.values()) {
				MapModel map = (MapModel) mapObj;
				File mapFile = map.getFile();
				if (mapFile != null && mapFile.equals(record.file)) {
					NodeModel node = map.getNodeForID(record.nodeId);
					if (node != null) {
						Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(node);
						Controller.getCurrentModeController().getMapController().centerNode(node);
						tree.requestFocusInWindow();
						return;
					}
				}
			}
		} catch (Exception e) {
			LogUtils.warn(e);
		}
		Timer retry = new Timer(250, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				selectItemNodeWithRetry(record, attempt + 1);
			}
		});
		retry.setRepeats(false);
		retry.start();
	}

	private void installArrowKeyNavigation() {
		tree.getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "item.right");
		tree.getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "item.left");

		tree.getActionMap().put("item.left", new AbstractAction() {
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

		tree.getActionMap().put("item.right", new AbstractAction() {
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

	private void mergeChunk(ScanChunk chunk) {
		List oldKeys = (List) itemKeysByFile.get(chunk.fileKey);
		if (oldKeys != null) {
			for (int i = 0; i < oldKeys.size(); i++) {
				itemsByKey.remove(oldKeys.get(i));
			}
		}

		List newKeys = new ArrayList();
		for (int i = 0; i < chunk.items.size(); i++) {
			ItemRecord record = (ItemRecord) chunk.items.get(i);
			String key = itemKey(record);
			if (!itemsByKey.containsKey(key)) {
				itemsByKey.put(key, record);
				newKeys.add(key);
			}
		}
		itemKeysByFile.put(chunk.fileKey, newKeys);
	}

	private List collectAllMindmapFiles() {
		final List files = new ArrayList();
		MindMapDataRootResolver.collectMindmapFiles(files);
		return files;
	}

	private List getItemsForFile(final File file) {
		long modified = file.lastModified();
		long length = file.length();
		CachedFileResult cached = (CachedFileResult) cacheByFile.get(file.getAbsolutePath());
		if (cached != null && cached.modified == modified && cached.length == length) {
			return cached.items;
		}

		final List items = new ArrayList();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setNamespaceAware(false);
			SAXParser saxParser = factory.newSAXParser();
			saxParser.parse(file, new DefaultHandler() {
				private final List nodeStack = new ArrayList();

				public void startElement(String uri, String localName, String qName, Attributes attributes) {
					if ("node".equals(qName)) {
						String id = attributes.getValue("ID");
						String text = attributes.getValue("TEXT");
						nodeStack.add(new String[] { id, text == null ? "" : text });
					} else if ("icon".equals(qName) && !nodeStack.isEmpty()) {
						String iconName = attributes.getValue("BUILTIN");
						if (iconName != null && getIconName().equalsIgnoreCase(iconName)) {
							String[] nodeInfo = (String[]) nodeStack.get(nodeStack.size() - 1);
							String nodeText = nodeInfo[1] == null ? "" : nodeInfo[1].trim();
							if (!"bin".equalsIgnoreCase(nodeText)) {
								items.add(new ItemRecord(file, nodeInfo[0], nodeText, file.getName()));
							}
						}
					}
				}

				public void endElement(String uri, String localName, String qName) {
					if ("node".equals(qName) && !nodeStack.isEmpty()) {
						nodeStack.remove(nodeStack.size() - 1);
					}
				}
			});
		} catch (Exception e) {
			LogUtils.warn(e);
		}

		cacheByFile.put(file.getAbsolutePath(), new CachedFileResult(modified, length, items));
		return items;
	}

	private void rebuildTreeFromCache() {
		String selectedKey = getSelectedItemKey();
		List records = new ArrayList(itemsByKey.values());

		Collections.sort(records, new Comparator() {
			public int compare(Object o1, Object o2) {
				ItemRecord a = (ItemRecord) o1;
				ItemRecord b = (ItemRecord) o2;
				String pathA = a.file.getParent() != null ? a.file.getParent() : "";
				String pathB = b.file.getParent() != null ? b.file.getParent() : "";
				int pathCompare = pathA.compareTo(pathB);
				if (pathCompare != 0) {
					return pathCompare;
				}
				int fileCompare = a.mapName.compareTo(b.mapName);
				if (fileCompare != 0) {
					return fileCompare;
				}
				return a.nodeText.compareTo(b.nodeText);
			}
		});

		Set<String> addedKeys = new HashSet();
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(getRootLabel());
		Map<String, DefaultMutableTreeNode> pathNodes = new HashMap();
		Map itemNodesByKey = new HashMap();

		for (int i = 0; i < records.size(); i++) {
			ItemRecord record = (ItemRecord) records.get(i);
			String recordKey = record.nodeText + "|" + record.mapName;

			if (addedKeys.contains(recordKey)) {
				continue;
			}
			addedKeys.add(recordKey);

			String parentPath = record.file.getParent();
			DefaultMutableTreeNode parentNode = root;

			if (parentPath != null) {
				final String relativePath = MindMapDataRootResolver.getRelativePathWithinScanRoots(record.file.getParentFile());
				if (relativePath != null && relativePath.length() > 0) {
					final String[] pathParts = relativePath.split("/");
					String currentPath = "";
					for (int p = 0; p < pathParts.length; p++) {
						final String part = pathParts[p];
						if (part == null || part.trim().length() == 0) {
							continue;
						}
						currentPath += "/" + part;
						DefaultMutableTreeNode pathNode = pathNodes.get(currentPath);
						if (pathNode == null) {
							pathNode = new DefaultMutableTreeNode(new GroupLabel(part), true);
							pathNodes.put(currentPath, pathNode);
							parentNode.add(pathNode);
						}
						parentNode = pathNode;
					}
				}
			}

			DefaultMutableTreeNode fileNode = pathNodes.get(record.file.getAbsolutePath());
			if (fileNode == null) {
				fileNode = new DefaultMutableTreeNode(new GroupLabel(record.mapName), true);
				pathNodes.put(record.file.getAbsolutePath(), fileNode);
				parentNode.add(fileNode);
			}

			DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(record, false);
			fileNode.add(itemNode);
			itemNodesByKey.put(itemKey(record), itemNode);
		}

		tree.setModel(new DefaultTreeModel(root));

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		if (selectedKey != null) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) itemNodesByKey.get(selectedKey);
			if (selectedNode != null) {
				TreePath selectedPath = new TreePath(selectedNode.getPath());
				tree.setSelectionPath(selectedPath);
				tree.scrollPathToVisible(selectedPath);
			}
		}
		onSideTabCacheRefreshed(records);
	}

	protected void onSideTabCacheRefreshed(List records) {
		if (!"\u5168\u90e8\u5f85\u529e".equals(getRootLabel())) {
			return;
		}
		List entries = new ArrayList();
		for (int i = 0; i < records.size(); i++) {
			ItemRecord record = (ItemRecord) records.get(i);
			String text = record.nodeText == null ? "" : HtmlUtils.removeHtmlTagsFromString(record.nodeText)
					.replaceAll("\\s+", " ").trim();
			entries.add(new WorkspaceSideTabSnapshot.TodoEntry(record.file, record.nodeId, text));
		}
		WorkspaceSideTabSnapshotRegistry.updateTodos(entries);
	}

	private String getSelectedItemKey() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return null;
		}
		Object nodeObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(nodeObj instanceof ItemRecord)) {
			return null;
		}
		return itemKey((ItemRecord) nodeObj);
	}

	private String itemKey(ItemRecord record) {
		return record.file.getAbsolutePath() + "|" + record.nodeId;
	}
}