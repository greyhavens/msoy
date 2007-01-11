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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class SwiftlyProjectPanel extends JPanel
    implements TreeSelectionListener, TreeModelListener
{
    public SwiftlyProjectPanel (SwiftlyApplet applet)
    {
        super(new BorderLayout());
        _applet = applet;

        add(_scrollPane, BorderLayout.CENTER);

        setupToolbar();
        add(_toolbar, BorderLayout.PAGE_END);
    }

    /** Remove all nodes except the root node. */
    public void clear ()
    {
        _top.removeAllChildren();
        _treeModel.reload();
    }

    /** Remove the currently selected node. */
    public void removeCurrentNode ()
    {
        TreePath currentSelection = _tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)
                 (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
            if (parent != null) {
                _treeModel.removeNodeFromParent(currentNode);
                return;
            }
        } 
    }

    /** Add child to the currently selected nodes parent. */
    public DefaultMutableTreeNode addNode (Object child)
    {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = _tree.getSelectionPath();

        if (parentPath == null) {
            parentNode = _top;
        } else {
            parentNode = (DefaultMutableTreeNode)(parentPath.getLastPathComponent());
        }

        return addNode(parentNode, child, true);
    }

    public DefaultMutableTreeNode addNode (DefaultMutableTreeNode parent, Object child)
    {
        return addNode(parent, child, false);
    }

    public DefaultMutableTreeNode addNode (DefaultMutableTreeNode parent, Object child, 
                                            boolean shouldBeVisible)
    {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = _top;
        }

        _treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        if (shouldBeVisible) {
            _tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    public void loadProject (SwiftlyProject project)
    {
        _project = project;

        _top = new DefaultMutableTreeNode(project);
        _treeModel = new DefaultTreeModel(_top);
        _treeModel.addTreeModelListener(this);

        for (SwiftlyDocument doc : project.getFiles()) {
            addDocumentToTree(doc);
        }

        _tree = new JTree(_treeModel);
        _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.setShowsRootHandles(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);

        _scrollPane.getViewport().setView(_tree);
        disableToolbar();
    }

    public void addDocumentToTree (SwiftlyDocument document)
    {
        _top.add(new DefaultMutableTreeNode(document));
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;
        enableToolbar();

        _selectedNode = (FileElement)node.getUserObject();
        if (_selectedNode.getType() == FileElement.DOCUMENT) {
            _applet.getEditor().addEditorTab((SwiftlyDocument)_selectedNode);
        }
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        DefaultMutableTreeNode node;
        node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

        /*
         * If the event lists children, then the changed
         * node is the child of the node we've already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */
        try {
            int index = e.getChildIndices()[0];
            node = (DefaultMutableTreeNode) (node.getChildAt(index));
        } catch (NullPointerException exc) {}

        // grab the new name
        String newName = (String)node.getUserObject();
        _selectedNode.setName(newName);
        // TODO need to save the file element to the server
        node.setUserObject(_selectedNode);
        if (_selectedNode.getType() == FileElement.DOCUMENT) {
            _applet.getEditor().updateTabTitleAt((SwiftlyDocument)_selectedNode);
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
                openNewDocument();
            }
        };
    }

    protected Action createMinusButtonAction ()
    {
        // TODO need icon
        return new AbstractAction("-") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                deleteDocument();
            }
        };
    }

    protected Action createAddDirectoryAction ()
    {
        // TODO need icon
        return new AbstractAction("+Dir") {
            // from AbstractAction
            public void actionPerformed (ActionEvent e) {
                addDirectory();
            }
        };
    }

    // Opens a new, unsaved document in a tab.
    protected void openNewDocument ()
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        FileElement element = (FileElement)nodeInfo;
        SwiftlyDocument doc = new SwiftlyDocument("", "", element.getParent());
        _applet.showSelectFileElementNameDialog(doc);
        _applet.getEditor().addEditorTab(doc);
        addNode(doc);
    }

    protected void deleteDocument ()
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        // TODO this is almost certainly not right.
        if (node.isLeaf()) {
            SwiftlyDocument doc = (SwiftlyDocument)nodeInfo;
            _applet.deleteDocument(doc);
            _applet.getEditor().closeCurrentTab();
            removeCurrentNode();
        }
    }

    protected void addDirectory ()
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        FileElement element = (FileElement)nodeInfo;
        ProjectDirectory dir = new ProjectDirectory("", element.getParent());
        _applet.showSelectFileElementNameDialog(dir);
        addNode(dir);
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

    protected SwiftlyApplet _applet;
    protected SwiftlyProject _project;
    protected FileElement _selectedNode;
    protected DefaultMutableTreeNode _top;
    protected DefaultTreeModel _treeModel;
    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected JButton _plusButton;
    protected JButton _minusButton;
    protected JButton _addDirectoryButton;
    protected JScrollPane _scrollPane = new JScrollPane();
}
