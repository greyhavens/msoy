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
            FileElementTreeNode currentNode = (FileElementTreeNode)
                 (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode)(currentNode.getParent());
            if (parent != null) {
                _treeModel.removeNodeFromParent(currentNode);
                return;
            }
        } 
    }

    /** Add a file element to the right spot based on the current selected node. */
    public FileElementTreeNode addNode (FileElement element)
    {
        FileElementTreeNode parentNode = (FileElementTreeNode)getSelectedNode().getParent();

        if (parentNode == null) {
            parentNode = _top;
        }

        // if the node selected is a directory, that's the parent
        // TODO consider just using getAllowsChildren()

        if (getSelectedFileElement().getType() == FileElement.DIRECTORY) {
            parentNode = getSelectedNode();
        }

        FileElementTreeNode newNode = new FileElementTreeNode(element);
        // TODO this needs to insert the node in a sorted manner
        _treeModel.insertNodeInto(newNode, parentNode, parentNode.getChildCount());

        // Open any directory drop downs that need to be and scroll to the new node
        _tree.scrollPathToVisible(new TreePath(newNode.getPath()));
        return newNode;
    }

    public void loadProject (SwiftlyProject project)
    {
        _project = project;

        _top = new FileElementTreeNode(project);
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
        _top.add(new FileElementTreeNode(document));
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        FileElementTreeNode node = (FileElementTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;
        enableToolbar();

        setSelectedNode(node);
        FileElement element = getSelectedFileElement();

        if (element.getType() == FileElement.DOCUMENT) {
            _applet.getEditor().addEditorTab((SwiftlyDocument)element);
        }
    }

    // from interface TreeModelListener
    public void treeNodesChanged (TreeModelEvent e)
    {
        // get the changed node
        FileElementTreeNode node =
            (FileElementTreeNode) e.getTreePath().getLastPathComponent();

        /*
         * If the event lists children, then the changed
         * node is the child of the node we've already
         * gotten.  Otherwise, the changed node and the
         * specified node are the same.
         */
        try {
            int index = e.getChildIndices()[0];
            node = (FileElementTreeNode) (node.getChildAt(index));
        } catch (NullPointerException exc) {}

        // grab the new name
        String newName = (String)node.getUserObject();

        // the renamed node has a string user object. set it back to the file element.
        FileElement element = getSelectedFileElement();
        // TODO try/catch block here
        _applet.renameFileElement(element, newName);
        element.setName(newName);
        node.setUserObject(element);
        if (element.getType() == FileElement.DOCUMENT) {
            _applet.getEditor().updateTabTitleAt((SwiftlyDocument)element);
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
        FileElementTreeNode node = (FileElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) return;

        FileElement element = (FileElement)node.getUserObject();
        SwiftlyDocument doc = new SwiftlyDocument("", "", element.getParent());

        // prompt the user for the file name
        String name = _applet.showSelectFileElementNameDialog(FileElement.DOCUMENT);
        // if the user hit cancel do no more
        if (name == null) return;
        doc.setName(name);

        addNode(doc);
        if (getSelectedFileElement().getType() == FileElement.DOCUMENT) {
            _applet.getEditor().addEditorTab(doc);
        }
    }

    protected void deleteDocument ()
    {
        FileElementTreeNode node = (FileElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) return;

        // TODO throw up a Are you sure yes/no dialog

        FileElement element = (FileElement)node.getUserObject();

        // TODO We're probably going to put this in a try/catch block
        _applet.deleteFileElement(element);

        // XXX we know the tab was selected in order for delete to work. This might be dangerous.
        // we also know the tab was open.. hmmm
        if (element.getType() == FileElement.DOCUMENT) {
            _applet.getEditor().closeCurrentTab();
        } else if (element.getType() == FileElement.DIRECTORY) {
            // TODO oh god we have to remove all the tabs associated with this directory
            // soo.. every tab that has a common getParent() ?
        } else {
            // TODO you're trying to remove the project itself? Does Homey play that?
        }
        removeCurrentNode();
    }

    protected void addDirectory ()
    {
        FileElementTreeNode node = (FileElementTreeNode) _tree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }

        FileElement element = (FileElement)node.getUserObject();
        ProjectDirectory dir = new ProjectDirectory("", element.getParent());

        // prompt the user for the directory name
        String name = _applet.showSelectFileElementNameDialog(FileElement.DIRECTORY);
        // if the user clicked cancel do no more
        if (name == null) {
            return;
        }
        dir.setName(name);

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

    protected FileElement getSelectedFileElement ()
    {
        return _selectedFileElement;
    }

    protected FileElementTreeNode getSelectedNode ()
    {
        return _selectedNode;
    }

    protected void setSelectedNode (FileElementTreeNode node)
    {
        _selectedNode = node;
        _selectedFileElement = (FileElement)node.getUserObject();
    }

    protected SwiftlyApplet _applet;
    protected SwiftlyProject _project;
    protected FileElement _selectedFileElement;
    protected FileElementTreeNode _selectedNode;
    protected FileElementTreeNode _top;
    protected DefaultTreeModel _treeModel;
    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected JButton _plusButton;
    protected JButton _minusButton;
    protected JButton _addDirectoryButton;
    protected JScrollPane _scrollPane = new JScrollPane();
}
