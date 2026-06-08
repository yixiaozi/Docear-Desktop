package org.docear.plugin.core.todoist;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.freeplane.core.util.Compat;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.note.NoteController;
import org.freeplane.features.note.mindmapmode.MNoteController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.features.url.mindmapmode.MFileManager;
import org.freeplane.view.swing.features.time.mindmapmode.ReminderExtension;
import org.freeplane.view.swing.features.time.mindmapmode.ReminderHook;

final class TodoistMindMapWriter {
	private static final String TODOIST_BRANCH = "Todoist";
	private static final String NO_SECTION_KEY = "__no_section__";

	TodoistImportResult write(File targetFile, List tasks, Map projectNames, Map sectionNames) {
		final TodoistImportResult result = new TodoistImportResult();
		result.targetFile = targetFile.getAbsolutePath();
		try {
			final MapModel map = loadOrCreateMap(targetFile);
			if (map == null) {
				result.failed = 1;
				result.errorMessage = "Could not open mind map: " + targetFile.getAbsolutePath();
				result.addFailed(result.errorMessage);
				return result;
			}
			final NodeModel todoistRoot = ensureTodoistBranch(map);
			clearChildren(todoistRoot);
			final Map grouped = groupTasks(tasks);
			final List projectIds = new ArrayList(grouped.keySet());
			Collections.sort(projectIds, new ProjectNameComparator(projectNames));
			for (int p = 0; p < projectIds.size(); p++) {
				String projectId = (String) projectIds.get(p);
				String projectName = resolveName(projectNames, projectId, TextUtils.getText("todoist.import.unknown_project"));
				NodeModel projectNode = createNode(map, todoistRoot, projectName);
				final Map sectionMap = (Map) grouped.get(projectId);
				final List sectionIds = new ArrayList(sectionMap.keySet());
				Collections.sort(sectionIds, new SectionNameComparator(sectionNames));
				for (int s = 0; s < sectionIds.size(); s++) {
					String sectionId = (String) sectionIds.get(s);
					String sectionName = NO_SECTION_KEY.equals(sectionId) ? TextUtils.getText("todoist.import.no_section")
							: resolveName(sectionNames, sectionId, TextUtils.getText("todoist.import.unknown_section"));
					NodeModel sectionNode = createNode(map, projectNode, sectionName);
					final List sectionTasks = (List) sectionMap.get(sectionId);
					for (int t = 0; t < sectionTasks.size(); t++) {
						TodoistImportTask task = (TodoistImportTask) sectionTasks.get(t);
						createTaskNode(map, sectionNode, task);
						result.totalFetched++;
						result.addCreated("[" + projectName + " / " + sectionName + "] " + parsedLine(task));
					}
				}
			}
			saveMap(map, targetFile);
		}
		catch (Exception e) {
			result.failed++;
			result.errorMessage = e.getMessage();
			result.addFailed(e.getMessage());
			LogUtils.warn("Todoist import write failed", e);
		}
		return result;
	}

	private static Map groupTasks(List tasks) {
		final Map grouped = new HashMap();
		for (int i = 0; i < tasks.size(); i++) {
			TodoistImportTask task = (TodoistImportTask) tasks.get(i);
			String projectId = task.projectId == null ? "" : task.projectId;
			String sectionId = task.sectionId == null || task.sectionId.length() == 0 ? NO_SECTION_KEY : task.sectionId;
			Map sectionMap = (Map) grouped.get(projectId);
			if (sectionMap == null) {
				sectionMap = new HashMap();
				grouped.put(projectId, sectionMap);
			}
			List sectionTasks = (List) sectionMap.get(sectionId);
			if (sectionTasks == null) {
				sectionTasks = new ArrayList();
				sectionMap.put(sectionId, sectionTasks);
			}
			sectionTasks.add(task);
		}
		return grouped;
	}

	private static MapModel loadOrCreateMap(File targetFile) throws Exception {
		MapModel existing = findOpenMap(targetFile);
		if (existing != null) {
			return existing;
		}
		final ModeController modeController = Controller.getCurrentModeController();
		final MMapController mapController = (MMapController) modeController.getMapController();
		final URL url = Compat.fileToUrl(targetFile);
		if (targetFile.isFile()) {
			mapController.newMap(url);
			existing = findOpenMap(targetFile);
			if (existing != null) {
				return existing;
			}
		}
		final MapModel map = mapController.newMap();
		map.setURL(url);
		map.getRootNode().setText(targetFile.getName().endsWith(".mm")
				? targetFile.getName().substring(0, targetFile.getName().length() - 3)
				: targetFile.getName());
		return map;
	}

	private static MapModel findOpenMap(File targetFile) {
		final Map maps = Controller.getCurrentController().getMapViewManager().getMaps();
		for (Iterator it = maps.values().iterator(); it.hasNext();) {
			MapModel map = (MapModel) it.next();
			File file = map.getFile();
			if (file != null && file.getAbsolutePath().equalsIgnoreCase(targetFile.getAbsolutePath())) {
				return map;
			}
		}
		return null;
	}

	private static NodeModel ensureTodoistBranch(MapModel map) {
		NodeModel root = map.getRootNode();
		NodeModel branch = findChildByPlainText(root, TODOIST_BRANCH);
		if (branch == null) {
			branch = createNode(map, root, TODOIST_BRANCH);
		}
		return branch;
	}

	private static NodeModel findChildByPlainText(NodeModel parent, String plainText) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			NodeModel child = (NodeModel) parent.getChildAt(i);
			if (plainText.equals(nodePlainText(child))) {
				return child;
			}
		}
		return null;
	}

	private static void clearChildren(NodeModel parent) {
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		while (parent.getChildCount() > 0) {
			mapController.deleteNode((NodeModel) parent.getChildAt(0));
		}
	}

	private static NodeModel createNode(MapModel map, NodeModel parent, String text) {
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		final NodeModel node = new NodeModel(text, map);
		MTextController.getController().setNodeText(node, text);
		mapController.insertNode(node, parent, parent.getChildCount());
		return node;
	}

	private static void createTaskNode(MapModel map, NodeModel parent, TodoistImportTask task) {
		TodoistContentParser parsed = TodoistContentParser.parse(task.content);
		final NodeModel node = createNode(map, parent, parsed.nodeText);
		applyLink(node, parsed.linkUri);
		if (task.description != null && task.description.trim().length() > 0) {
			((MNoteController) NoteController.getController()).setNoteText(node, task.description);
		}
		if (task.dueAtMillis > 0) {
			final ModeController modeController = Controller.getCurrentModeController();
			final ReminderHook reminderHook = (ReminderHook) modeController.getExtension(ReminderHook.class);
			if (reminderHook != null) {
				final ReminderExtension reminder = new ReminderExtension(node);
				reminder.setRemindUserAt(task.dueAtMillis);
				reminder.setPeriod(1);
				reminder.setPeriodUnitAsString(task.recurring ? "WEEK" : "DAY");
				reminderHook.undoableActivateHook(node, reminder);
			}
		}
	}

	private static void applyLink(NodeModel node, String linkUri) {
		if (linkUri == null || linkUri.trim().length() == 0) {
			return;
		}
		try {
			final ModeController modeController = Controller.getCurrentModeController();
			final URI uri = new URI(linkUri.trim());
			((MLinkController) LinkController.getController(modeController)).setLink(node, uri,
					LinkController.LINK_ABSOLUTE);
		}
		catch (Exception e) {
			LogUtils.warn("Todoist import: could not set link " + linkUri, e);
		}
	}

	private static String parsedLine(TodoistImportTask task) {
		TodoistContentParser parsed = TodoistContentParser.parse(task.content);
		if (parsed.linkUri != null) {
			return parsed.nodeText + " -> " + parsed.linkUri;
		}
		return parsed.nodeText;
	}

	private static void saveMap(MapModel map, File targetFile) throws Exception {
		final ModeController modeController = Controller.getCurrentModeController();
		final MFileManager fileManager = MFileManager.getController(modeController);
		File parent = targetFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		((MapModel) map).setURL(Compat.fileToUrl(targetFile));
		map.setSaved(false);
		if (!fileManager.save(map, targetFile)) {
			throw new Exception("Could not save " + targetFile.getAbsolutePath());
		}
	}

	private static String nodePlainText(NodeModel node) {
		String text = node.getText();
		if (text == null) {
			return "";
		}
		return HtmlUtils.htmlToPlain(text).trim();
	}

	private static String resolveName(Map names, String id, String fallback) {
		if (id == null || id.length() == 0) {
			return fallback;
		}
		String name = (String) names.get(id);
		return name != null && name.length() > 0 ? name : fallback + " (" + id + ")";
	}

	private static final class ProjectNameComparator implements Comparator {
		private final Map projectNames;

		private ProjectNameComparator(Map projectNames) {
			this.projectNames = projectNames;
		}

		public int compare(Object a, Object b) {
			String nameA = resolveName(projectNames, (String) a, "");
			String nameB = resolveName(projectNames, (String) b, "");
			return nameA.compareToIgnoreCase(nameB);
		}
	}

	private static final class SectionNameComparator implements Comparator {
		private final Map sectionNames;

		private SectionNameComparator(Map sectionNames) {
			this.sectionNames = sectionNames;
		}

		public int compare(Object a, Object b) {
			if (NO_SECTION_KEY.equals(a)) {
				return 1;
			}
			if (NO_SECTION_KEY.equals(b)) {
				return -1;
			}
			String nameA = resolveName(sectionNames, (String) a, "");
			String nameB = resolveName(sectionNames, (String) b, "");
			return nameA.compareToIgnoreCase(nameB);
		}
	}
}
