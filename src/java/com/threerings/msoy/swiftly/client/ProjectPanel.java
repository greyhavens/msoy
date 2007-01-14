//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.threerings.msoy.swiftly.data.DocumentElement;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.PathElementTreeNode;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.ProjectTreeModel;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class ProjectPanel extends JPanel
    implements TreeSelectionListener, TreeModelListener
{
    public ProjectPanel (SwiftlyContext ctx)
    {
        super(new BorderLayout());
        _ctx = ctx;
        add(_scrollPane, BorderLayout.CENTER);
        setupToolbar();
        add(_toolbar, BorderLayout.PAGE_END);
    }

    /**
     * Removes the currently selected node.
     */
    public void removeCurrentNode ()
    {
        PathElementTreeNode parent = (PathElementTreeNode)getSelectedNode().getParent();
        if (parent != null) {
            _treeModel.removeNodeFromParent(getSelectedNode());
        }
    }

    /**
     * Add a file element to the right spot based on the current selected node.
     */
    public PathElementTreeNode addNode (PathElement element)
    {
        PathElementTreeNode parent = (PathElementTreeNode)
            ((getSelectedPathElement().getType() == PathElement.Type.FILE) ?
             getSelectedNode().getParent() : getSelectedNode());

        // TODO this needs to insert the node in a sorted manner
        PathElementTreeNode child = new PathElementTreeNode(element);
        _treeModel.insertNodeInto(child, parent, parent.getChildCount());

        // Open any directory drop downs that need to be and scroll to the new node
        _tree.scrollPathToVisible(new TreePath(child.getPath()));
        return child;
    }

    public void setProject (ProjectRoomObject roomObj)
    {
        _roomObj = roomObj;
        _treeModel = new ProjectTreeModel(roomObj);
        _treeModel.addTreeModelListener(this);

        _tree = new JTree(_treeModel);
        _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.setShowsRootHandles(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);

        _scrollPane.getViewport().setView(_tree);
        disableToolbar();
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        PathElementTreeNode node = (PathElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        enableToolbar();
        setSelectedNode(node);

        PathElement element = getSelectedPathElement();
        if (element instanceof DocumentElement) {
            _ctx.getEditor().addEditorTab((DocumentElement)element);
        }
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        PathElementTreeNode node = (PathElementTreeNode)e.getChildren()[0];
        if (node.getElement() instanceof DocumentElement) {
            _ctx.getEditor().updateTabTitleAt((DocumentElement)node.getElement());
            // TODO: replace this with super sophisticated fine grained editing model
            _ctx.getEditor().updateTabDocument((DocumentElement)node.getElement());
        }
    }

    // from interface TreeModelListener
    public void treeNodesInserted (TreeModelEvent e)
    {
        // nada
    }

    // from interface TreeModelListener
    public void treeNodesRemoved (TreeModelEvent e)
    {
        // nada
    }

    // from interface TreeModelListener
    public void treeStructureChanged (TreeModelEvent e)
    {
        // nada
    }

    protected Action createPlusButtonAction ()
    {
        // TODO need icon
        return new AbstractAction("+") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.FILE);
            }
        };
    }

    protected Action createAddDirectoryAction ()
    {
        // TODO need icon
        return new AbstractAction("+Dir") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addPathElement(PathElement.Type.DIRECTORY);
            }
        };
    }

    protected Action createMinusButtonAction ()
    {
        // TODO need icon
        return new AbstractAction("-") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                deletePathElement();
            }
        };
    }

    protected void deletePathElement ()
    {
        // TODO throw up a Are you sure yes/no dialog
        PathElement element = getSelectedPathElement();

        // XXX we know the tab was selected in order for delete to work. This might be dangerous.
        // we also know the tab was open.. hmmm
        if (element instanceof DocumentElement) {
            _ctx.getEditor().closeCurrentTab();
        } else if (element.getType() == PathElement.Type.DIRECTORY) {
            // TODO oh god we have to remove all the tabs associated with this directory
            // soo.. every tab that has a common getParentId() ?
        } else {
            // TODO you're trying to remove the project itself? Does Homey play that?
            return;
        }
        // TODO _roomObj.service.deletePathElement(_ctx.getClient(), element);
        removeCurrentNode();
    }

    protected void addPathElement (PathElement.Type type)
    {
        // prompt the user for the directory name
        String name = _ctx.getEditor().showSelectPathElementNameDialog(type);
        if (name == null) {
            return; // if the user hit cancel do no more
        }

        // the parent element is the directory or project the selected element is in, or if
        // a project or directory is selected, that is the parent element
        PathElement parentElement = getSelectedPathElement();
        int parentId = (parentElement.getType() == PathElement.Type.FILE) ?
            parentElement.getParentId() : parentElement.elementId;

        PathElement element = null;
        if (type == PathElement.Type.DIRECTORY) {
            element = PathElement.createDirectory(name, parentId);
        } else if (type == PathElement.Type.FILE) {
            element = new DocumentElement(name, parentId, "");
            _ctx.getEditor().addEditorTab((DocumentElement)element);
        } else {
            // other types not implemented
        }
        if (element != null) {
            _roomObj.service.addPathElement(_ctx.getClient(), element);
            addNode(element);
        }
    }

    protected void setupToolbar ()
    {
        _plusButton = new JButton(createPlusButtonAction());
        _toolbar.add(_plusButton);

        _minusButton = new JButton(createMinusButtonAction());
        _toolbar.add(_minusButton);

        _addDirectoryButton = new JButton(createAddDirectoryAction());
        _toolbar.add(_addDirectoryButton);

        _toolbar.setFloatable(false);
        disableToolbar();
    }

    protected void enableToolbar ()
    {
        setToolbarEnabled(true);
    }

    protected void disableToolbar ()
    {
        setToolbarEnabled(false);
    }

    protected void setToolbarEnabled (boolean value)
    {
        _plusButton.setEnabled(value);
        _minusButton.setEnabled(value);
        _addDirectoryButton.setEnabled(value);
    }

    protected PathElementTreeNode getSelectedNode ()
    {
        return _selectedNode;
    }

    protected PathElement getSelectedPathElement ()
    {
        return _selectedNode == null ? null : (PathElement)_selectedNode.getUserObject();
    }

    protected void setSelectedNode (PathElementTreeNode node)
    {
        _selectedNode = node;
    }

    protected SwiftlyContext _ctx;
    protected ProjectRoomObject _roomObj;
    protected ProjectTreeModel _treeModel;
    protected PathElementTreeNode _selectedNode;

    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected JButton _plusButton;
    protected JButton _minusButton;
    protected JButton _addDirectoryButton;
    protected JScrollPane _scrollPane = new JScrollPane();
}
