package org.freeplane.features.map;

import java.util.Date;

/**
 * Finds the node with the latest {@link HistoryInformationModel#getLastModifiedAt()} in a map.
 */
public final class LastModifiedNodeSelector {
	private LastModifiedNodeSelector() {
	}

	public static NodeModel find(final NodeModel root) {
		if (root == null) {
			return null;
		}
		final SearchState state = new SearchState();
		visit(root, state);
		return state.bestNode;
	}

	private static void visit(final NodeModel node, final SearchState state) {
		if (!node.isRoot()) {
			final HistoryInformationModel history = node.getHistoryInformation();
			if (history != null) {
				final Date modifiedAt = history.getLastModifiedAt();
				if (modifiedAt != null) {
					final long time = modifiedAt.getTime();
					if (time >= state.bestTime) {
						state.bestTime = time;
						state.bestNode = node;
					}
				}
			}
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			visit((NodeModel) node.getChildAt(i), state);
		}
	}

	private static final class SearchState {
		private NodeModel bestNode;
		private long bestTime = Long.MIN_VALUE;
	}
}
