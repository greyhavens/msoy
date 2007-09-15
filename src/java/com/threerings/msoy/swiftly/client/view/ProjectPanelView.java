//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.EditorActionProvider;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;
import com.threerings.msoy.swiftly.data.ProjectTreeModel;

/**
 * Implementation of a ProjectPanel.
 */
public class ProjectPanelView extends JPanel
    implements ProjectPanel
{
    public ProjectPanelView (EditorActionProvider actions, TreeSelectionListener listener,
                             Translator translator, ProjectTreeModel model)
    {
        super(new BorderLayout());

        // setup the toolbar
        _toolbar.add(createButton(actions.getAddFileAction()));
        _toolbar.add(createButton(actions.getUploadFileAction()));
        _toolbar.add(createButton(actions.getDeleteFileAction()));
        _toolbar.setFloatable(false);

        add(_toolbar, BorderLayout.PAGE_START);
        _popup.add(actions.getDeleteFileAction());
        _popup.add(actions.getRenameFileAction());
        add(_scrollPane, BorderLayout.CENTER);

        _tree = new JTree(model);
        // XXX disable dragging until the rest of the support can be wired up
        // _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.setShowsRootHandles(false);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addMouseListener(new PopupListener());
        _tree.setCellRenderer(new ProjectTreeCellRenderer());
        ToolTipManager.sharedInstance().registerComponent(_tree);

        _scrollPane.getViewport().setView(_tree);

        _tree.addTreeSelectionListener(listener);
    }

    @Override // from JComponent
    public Dimension getPreferredSize ()
    {
        Dimension d = super.getPreferredSize();
        d.width = Math.min(250, d.width);
        return d;
    }

    // from ProjectPanel
    public void renameCurrentElement ()
    {
        _tree.startEditingAtPath(_tree.getSelectionPath());
    }

    // from ProjectPanel
    public void enableEditing ()
    {
        _tree.setEditable(true);
    }

    // from ProjectPanel
    public void disableEditing ()
    {
        _tree.setEditable(false);
    }

    // from ProjectPanel
    public PathElement getCurrentParent ()
    {
        // the parent element is the directory or project the selected element is in, or if
        // a project or directory is selected, that is the parent element
        PathElement parentElement = getSelectedPathElement();
        if (parentElement.getType() == PathElement.Type.FILE) {
            parentElement = parentElement.getParent();
        }
        return parentElement;
    }

    // from ProjectPanel
    public PathElement getSelectedPathElement ()
    {
        if (getSelectedNode() == null) {
            return null;
        }
        return getSelectedNode().getElement();
    }

    /**
     * Convenience method to strip action names from buttons.
     */
    private JButton createButton (Action action)
    {
        JButton button = new JButton(action);
        // hide the action text
        button.setText("");
        return button;
    }

    /**
     * Return the currently selected PathElementTreeNode.
     */
    private PathElementTreeNode getSelectedNode ()
    {
        if (_tree.getSelectionPath() == null) {
            return null;
        }
        return (PathElementTreeNode)_tree.getSelectionPath().getLastPathComponent();
    }

    private class PopupListener extends MouseAdapter
    {
        @Override // from MouseAdapter
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override // from MouseAdapter
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                return;
            }
            TreePath path = _tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                _tree.setSelectionPath(path);
                _popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    private final JTree _tree;
    private final JToolBar _toolbar = new JToolBar();
    private final JScrollPane _scrollPane = new JScrollPane();
    private final JPopupMenu _popup = new JPopupMenu();
}
