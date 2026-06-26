package org.freeplane.features.hashphotos.mindmapmode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.core.util.LogUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.mindmapmode.MModeController;

/**
 * 将 Hash Photos 导出的 CSV（dateKey,title）同步到「活动数据」节点下，结构为 年 → 月 → 日 → 活动标题。
 */
public final class HashPhotosEventsImporter {

	public static final String MAP_FILE_NAME = "Hash Photos.mm";
	public static final String ACTIVITIES_NODE_TEXT = "\u6d3b\u52a8\u6570\u636e";
	public static final String EXPORT_DIR_NAME = ".files" + File.separator + "Hash Photos";
	public static final String SYNC_STATE_FILE = ".hashphotos-sync.state";

	private static final Pattern DATE_KEY_PATTERN = Pattern.compile("^\\d{8}$");

	private HashPhotosEventsImporter() {
	}

	public static boolean isHashPhotosMap(final MapModel map) {
		if (map == null || map.getFile() == null) {
			return false;
		}
		return MAP_FILE_NAME.equalsIgnoreCase(map.getFile().getName());
	}

	public static File getExportDir(final File mapFile) {
		if (mapFile == null || mapFile.getParentFile() == null) {
			return null;
		}
		return new File(mapFile.getParentFile(), EXPORT_DIR_NAME);
	}

	public static File findLatestCsv(final File exportDir) {
		if (exportDir == null || !exportDir.isDirectory()) {
			return null;
		}
		final File[] files = exportDir.listFiles();
		if (files == null || files.length == 0) {
			return null;
		}
		File latest = null;
		long latestKey = Long.MIN_VALUE;
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (!file.isFile()) {
				continue;
			}
			String name = file.getName().toLowerCase();
			if (!name.endsWith(".csv") || SYNC_STATE_FILE.equals(file.getName())) {
				continue;
			}
			long key = file.lastModified();
			int dash = file.getName().lastIndexOf('-');
			if (dash >= 0) {
				String stamp = file.getName().substring(dash + 1, file.getName().length() - 4);
				long parsed = parseTimestamp(stamp);
				if (parsed > 0) {
					key = parsed;
				}
			}
			if (latest == null || key > latestKey) {
				latest = file;
				latestKey = key;
			}
		}
		return latest;
	}

	public static SyncResult syncMap(final MapModel map) {
		return syncMap(map, false);
	}

	public static SyncResult syncMapForce(final MapModel map) {
		return syncMap(map, true);
	}

	public static SyncResult syncMap(final MapModel map, final boolean force) {
		final SyncResult result = new SyncResult();
		if (!isHashPhotosMap(map)) {
			return result;
		}
		final File mapFile = map.getFile();
		final File exportDir = getExportDir(mapFile);
		final File csvFile = findLatestCsv(exportDir);
		if (csvFile == null) {
			return result;
		}
		if (!force && !needsSync(exportDir, csvFile)) {
			result.skipped = true;
			return result;
		}

		final NodeModel activitiesNode = findActivitiesNode(map.getRootNode());
		if (activitiesNode == null) {
			LogUtils.warn("Hash Photos sync: node '" + ACTIVITIES_NODE_TEXT + "' not found in " + mapFile.getAbsolutePath());
			return result;
		}

		final MModeController modeController = (MModeController) Controller.getCurrentController().getModeController(
		        MModeController.MODENAME);
		if (modeController == null) {
			return result;
		}

		final Map desired = readCsv(csvFile);
		if (desired.isEmpty()) {
			return result;
		}

		final MMapController mapController = (MMapController) modeController.getMapController();
		final Map existingByDateKey = indexExistingEvents(activitiesNode);
		final Map structureCache = new HashMap();
		final Map titleStore = HashPhotosTitleStore.load(exportDir);
		final Map newTitleStore = new HashMap();

		for (Object dateKeyObj : desired.keySet()) {
			final String dateKey = (String) dateKeyObj;
			final String title = (String) desired.get(dateKey);
			newTitleStore.put(dateKey, title);
			NodeModel existing = (NodeModel) existingByDateKey.get(dateKey);
			if (existing == null) {
				final NodeModel dayNode = ensureDayNode(mapController, map, activitiesNode, dateKey, structureCache);
				final NodeModel newNode = mapController.newNode(title, map);
				newNode.setLeft(false);
				mapController.insertNodeIntoWithoutUndo(newNode, dayNode, 0);
				existingByDateKey.put(dateKey, newNode);
				result.added++;
			}
			else {
				final String fullText = HashPhotosTitleMerge.normalize(existing.getText());
				final String syncedTitle = (String) titleStore.get(dateKey);
				final String merged = HashPhotosTitleMerge.mergeTitle(title, fullText, syncedTitle);
				if (HashPhotosTitleMerge.needsTextUpdate(title, fullText, syncedTitle)) {
					final Object oldText = existing.getUserObject();
					existing.setUserObject(merged);
					mapController.nodeChanged(existing, NodeModel.NODE_TEXT, oldText, merged);
					result.updated++;
				}
			}
		}

		final List toDelete = new ArrayList();
		for (Object dateKeyObj : existingByDateKey.keySet()) {
			if (!desired.containsKey(dateKeyObj)) {
				toDelete.add(existingByDateKey.get(dateKeyObj));
			}
		}
		for (int i = 0; i < toDelete.size(); i++) {
			final NodeModel eventNode = (NodeModel) toDelete.get(i);
			final NodeModel dayNode = eventNode.getParentNode();
			mapController.deleteWithoutUndo(eventNode);
			pruneEmptyStructure(mapController, dayNode, activitiesNode);
			result.deleted++;
		}

		if (result.added > 0 || result.updated > 0 || result.deleted > 0) {
			mapController.setSaved(map, false);
			LogUtils.info("Hash Photos sync from " + csvFile.getName() + ": +" + result.added + " ~" + result.updated
			        + " -" + result.deleted);
		}
		else {
			result.skipped = !force;
		}
		writeState(new File(exportDir, SYNC_STATE_FILE), csvFile);
		HashPhotosTitleStore.save(exportDir, newTitleStore);
		return result;
	}

	private static NodeModel ensureDayNode(final MMapController mapController, final MapModel map,
	        final NodeModel activitiesNode, final String dateKey, final Map structureCache) {
		final String year = dateKey.substring(0, 4);
		final String month = Integer.toString(Integer.parseInt(dateKey.substring(4, 6)));
		final String day = Integer.toString(Integer.parseInt(dateKey.substring(6, 8)));
		final NodeModel yearNode = findOrCreateStructureNode(mapController, map, activitiesNode, year, structureCache);
		final NodeModel monthNode = findOrCreateStructureNode(mapController, map, yearNode, month, structureCache);
		return findOrCreateStructureNode(mapController, map, monthNode, day, structureCache);
	}

	private static NodeModel findOrCreateStructureNode(final MMapController mapController, final MapModel map,
	        final NodeModel parent, final String label, final Map structureCache) {
		final String cacheKey = parent.getID() + "|" + label;
		final NodeModel cached = (NodeModel) structureCache.get(cacheKey);
		if (cached != null) {
			return cached;
		}
		for (NodeModel child : parent.getChildren()) {
			if (label.equals(normalizeNodeText(child.getText()))) {
				structureCache.put(cacheKey, child);
				return child;
			}
		}
		final NodeModel newNode = mapController.newNode(label, map);
		newNode.setLeft(false);
		mapController.insertNodeIntoWithoutUndo(newNode, parent, findStructureInsertIndex(parent, label));
		structureCache.put(cacheKey, newNode);
		return newNode;
	}

	private static int findStructureInsertIndex(final NodeModel parent, final String label) {
		for (int i = 0; i < parent.getChildCount(); i++) {
			final String existing = normalizeNodeText(((NodeModel) parent.getChildAt(i)).getText());
			if (compareStructureLabel(existing, label) > 0) {
				return i;
			}
		}
		return parent.getChildCount();
	}

	private static int compareStructureLabel(final String a, final String b) {
		try {
			return Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
		}
		catch (NumberFormatException e) {
			return a.compareTo(b);
		}
	}

	private static void pruneEmptyStructure(final MMapController mapController, NodeModel node,
	        final NodeModel activitiesNode) {
		while (node != null && node != activitiesNode && node.getChildCount() == 0) {
			final NodeModel parent = node.getParentNode();
			mapController.deleteWithoutUndo(node);
			node = parent;
		}
	}

	private static NodeModel findActivitiesNode(final NodeModel node) {
		if (node == null) {
			return null;
		}
		if (ACTIVITIES_NODE_TEXT.equals(normalizeNodeText(node.getText()))) {
			return node;
		}
		for (NodeModel child : node.getChildren()) {
			final NodeModel found = findActivitiesNode(child);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static Map indexExistingEvents(final NodeModel activitiesNode) {
		final Map result = new HashMap();
		collectExistingEvents(activitiesNode, new ArrayList(), result);
		return result;
	}

	private static void collectExistingEvents(final NodeModel node, final List pathLabels, final Map result) {
		if (node != null && !ACTIVITIES_NODE_TEXT.equals(normalizeNodeText(node.getText()))) {
			pathLabels.add(normalizeNodeText(node.getText()));
		}
		if (pathLabels.size() == 3) {
			final NodeModel dayNode = node;
			if (dayNode.getChildCount() > 0) {
				final NodeModel event = (NodeModel) dayNode.getChildAt(0);
				final String dateKey = buildDateKey(pathLabels);
				if (dateKey != null) {
					result.put(dateKey, event);
				}
			}
			return;
		}
		for (NodeModel child : node.getChildren()) {
			collectExistingEvents(child, new ArrayList(pathLabels), result);
		}
	}

	private static String buildDateKey(final List pathLabels) {
		if (pathLabels.size() != 3) {
			return null;
		}
		try {
			final String year = (String) pathLabels.get(0);
			final int month = Integer.parseInt((String) pathLabels.get(1));
			final int day = Integer.parseInt((String) pathLabels.get(2));
			return String.format("%s%02d%02d", year, month, day);
		}
		catch (Exception e) {
			return null;
		}
	}

	private static long parseTimestamp(final String stamp) {
		try {
			if (stamp.length() < 8) {
				return -1;
			}
			int year = Integer.parseInt(stamp.substring(0, 4));
			int month = Integer.parseInt(stamp.substring(4, 6));
			int day = Integer.parseInt(stamp.substring(6, 8));
			int hour = 0;
			int minute = 0;
			int second = 0;
			if (stamp.length() >= 15 && stamp.charAt(8) == '_') {
				hour = Integer.parseInt(stamp.substring(9, 11));
				minute = Integer.parseInt(stamp.substring(11, 13));
				second = Integer.parseInt(stamp.substring(13, 15));
			}
			java.util.Calendar cal = java.util.Calendar.getInstance();
			cal.clear();
			cal.set(year, month - 1, day, hour, minute, second);
			return cal.getTimeInMillis();
		}
		catch (Exception e) {
			return -1;
		}
	}

	public static boolean needsSync(final File exportDir, final File csvFile) {
		if (csvFile == null || !csvFile.isFile()) {
			return false;
		}
		final File stateFile = new File(exportDir, SYNC_STATE_FILE);
		if (!stateFile.isFile()) {
			return true;
		}
		final Map state = readState(stateFile);
		final String source = (String) state.get("source");
		final String mtime = (String) state.get("mtime");
		if (source == null || !source.equals(csvFile.getName())) {
			return true;
		}
		return mtime == null || !mtime.equals(Long.toString(csvFile.lastModified()));
	}

	static Map readCsv(final File csvFile) {
		final Map result = new HashMap();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), "UTF-8"));
			String line = reader.readLine();
			if (line != null && line.length() > 0 && line.charAt(0) == '\ufeff') {
				line = line.substring(1);
			}
			while ((line = reader.readLine()) != null) {
				final String[] parts = parseCsvLine(line);
				if (parts == null) {
					continue;
				}
				final String dateKey = parts[0].trim();
				final String title = parts[1].trim();
				if (!DATE_KEY_PATTERN.matcher(dateKey).matches() || title.length() == 0) {
					continue;
				}
				result.put(dateKey, title);
			}
		}
		catch (Exception e) {
			LogUtils.warn("Hash Photos CSV read failed: " + e.getMessage(), e);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					LogUtils.warn(e);
				}
			}
		}
		return result;
	}

	private static String[] parseCsvLine(final String line) {
		if (line == null) {
			return null;
		}
		final String trimmed = line.trim();
		if (trimmed.length() == 0) {
			return null;
		}
		final int comma = trimmed.indexOf(',');
		if (comma <= 0) {
			return null;
		}
		final String dateKey = trimmed.substring(0, comma);
		String title = trimmed.substring(comma + 1).trim();
		if (title.startsWith("\"")) {
			if (title.endsWith("\"") && title.length() >= 2) {
				title = title.substring(1, title.length() - 1);
			}
			title = title.replace("\"\"", "\"");
		}
		return new String[] { dateKey, title };
	}

	private static String normalizeNodeText(final String text) {
		if (text == null) {
			return "";
		}
		return HtmlUtils.removeHtmlTagsFromString(text).replaceAll("\\s+", " ").trim();
	}

	private static Map readState(final File stateFile) {
		final Map state = new HashMap();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(stateFile), "UTF-8"));
			String line;
			while ((line = reader.readLine()) != null) {
				final int eq = line.indexOf('=');
				if (eq <= 0) {
					continue;
				}
				state.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
			}
		}
		catch (Exception e) {
			LogUtils.warn(e);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {
					LogUtils.warn(e);
				}
			}
		}
		return state;
	}

	static void writeState(final File stateFile, final File csvFile) {
		OutputStreamWriter writer = null;
		try {
			writer = new OutputStreamWriter(new FileOutputStream(stateFile), "UTF-8");
			writer.write("source=" + csvFile.getName() + "\n");
			writer.write("mtime=" + csvFile.lastModified() + "\n");
			writer.flush();
		}
		catch (Exception e) {
			LogUtils.warn("Hash Photos sync state write failed: " + e.getMessage(), e);
		}
		finally {
			if (writer != null) {
				try {
					writer.close();
				}
				catch (Exception e) {
					LogUtils.warn(e);
				}
			}
		}
	}

	public static final class SyncResult {
		public int added;
		public int updated;
		public int deleted;
		public boolean skipped;
	}
}
