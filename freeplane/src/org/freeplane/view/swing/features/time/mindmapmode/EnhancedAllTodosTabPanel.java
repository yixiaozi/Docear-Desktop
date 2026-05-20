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
import org.freeplane.features.map.IMapSelectionListener;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class EnhancedAllTodosTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String HARD_CODED_SCAN_ROOT = "E:\\yixiaozi";
	private static final String TODO_ICON_NAME = "hourglass";

	private static final class TodoRecord {
		private final File file;
		private final String nodeId;
		private final String nodeText;
		private final String mapName;

		private TodoRecord(File file, String nodeId, String nodeText, String mapName) {
			this.file = file;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.mapName = mapName;
		}
	}

	private static final class CachedFileResult {
		private final long modified;
		private final long length;
		private final List todos;

		private CachedFileResult(long modified, long length, List todos) {
			this.modified = modified;
			this.length = length;
			this.todos = todos;
		}
	}

	private static final class GroupLabel {
		private final String text;

		private GroupLabel(String text) {
			this.text = text;
		}
	}

	private static final class ScanChunk {
		private final String fileKey;
		private final List todos;
		private final int scanned;
		private final int total;

		private ScanChunk(String fileKey, List todos, int scanned, int total) {
			this.fileKey = fileKey;
			this.todos = todos;
			this.scanned = scanned;
			this.total = total;
		}
	}

	private final JButton refreshButton = new JButton("\u5237\u65b0");
	private final JLabel statusLabel = new JLabel("\u5f85\u529e\u603b\u6570: 0");
	private final JTree tree = new JTree(new DefaultMutableTreeNode("\u5168\u90e8\u5f85\u529e"));
	private final DecimalFormat twoDigits = new DecimalFormat("00");
	private final Map cacheByFile = new HashMap();
	private final Map todosByKey = new HashMap();
	private final Map todoKeysByFile = new HashMap();
	private SwingWorker activeWorker;
	private boolean rescanRequested;
	private final Timer autoRefreshTimer;

	public EnhancedAllTodosTabPanel() {
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
				if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
					showContextMenu(e);
					return;
				}
				if (e.getClickCount() >= 1) {
					openSelectedTodo();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e);
				}
			}
		});

		add(new JScrollPane(tree), BorderLayout.CENTER);

		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				refreshInBackground();
			}
		});

		autoRefreshTimer = new Timer(12000, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				refreshInBackground();
			}
		});
		autoRefreshTimer.setDelay(300000);
		autoRefreshTimer.setRepeats(true);
		autoRefreshTimer.start();

		addListeners();
		refreshInBackground();
	}

	private void addListeners() {
		Controller.getCurrentController().getMapViewManager().addMapSelectionListener(new IMapSelectionListener() {
			public void beforeMapChange(MapModel oldMap, MapModel newMap) {
			}

			public void afterMapChange(MapModel oldMap, MapModel newMap) {
				refreshInBackground();
			}
		});
	}

	private synchronized void refreshInBackground() {
		if (activeWorker != null && !activeWorker.isDone()) {
			rescanRequested = true;
			return;
		}

		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				List files = collectAllMindmapFiles();
				cleanupCache(files);
				for (int i = 0; i < files.size(); i++) {
					if (isCancelled()) {
						break;
					}
					File file = (File) files.get(i);
					List todos = getTodosForFile(file);
					publish(new ScanChunk(file.getAbsolutePath(), todos, i + 1, files.size()));
				}
				return null;
			}

			protected void process(List chunks) {
				for (int i = 0; i < chunks.size(); i++) {
					ScanChunk chunk = (ScanChunk) chunks.get(i);
					mergeChunk(chunk);
				}
				rebuildTreeFromCache();
			}

			protected void done() {
				statusLabel.setText("\u5f85\u529e\u603b\u6570: " + todosByKey.size());
				if (rescanRequested) {
					rescanRequested = false;
					refreshInBackground();
				}
			}
		};
		activeWorker.execute();
	}

	private void mergeChunk(ScanChunk chunk) {
		List oldKeys = (List) todoKeysByFile.get(chunk.fileKey);
		if (oldKeys != null) {
			for (int i = 0; i < oldKeys.size(); i++) {
				todosByKey.remove(oldKeys.get(i));
			}
		}

		List newKeys = new ArrayList();
		for (int i = 0; i < chunk.todos.size(); i++) {
			TodoRecord record = (TodoRecord) chunk.todos.get(i);
			String key = todoKey(record);
			if (!todosByKey.containsKey(key)) {
				todosByKey.put(key, record);
				newKeys.add(key);
			}
		}
		todoKeysByFile.put(chunk.fileKey, newKeys);
	}

	private List collectAllMindmapFiles() {
		Set roots = new HashSet();
		File fixedRoot = new File(HARD_CODED_SCAN_ROOT);
		if (fixedRoot.exists() && fixedRoot.isDirectory()) {
			roots.add(fixedRoot);
		}

		List normalizedRoots = normalizeRoots(roots);
		List files = new ArrayList();
		Set seenFiles = new HashSet();

		for (Object rootObj : normalizedRoots) {
			collectMindmapFilesRecursive((File) rootObj, files);
		}

		for (int i = files.size() - 1; i >= 0; i--) {
			File f = (File) files.get(i);
			if (!seenFiles.add(f.getAbsolutePath())) {
				files.remove(i);
			}
		}

		return files;
	}

	private List normalizeRoots(Set rawRoots) {
		List roots = new ArrayList(rawRoots);
		Collections.sort(roots, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((File) o1).getAbsolutePath().length() - ((File) o2).getAbsolutePath().length();
			}
		});

		List normalized = new ArrayList();
		for (int i = 0; i < roots.size(); i++) {
			File candidate = (File) roots.get(i);
			boolean covered = false;
			for (int j = 0; j < normalized.size(); j++) {
				File existing = (File) normalized.get(j);
				String existingPath = existing.getAbsolutePath();
				String candidatePath = candidate.getAbsolutePath();
				if (candidatePath.equals(existingPath) || candidatePath.startsWith(existingPath + File.separator)) {
					covered = true;
					break;
				}
			}
			if (!covered) {
				normalized.add(candidate);
			}
		}
		return normalized;
	}

	private void cleanupCache(List currentFiles) {
		Set currentPaths = new HashSet();
		for (int i = 0; i < currentFiles.size(); i++) {
			File file = (File) currentFiles.get(i);
			currentPaths.add(file.getAbsolutePath());
		}

		List toRemove = new ArrayList();
		for (Object key : cacheByFile.keySet()) {
			if (!currentPaths.contains(key)) {
				toRemove.add(key);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			cacheByFile.remove(toRemove.get(i));
		}

		toRemove.clear();
		for (Object key : todoKeysByFile.keySet()) {
			if (!currentPaths.contains(key)) {
				toRemove.add(key);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			String fileKey = (String) toRemove.get(i);
			List oldKeys = (List) todoKeysByFile.get(fileKey);
			if (oldKeys != null) {
				for (int j = 0; j < oldKeys.size(); j++) {
					todosByKey.remove(oldKeys.get(j));
				}
			}
			todoKeysByFile.remove(fileKey);
		}
	}

	private void collectMindmapFilesRecursive(File dir, List out) {
		if (dir == null || !dir.exists() || !dir.isDirectory()) {
			return;
		}

		File[] children = dir.listFiles();
		if (children == null) {
			return;
		}

		for (int i = 0; i < children.length; i++) {
			File file = children[i];
			if (file.isDirectory()) {
				if (!file.isHidden() && !file.getName().startsWith(".")) {
					collectMindmapFilesRecursive(file, out);
				}
			} else {
				String lower = file.getName().toLowerCase();
				if (lower.endsWith(".mm") && !file.getName().startsWith("~")
						&& !file.getName().contains("冲突副本")) {
					out.add(file);
				}
			}
		}
	}

	private List getTodosForFile(final File file) {
		long modified = file.lastModified();
		long length = file.length();
		CachedFileResult cached = (CachedFileResult) cacheByFile.get(file.getAbsolutePath());
		if (cached != null && cached.modified == modified && cached.length == length) {
			return cached.todos;
		}

		final List todos = new ArrayList();
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
						if (iconName != null && TODO_ICON_NAME.equalsIgnoreCase(iconName)) {
							String[] nodeInfo = (String[]) nodeStack.get(nodeStack.size() - 1);
							String nodeText = nodeInfo[1] == null ? "" : nodeInfo[1].trim();
							if (!"bin".equalsIgnoreCase(nodeText)) {
								todos.add(new TodoRecord(file, nodeInfo[0], nodeText, file.getName()));
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

		cacheByFile.put(file.getAbsolutePath(), new CachedFileResult(modified, length, todos));
		return todos;
	}

	private void rebuildTreeFromCache() {
		String selectedKey = getSelectedTodoKey();
		List records = new ArrayList(todosByKey.values());

		Collections.sort(records, new Comparator() {
			public int compare(Object o1, Object o2) {
				TodoRecord a = (TodoRecord) o1;
				TodoRecord b = (TodoRecord) o2;
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
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u5168\u90e8\u5f85\u529e");
		Map<String, DefaultMutableTreeNode> pathNodes = new HashMap();
		Map todoNodesByKey = new HashMap();

		for (int i = 0; i < records.size(); i++) {
			TodoRecord record = (TodoRecord) records.get(i);
			String recordKey = record.nodeText + "|" + record.mapName;
			
			if (addedKeys.contains(recordKey)) {
				continue;
			}
			addedKeys.add(recordKey);

			String parentPath = record.file.getParent();
			DefaultMutableTreeNode parentNode = root;

			if (parentPath != null) {
				String normalizedPath = parentPath.replace("\\", "/");
				String[] pathParts = normalizedPath.split("/");
				String currentPath = "";
				boolean passedYixiaozi = false;

				for (String part : pathParts) {
					if (part.trim().isEmpty()) {
						continue;
					}
					
					if (!passedYixiaozi) {
						if ("yixiaozi".equalsIgnoreCase(part)) {
							passedYixiaozi = true;
						}
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

			DefaultMutableTreeNode fileNode = pathNodes.get(record.file.getAbsolutePath());
			if (fileNode == null) {
				fileNode = new DefaultMutableTreeNode(new GroupLabel(record.mapName), true);
				pathNodes.put(record.file.getAbsolutePath(), fileNode);
				parentNode.add(fileNode);
			}

			DefaultMutableTreeNode todoNode = new DefaultMutableTreeNode(record, false);
			fileNode.add(todoNode);
			todoNodesByKey.put(todoKey(record), todoNode);
		}

		tree.setModel(new DefaultTreeModel(root));

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		if (selectedKey != null) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) todoNodesByKey.get(selectedKey);
			if (selectedNode != null) {
				TreePath selectedPath = new TreePath(selectedNode.getPath());
				tree.setSelectionPath(selectedPath);
				tree.scrollPathToVisible(selectedPath);
			}
		}
	}

	private String getSelectedTodoKey() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return null;
		}
		Object nodeObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(nodeObj instanceof TodoRecord)) {
			return null;
		}
		return todoKey((TodoRecord) nodeObj);
	}

	private String todoKey(TodoRecord record) {
		if (record == null || record.nodeId == null) {
			return "";
		}
		return canonicalPath(record.file) + "|" + record.nodeId;
	}

	private void showContextMenu(MouseEvent e) {
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path == null) {
			return;
		}

		tree.setSelectionPath(path);
		final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
		final Object user = selectedNode.getUserObject();

		JPopupMenu menu = new JPopupMenu();

		if (user instanceof TodoRecord) {
			final TodoRecord record = (TodoRecord) user;

			JMenuItem openFolderItem = new JMenuItem("\u6253\u5f00\u6587\u4ef6\u5939");
			openFolderItem.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent event) {
					openContainingFolder(record.file);
				}
			});
			menu.add(openFolderItem);
		}

		JMenuItem copyItem = new JMenuItem("\u590d\u5236");
		copyItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent event) {
				copyNodeContent(selectedNode, user);
			}
		});
		menu.add(copyItem);

		menu.show(tree, e.getX(), e.getY());
	}

	private void openContainingFolder(File file) {
		if (file == null || !file.exists()) {
			return;
		}
		File parentDir = file.getParentFile();
		if (parentDir == null || !parentDir.isDirectory()) {
			return;
		}
		try {
			if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
				Runtime.getRuntime().exec("explorer.exe \"" + parentDir.getAbsolutePath() + "\"");
			} else if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
				Runtime.getRuntime().exec(new String[] { "open", parentDir.getAbsolutePath() });
			} else {
				Runtime.getRuntime().exec(new String[] { "xdg-open", parentDir.getAbsolutePath() });
			}
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private void copyNodeContent(DefaultMutableTreeNode selectedNode, Object user) {
		String textToCopy = "";
		if (user instanceof TodoRecord) {
			TodoRecord record = (TodoRecord) user;
			textToCopy = normalizeTaskText(record.nodeText);
		} else {
			List lines = new ArrayList();
			collectTodoLines(selectedNode, lines, "");
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < lines.size(); i++) {
				if (i > 0) {
					sb.append('\n');
				}
				sb.append(((String) lines.get(i)).trim());
			}
			textToCopy = sb.toString();
		}

		if (textToCopy != null && textToCopy.length() > 0) {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textToCopy.trim()), null);
		}
	}

	private void collectTodoLines(DefaultMutableTreeNode node, List out, String prefix) {
		Object user = node.getUserObject();
		if (user instanceof TodoRecord) {
			TodoRecord record = (TodoRecord) user;
			String leaf = normalizeTaskText(record.nodeText) + " (" + record.mapName + ")";
			String line = prefix.length() == 0 ? leaf : (prefix + " > " + leaf);
			out.add(line.trim());
			return;
		}

		String nextPrefix = prefix;
		if (user instanceof GroupLabel) {
			String groupText = ((GroupLabel) user).text == null ? "" : ((GroupLabel) user).text.trim();
			if (groupText.length() > 0) {
				nextPrefix = prefix.length() == 0 ? groupText : (prefix + " > " + groupText);
			}
		}

		for (int i = 0; i < node.getChildCount(); i++) {
			collectTodoLines((DefaultMutableTreeNode) node.getChildAt(i), out, nextPrefix);
		}
	}

	private String normalizeTaskText(String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
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
		try {
			IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			URL url = record.file.toURI().toURL();
			if (!mapViewManager.tryToChangeToMapView(url)) {
				Controller.getCurrentModeController().getMapController().newMap(url);
			}
			selectTodoNodeWithRetry(record, 0);
		} catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private void selectTodoNodeWithRetry(final TodoRecord record, final int attempt) {
		final int maxAttempts = 12;
		if (record == null || attempt > maxAttempts) {
			return;
		}

		try {
			IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (Object mapObj : maps.values()) {
				MapModel map = (MapModel) mapObj;
				File mapFile = map.getFile();
				if (isSameFile(mapFile, record.file)) {
					Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(map.getNodeForID(record.nodeId));
					Controller.getCurrentModeController().getMapController().centerNode(map.getNodeForID(record.nodeId));
					tree.requestFocusInWindow();
					return;
				}
			}
		} catch (Exception e) {
			LogUtils.warn(e);
		}

		Timer retry = new Timer(250, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				selectTodoNodeWithRetry(record, attempt + 1);
			}
		});
		retry.setRepeats(false);
		retry.start();
	}

	private boolean isSameFile(File a, File b) {
		if (a == null || b == null) {
			return false;
		}
		return canonicalPath(a).equals(canonicalPath(b));
	}

	private String canonicalPath(File file) {
		if (file == null) {
			return "";
		}
		try {
			return file.getCanonicalPath();
		} catch (Exception e) {
			return file.getAbsolutePath();
		}
	}

	private void installArrowKeyNavigation() {
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0),
				"all.todos.up");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0),
				"all.todos.down");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0),
				"all.todos.left");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0),
				"all.todos.right");

		tree.getActionMap().put("all.todos.up", new AbstractAction() {
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

		tree.getActionMap().put("all.todos.down", new AbstractAction() {
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

		tree.getActionMap().put("all.todos.left", new AbstractAction() {
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

		tree.getActionMap().put("all.todos.right", new AbstractAction() {
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
}