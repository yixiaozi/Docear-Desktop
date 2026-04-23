package org.freeplane.plugin.workspace.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.ClipboardOwner;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.freeplane.core.util.LogUtils;
import org.freeplane.plugin.workspace.dnd.DnDController;
import org.freeplane.plugin.workspace.dnd.IWorspaceClipboardOwner;
import org.freeplane.plugin.workspace.io.IFileSystemRepresentation;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

public class WorkspaceNodeRenderer extends DefaultTreeCellRenderer {

	private int highlightedRow = -1;
	private String highlightQuery = "";
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public WorkspaceNodeRenderer() {
		
	}

	public Component getTreeCellRendererComponent(JTree tree, Object treeNode, boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {
		if(treeNode != null && treeNode instanceof AWorkspaceTreeNode ) {
			DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
			AWorkspaceTreeNode node = (AWorkspaceTreeNode) treeNode;
			setNodeIcon(renderer, node);
			setToolTip(renderer, node);
			JLabel label = (JLabel) renderer.getTreeCellRendererComponent(tree, treeNode, sel, expanded, leaf, row, hasFocus);			
			if(row == this.highlightedRow) {
				try {
				label.setBorder(BorderFactory.createLineBorder(UIManager.getColor(borderSelectionColor), 1));
				} 
				catch (Exception e) {
					label.setBorder(BorderFactory.createLineBorder(label.getForeground(), 1));
				}
			}
			label.setText(formatHighlightedText(node.getName()));
			label.setIcon(null);
			if(isCut(node)) {
				//WORKSPACE - ToDo: make the item transparent (including the icon?)
				int alpha = new Double(255 * 0.5).intValue();
				label.setForeground(new Color(label.getForeground().getRed(), label.getForeground().getGreen(), label.getForeground().getBlue(), alpha));
			}
			return label;
		}
		return super.getTreeCellRendererComponent(tree, treeNode, sel, expanded, leaf, row, hasFocus);
	}
	
	private boolean isCut(AWorkspaceTreeNode node) {
		ClipboardOwner owner = DnDController.getSystemClipboardController().getClipboardOwner();
		if(owner != null && owner instanceof IWorspaceClipboardOwner) {
			if(!((IWorspaceClipboardOwner) owner).getTransferable().isCopy() && ((IWorspaceClipboardOwner) owner).getTransferable().contains(node)) {
				return true;
			}
		}
		return false;
	}

	private void setToolTip(DefaultTreeCellRenderer renderer, AWorkspaceTreeNode node) {
		if(node instanceof IFileSystemRepresentation) {
			try {
				renderer.setToolTipText(((IFileSystemRepresentation) node).getFile().getPath());
			}
			catch (Exception e) {
				LogUtils.warn(e);
			}
		}
	}

	/**
	 * @param value
	 */
	protected void setNodeIcon(DefaultTreeCellRenderer renderer, AWorkspaceTreeNode wsNode) {
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		renderer.setLeafIcon(null);
	}
	
	public void highlightRow(int row) {
		this.highlightedRow = row;
	}

	public void setHighlightQuery(String query) {
		this.highlightQuery = query == null ? "" : query.trim().toLowerCase();
	}

	private String formatHighlightedText(String original) {
		if (original == null) {
			return "";
		}
		if (highlightQuery.length() == 0) {
			return original;
		}
		String lower = original.toLowerCase();
		int matchIndex = lower.indexOf(highlightQuery);
		if (matchIndex < 0) {
			return original;
		}
		int matchEnd = matchIndex + highlightQuery.length();
		String before = escapeHtml(original.substring(0, matchIndex));
		String match = escapeHtml(original.substring(matchIndex, matchEnd));
		String after = escapeHtml(original.substring(matchEnd));
		return "<html>" + before + "<font color='#d11a2a'>" + match + "</font>" + after + "</html>";
	}

	private String escapeHtml(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
