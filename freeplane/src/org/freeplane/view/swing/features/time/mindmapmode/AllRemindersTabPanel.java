package org.freeplane.view.swing.features.time.mindmapmode;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

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

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class AllRemindersTabPanel extends JPanel {
	private static final long serialVersionUID = 1L;

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

	private static final class DayGroup {
		private final String day;

		private DayGroup(String day) {
			this.day = day;
		}
	}

	private final JButton refreshButton = new JButton("\u5237\u65b0");
	private final JLabel statusLabel = new JLabel("\u626b\u63cf\u51c6\u5907\u4e2d");
	private final JTree tree = new JTree(new DefaultMutableTreeNode("\u5168\u90e8\u63d0\u9192"));
	private final DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd EEEE", Locale.CHINA);
	private final DateFormat dateTimeFormat = DateFormat.getDateTimeInstance();
	private final Map cacheByFile = new HashMap();
	private SwingWorker activeWorker;

	public AllRemindersTabPanel() {
		super(new BorderLayout(4, 4));
		JPanel top = new JPanel(new BorderLayout(4, 0));
		top.add(statusLabel, BorderLayout.CENTER);
		top.add(refreshButton, BorderLayout.EAST);
		add(top, BorderLayout.NORTH);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			private static final long serialVersionUID = 1L;
			public Component getTreeCellRendererComponent(JTree pTree, Object value, boolean sel, boolean expanded,
				boolean leaf, int row, boolean hasFocus) {
				super.getTreeCellRendererComponent(pTree, value, sel, expanded, leaf, row, hasFocus);
				Object user = ((DefaultMutableTreeNode) value).getUserObject();
				if (user instanceof DayGroup) {
					setText(((DayGroup) user).day);
				}
				else if (user instanceof ReminderRecord) {
					ReminderRecord record = (ReminderRecord) user;
					String text = record.nodeText == null ? "" : HtmlUtils.removeHtmlTagsFromString(record.nodeText).replaceAll("\\s+", " ");
					setText(dateTimeFormat.format(new Date(record.remindAt)) + "  -  " + text + "  (" + record.file.getName() + ")");
				}
				return this;
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() >= 1) {
					openSelectedReminder();
				}
			}
		});
		add(new JScrollPane(tree), BorderLayout.CENTER);

		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				refreshInBackground();
			}
		});
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
			activeWorker.cancel(true);
		}
		statusLabel.setText("\u6b63\u5728\u540e\u53f0\u626b\u63cf\u5168\u90e8\u5bfc\u56fe\u63d0\u9192...");
		activeWorker = new SwingWorker() {
			protected Object doInBackground() throws Exception {
				return scanAllReminders();
			}
			protected void done() {
				try {
					List records = (List) get();
					rebuildTree(records);
					statusLabel.setText("\u63d0\u9192\u603b\u6570: " + records.size());
				}
				catch (Exception e) {
					statusLabel.setText("\u626b\u63cf\u5931\u8d25");
					LogUtils.warn(e);
				}
			}
		};
		activeWorker.execute();
	}

	private List scanAllReminders() {
		List all = new ArrayList();
		List files = collectAllMindmapFiles();
		Set seenReminderKeys = new HashSet();
		for (int i = 0; i < files.size(); i++) {
			File file = (File) files.get(i);
			List fileReminders = getRemindersForFile(file);
			for (int j = 0; j < fileReminders.size(); j++) {
				ReminderRecord record = (ReminderRecord) fileReminders.get(j);
				String key = record.file.getAbsolutePath() + "|" + record.nodeId + "|" + record.remindAt;
				if (seenReminderKeys.add(key)) {
					all.add(record);
				}
			}
		}
		Collections.sort(all, new Comparator() {
			public int compare(Object o1, Object o2) {
				ReminderRecord a = (ReminderRecord) o1;
				ReminderRecord b = (ReminderRecord) o2;
				return a.remindAt < b.remindAt ? -1 : (a.remindAt == b.remindAt ? 0 : 1);
			}
		});
		return all;
	}

	private List collectAllMindmapFiles() {
		Set roots = new HashSet();
		IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
		Map maps = mapViewManager.getMaps(MModeController.MODENAME);
		for (Object mapObj : maps.values()) {
			MapModel map = (MapModel) mapObj;
			File file = map.getFile();
			if (file != null && file.getParentFile() != null && file.getParentFile().exists()) {
				roots.add(file.getParentFile());
			}
		}
		File currentFile = Controller.getCurrentController().getMap() != null ? Controller.getCurrentController().getMap().getFile() : null;
		if (currentFile != null && currentFile.getParentFile() != null) {
			roots.add(currentFile.getParentFile());
		}
		List normalizedRoots = normalizeRoots(roots);
		List files = new ArrayList();
		Set seenFiles = new HashSet();
		for (Object rootObj : normalizedRoots) {
			collectMindmapFilesRecursive((File) rootObj, files);
		}
		for (int i = files.size() - 1; i >= 0; i--) {
			File f = (File) files.get(i);
			String key = f.getAbsolutePath();
			if (!seenFiles.add(key)) {
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
				if (!file.isHidden()) {
					collectMindmapFilesRecursive(file, out);
				}
			}
			else {
				String lower = file.getName().toLowerCase();
				if (lower.endsWith(".mm") && !file.getName().startsWith("~")) {
					out.add(file);
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
						nodeStack.add(new String[] { id, text == null ? "" : text });
					}
					else if ("Parameters".equals(qName) && !nodeStack.isEmpty()) {
						String remindAt = attributes.getValue("REMINDUSERAT");
						if (remindAt != null) {
							try {
								long remindTs = Long.parseLong(remindAt);
								if (remindTs > 0) {
									String[] nodeInfo = (String[]) nodeStack.get(nodeStack.size() - 1);
									String nodeText = nodeInfo[1] == null ? "" : nodeInfo[1].trim();
									if (!"bin".equalsIgnoreCase(nodeText)) {
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

	private void rebuildTree(List records) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("\u5168\u90e8\u63d0\u9192");
		Map dayNodes = new HashMap();
		for (int i = 0; i < records.size(); i++) {
			ReminderRecord record = (ReminderRecord) records.get(i);
			String day = dayFormat.format(new Date(record.remindAt));
			DefaultMutableTreeNode dayNode = (DefaultMutableTreeNode) dayNodes.get(day);
			if (dayNode == null) {
				dayNode = new DefaultMutableTreeNode(new DayGroup(day));
				dayNodes.put(day, dayNode);
				root.add(dayNode);
			}
			dayNode.add(new DefaultMutableTreeNode(record, false));
		}
		tree.setModel(new DefaultTreeModel(root));
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
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
				Controller.getCurrentController().getViewController().openDocument(url);
				mapViewManager.tryToChangeToMapView(url);
			}
			Map maps = mapViewManager.getMaps(MModeController.MODENAME);
			for (Object mapObj : maps.values()) {
				MapModel map = (MapModel) mapObj;
				File mapFile = map.getFile();
				if (mapFile != null && mapFile.equals(record.file)) {
					NodeModel node = map.getNodeForID(record.nodeId);
					if (node != null) {
						Controller.getCurrentController().getSelection().selectAsTheOnlyOneSelected(node);
						Controller.getCurrentModeController().getMapController().centerNode(node);
					}
					break;
				}
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
	}
}
