package org.freeplane.plugin.workspace.features.nodepins;

import java.util.LinkedHashSet;
import java.util.Set;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.text.DetailTextModel;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;

public final class NodeDetailsTagService {

	private NodeDetailsTagService() {
	}

	public static Set getUserTags(final NodeModel node) {
		if (node == null) {
			return new LinkedHashSet();
		}
		return NodeDetailsTagUtils.parseUserTags(DetailTextModel.getDetailTextText(node));
	}

	public static boolean isPinned(final NodeModel node) {
		if (node == null) {
			return false;
		}
		return NodeDetailsTagUtils.isPinnedInDetails(DetailTextModel.getDetailTextText(node));
	}

	public static void setUserTags(final NodeModel node, final Set userTags) {
		writeTags(node, userTags, isPinned(node));
	}

	public static void togglePin(final NodeModel node) {
		writeTags(node, getUserTags(node), !isPinned(node));
	}

	public static void removeAllManagedTags(final NodeModel node) {
		writeTags(node, new LinkedHashSet(), false);
	}

	public static void removePinOnly(final NodeModel node) {
		writeTags(node, getUserTags(node), false);
	}

	private static void writeTags(final NodeModel node, final Set userTags, final boolean pinned) {
		if (node == null || node.getMap() == null) {
			return;
		}
		final String existing = DetailTextModel.getDetailTextText(node);
		final String userHtml = NodeDetailsTagUtils.extractUserContentHtml(existing);
		final LinkedHashSet allTags = new LinkedHashSet();
		if (userTags != null) {
			allTags.addAll(userTags);
		}
		if (pinned) {
			allTags.add(NodeDetailsTagUtils.PIN_TAG);
		}
		final String newHtml = NodeDetailsTagUtils.buildDetailsHtml(userHtml, allTags);
		if (Controller.getCurrentModeController() == null) {
			return;
		}
		final TextController textController = TextController.getController();
		if (!(textController instanceof MTextController)) {
			return;
		}
		final MTextController mTextController = (MTextController) textController;
		mTextController.setDetails(node, newHtml);
		if (newHtml != null && newHtml.length() > 0) {
			mTextController.setDetailsHidden(node, false);
		}
		NodePinsIndex.getInstance().updateFromNode(node);
	}
}
