package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.features.ui.IMapViewManager;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class EnhancedAllRecurringRemindersTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String HARD_CODED_SCAN_ROOT = "E:\\yixiaozi";

	private static final class ReminderRecord {
		private final File file;
		private final String nodeId;
		private final String nodeText;
		private final long remindAt;

		private ReminderRecord(File file, String nodeId, String nodeText, long remindAt) {
			this.file = file;
			this.nodeId = nodeId;
			this.nodeText = nodeText;
			this.remindAt = remindAt;
		}
	}

	private static final class CachedFileResult {
		private final long modified;
		private final long length;
		private final List reminders;

		private CachedFileResult(long modified, long length, List reminders) {
			this.modified = modified;
			this.length = length;
			this.reminders = reminders;
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
		private final List reminders;
		private final int scanned;
		private final int total;

		private ScanChunk(String fileKey, List reminders, int scanned, int total) {
			this.fileKey = fileKey;
			this.reminders = reminders;
			this.scanned = scanned;
			this.total = total;
		}
	}

	private final JButton refreshButton = new JButton("\u5237\u65b0");
	private final JLabel statusLabel = new JLabel("\u5468\u671f\u63d0\u9192\u603b\u6570: 0");
	private final JTree tree = new JTree(new DefaultMutableTreeNode("\u5468\u671f\u63d0\u9192"));
	private final DateFormat dayFormat = new SimpleDateFormat("ddE", Locale.CHINA);
	private final DateFormat monthFormat = new SimpleDateFormat("MM", Locale.CHINA);
	private final DateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.CHINA);
	private final DateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.CHINA);
	private final DecimalFormat twoDigits = new DecimalFormat("00");
	private final Map cacheByFile = new HashMap();
	private final Map remindersByKey = new HashMap();
	private final Map reminderKeysByFile = new HashMap();
	private SwingWorker activeWorker;
	private boolean rescanRequested;
	private final Timer autoRefreshTimer;

	public EnhancedAllRecurringRemindersTabPanel() {
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
			public Component getTreeCellRendererComponent(JTree pTree, Object value, boolean sel, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(pTree, value, sel, expanded, leaf, row, hasFocus);
				setOpenIcon(null);
				setClosedIcon(null);
				setLeafIcon(null);
				Object user = ((DefaultMutableTreeNode) value).getUserObject();
				if (user instanceof GroupLabel) {
					setText(((GroupLabel) user).text);
				}
				else if (user instanceof ReminderRecord) {
					ReminderRecord record = (ReminderRecord) user;
					String text = record.nodeText == null ? "" : HtmlUtils.removeHtmlTagsFromString(record.nodeText).replaceAll("\\s+", " ").trim();
					setText(timeFormat.format(new Date(record.remindAt)) + " " + text.trim() + " (" + record.file.getName() + ")");
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
					openSelectedReminder();
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
			}
		});
	}

	private synchronized void refreshInBackground() {
		if (activeWorker != null && !activeWorker.isDone()) {
			rescanRequested = true;
			return;
		}
		rescanRequested = false;
		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				List files = collectAllMindmapFiles();
				cleanupCache(files);
				for (int i = 0; i < files.size(); i++) {
					if (isCancelled()) {
						break;
					}
					File file = (File) files.get(i);
					List reminders = getRemindersForFile(file);
					publish(new ScanChunk(file.getAbsolutePath(), reminders, i + 1, files.size()));
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
				statusLabel.setText("\u5468\u671f\u63d0\u9192\u603b\u6570: " + remindersByKey.size());
				if (rescanRequested) {
					rescanRequested = false;
					refreshInBackground();
				}
			}
		};
		activeWorker.execute();
	}

	private void mergeChunk(ScanChunk chunk) {
		List oldKeys = (List) reminderKeysByFile.get(chunk.fileKey);
		if (oldKeys != null) {
			for (int i = 0; i < oldKeys.size(); i++) {
				remindersByKey.remove(oldKeys.get(i));
			}
		}
		List newKeys = new ArrayList();
		for (int i = 0; i < chunk.reminders.size(); i++) {
			ReminderRecord record = (ReminderRecord) chunk.reminders.get(i);
			String key = reminderKey(record);
			if (!remindersByKey.containsKey(key)) {
				remindersByKey.put(key, record);
				newKeys.add(key);
			}
		}
		reminderKeysByFile.put(chunk.fileKey, newKeys);
	}

	private List collectAllMindmapFiles() {
		Set roots = new HashSet();
		File fixedRoot = new File(HARD_CODED_SCAN_ROOT);
		if (fixedRoot.exists() && fixedRoot.isDirectory()) {
			roots.add(fixedRoot);
		}
		List normalizedRoots = normalizeRoots(roots);
		List files = new ArrayList();
		for (Object root : normalizedRoots) {
			collectMindmapFiles((File) root, files);
		}
		return files;
	}

	private List normalizeRoots(Set roots) {
		List normalizedRoots = new ArrayList();
		for (Object root : roots) {
			File file = (File) root;
			if (file.exists()) {
				try {
					normalizedRoots.add(file.getCanonicalFile());
				}
				catch (Exception e) {
					normalizedRoots.add(file.getAbsoluteFile());
				}
			}
		}
		return normalizedRoots;
	}

	private void collectMindmapFiles(File directory, List files) {
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}
		File[] children = directory.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.getName().startsWith(".")) {
				continue;
			}
			if (child.isDirectory()) {
				collectMindmapFiles(child, files);
			}
			else if (child.getName().toLowerCase().endsWith(".mm")) {
				files.add(child);
			}
		}
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
		for (Object key : reminderKeysByFile.keySet()) {
			if (!currentPaths.contains(key)) {
				toRemove.add(key);
			}
		}
		for (int i = 0; i < toRemove.size(); i++) {
			String key = (String) toRemove.get(i);
			List keys = (List) reminderKeysByFile.remove(key);
			if (keys != null) {
				for (int j = 0; j < keys.size(); j++) {
					remindersByKey.remove(keys.get(j));
				}
			}
		}
	}

	private List getRemindersForFile(final File file) {
		long modified = file.lastModified();
		long length = file.length();
		CachedFileResult cached = (CachedFileResult) cacheByFile.get(file.getAbsolutePath());
		if (cached != null && cached.modified == modified && cached.length == length) {
			return cached.reminders;
		}
		final List reminders = new ArrayList();
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
						String remindType = attributes.getValue("REMINDERTYPE");
						nodeStack.add(new String[] { id, text == null ? "" : text, remindType });
					}
					else if ("Parameters".equals(qName) && !nodeStack.isEmpty()) {
						String remindAt = attributes.getValue("REMINDUSERAT");
						if (remindAt != null) {
							try {
								long remindTs = Long.parseLong(remindAt);
								if (remindTs > 0) {
									String[] nodeInfo = (String[]) nodeStack.get(nodeStack.size() - 1);
									String nodeText = nodeInfo[1] == null ? "" : nodeInfo[1].trim();
									String remindType = nodeInfo.length > 2 ? nodeInfo[2] : null;
									boolean isRecurring = (remindType != null && !"onetime".equalsIgnoreCase(remindType));
									if (!"bin".equalsIgnoreCase(nodeText) && isRecurring) {
										reminders.add(new ReminderRecord(file, nodeInfo[0], nodeText, remindTs));
									}
								}
							}
							catch (Exception e) {
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
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
		cacheByFile.put(file.getAbsolutePath(), new CachedFileResult(modified, length, reminders));
		return reminders;
	}

	private void rebuildTreeFromCache() {
		String selectedKey = getSelectedReminderKey();
		List records = new ArrayList(remindersByKey.values());
		Collections.sort(records, new Comparator() {
			public int compare(Object o1, Object o2) {
				ReminderRecord a = (ReminderRecord) o1;
				ReminderRecord b = (ReminderRecord) o2;
				return Long.compare(a.remindAt, b.remindAt);
			}
		});

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u5468\u671f\u63d0\u9192");
		Map dateNodes = new HashMap();
		Map reminderNodesByKey = new HashMap();

		Calendar cal = Calendar.getInstance();
		long now = cal.getTimeInMillis();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long today = cal.getTimeInMillis();

		for (int i = 0; i < records.size(); i++) {
			ReminderRecord record = (ReminderRecord) records.get(i);

			long remindAt = record.remindAt;
			String groupLabel;

			if (remindAt < today) {
				groupLabel = "\u8fc7\u671f";
			}
			else if (remindAt < today + 24 * 60 * 60 * 1000) {
				groupLabel = "\u4eca\u5929";
			}
			else if (remindAt < today + 2 * 24 * 60 * 60 * 1000) {
				groupLabel = "\u660e\u5929";
			}
			else {
				cal.setTimeInMillis(remindAt);
				groupLabel = monthFormat.format(cal.getTime()) + "\u6708" + dayFormat.format(cal.getTime());
			}

			DefaultMutableTreeNode dateNode = (DefaultMutableTreeNode) dateNodes.get(groupLabel);
			if (dateNode == null) {
				dateNode = new DefaultMutableTreeNode(new GroupLabel(groupLabel), true);
				dateNodes.put(groupLabel, dateNode);
				root.add(dateNode);
			}

			DefaultMutableTreeNode reminderNode = new DefaultMutableTreeNode(record, false);
			dateNode.add(reminderNode);
			reminderNodesByKey.put(reminderKey(record), reminderNode);
		}

		tree.setModel(new DefaultTreeModel(root));

		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}

		if (selectedKey != null) {
			DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) reminderNodesByKey.get(selectedKey);
			if (selectedNode != null) {
				TreePath selectedPath = new TreePath(selectedNode.getPath());
				tree.setSelectionPath(selectedPath);
				tree.scrollPathToVisible(selectedPath);
			}
		}
	}

	private String getSelectedReminderKey() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return null;
		}
		Object nodeObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(nodeObj instanceof ReminderRecord)) {
			return null;
		}
		return reminderKey((ReminderRecord) nodeObj);
	}

	private String reminderKey(ReminderRecord record) {
		if (record == null || record.nodeId == null) {
			return "";
		}
		return canonicalPath(record.file) + "|" + record.nodeId;
	}

	private String canonicalPath(File file) {
		if (file == null) {
			return "";
		}
		try {
			return file.getCanonicalPath();
		}
		catch (Exception e) {
			return file.getAbsolutePath();
		}
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

		if (user instanceof ReminderRecord) {
			final ReminderRecord record = (ReminderRecord) user;
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
		String text = "";
		if (user instanceof ReminderRecord) {
			text = ((ReminderRecord) user).nodeText;
		}
		text = normalizeTaskText(text);
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
	}

	private String normalizeTaskText(String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}

	private void openSelectedReminder() {
		TreePath path = tree.getSelectionPath();
		if (path == null) {
			return;
		}
		Object nodeObj = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
		if (!(nodeObj instanceof ReminderRecord)) {
			return;
		}
		ReminderRecord record = (ReminderRecord) nodeObj;
		try {
			IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
			URL url = record.file.toURI().toURL();
			if (!mapViewManager.tryToChangeToMapView(url)) {
				Controller.getCurrentModeController().getMapController().newMap(url);
			}
			selectReminderNodeWithRetry(record, 0);
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
	}

	private void selectReminderNodeWithRetry(final ReminderRecord record, final int attempt) {
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
					NodeModel node = map.getNodeForID(record.nodeId);
					if (node != null) {
						Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(node);
						Controller.getCurrentModeController().getMapController().centerNode(node);
						tree.requestFocusInWindow();
						return;
					}
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
		Timer retry = new Timer(250, new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				selectReminderNodeWithRetry(record, attempt + 1);
			}
		});
		retry.setRepeats(false);
		retry.start();
	}

	private boolean isSameFile(File file1, File file2) {
		if (file1 == null || file2 == null) {
			return file1 == file2;
		}
		try {
			return file1.getCanonicalPath().equals(file2.getCanonicalPath());
		}
		catch (Exception e) {
			return file1.getAbsolutePath().equals(file2.getAbsolutePath());
		}
	}

	private void installArrowKeyNavigation() {
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0), "all.recurring.up");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0), "all.recurring.down");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0), "all.recurring.left");
		tree.getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0), "all.recurring.right");
		tree.getActionMap().put("all.recurring.up", new AbstractAction() {
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
		tree.getActionMap().put("all.recurring.down", new AbstractAction() {
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
		tree.getActionMap().put("all.recurring.left", new AbstractAction() {
			private static final long serialVersionUID = 1L;
			public void actionPerformed(java.awt.event.ActionEvent e) {
				TreePath path = tree.getSelectionPath();
				if (path == null) {
					return;
				}
				if (tree.isExpanded(path)) {
					tree.collapsePath(path);
				}
				else if (path.getParentPath() != null) {
					tree.setSelectionPath(path.getParentPath());
				}
			}
		});
		tree.getActionMap().put("all.recurring.right", new AbstractAction() {
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