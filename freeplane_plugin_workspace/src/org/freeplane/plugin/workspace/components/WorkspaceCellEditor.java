/**
 * author: Marcel Genzmehr
 * 24.07.2011
 */
package org.freeplane.plugin.workspace.components;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

import org.freeplane.plugin.workspace.WorkspaceController;
import org.freeplane.plugin.workspace.model.AWorkspaceTreeNode;

/**
 * 
 */
public class WorkspaceCellEditor extends DefaultTreeCellEditor {

	/***********************************************************************************
	 * CONSTRUCTORS
	 **********************************************************************************/

	/**
	 * @param tree
	 * @param renderer
	 */
	public WorkspaceCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
		super(tree, renderer);
	}

	/***********************************************************************************
	 * METHODS
	 **********************************************************************************/

	public Component getTreeCellEditorComponent(JTree tree, Object treeNode, boolean isSelected, boolean expanded, boolean leaf,
			int row) {
		if (treeNode instanceof AWorkspaceTreeNode) {
			AWorkspaceTreeNode node = (AWorkspaceTreeNode) treeNode;
			setNodeIcon(renderer,node);
			return super.getTreeCellEditorComponent(tree, node.getName(), isSelected, expanded, leaf, row);	
		}
		return super.getTreeCellEditorComponent(tree, treeNode, isSelected, expanded, leaf, row);
	}

	public boolean isCellEditable(EventObject event) {		
		if (event != null && event.getSource() instanceof JTree) {
			setTree((JTree) event.getSource());
			if (event instanceof MouseEvent) {
				TreePath path = tree.getPathForLocation(((MouseEvent) event).getX(), ((MouseEvent) event).getY());
				if (path != null) {
					AWorkspaceTreeNode treeNode = (AWorkspaceTreeNode) path.getLastPathComponent();
					if(!treeNode.isEditable()) {
						return false;
					}					
				}
			}
		}
		else if(event == null && WorkspaceController.getCurrentModeExtension().getView().getSelectionPath() != null) {
			return false;
		}
		return super.isCellEditable(event);
	}
	
	/**
	 * @param value
	 */
	protected void setNodeIcon(DefaultTreeCellRenderer renderer, AWorkspaceTreeNode wsNode) {
		renderer.setOpenIcon(null);
		renderer.setClosedIcon(null);
		renderer.setLeafIcon(null);
	}
	/***********************************************************************************
	 * REQUIRED METHODS FOR INTERFACES
	 **********************************************************************************/
}
