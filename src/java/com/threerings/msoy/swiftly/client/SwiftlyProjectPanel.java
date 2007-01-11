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
import javax.swing.tree.TreeSelectionModel;

public class SwiftlyProjectPanel extends JPanel
    implements TreeSelectionListener
{
    public SwiftlyProjectPanel (SwiftlyApplet applet)
    {
        super(new BorderLayout());
        _applet = applet;

        add(_scrollPane, BorderLayout.CENTER);

        setupToolbar();
        add(_toolbar, BorderLayout.PAGE_END);
    }

    public void loadProject (SwiftlyProject project)
    {
        _project = project;

        _top = new DefaultMutableTreeNode(project);
        for (SwiftlyDocument doc : project.getFiles()) {
            addDocumentToTree(doc);
        }

        _tree = new JTree(_top);
        _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);

        _scrollPane.getViewport().setView(_tree);
    }

    // TODO this is not adding new documents correctly.
    public void addDocumentToTree (SwiftlyDocument document)
    {
        _top.add(new DefaultMutableTreeNode(document));
    }

    // from interface TreeSelectionListener
    public void valueChanged (TreeSelectionEvent e)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            SwiftlyDocument doc = (SwiftlyDocument)nodeInfo;
            _applet.getEditor().addEditorTab(doc);
        }
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

    // Opens a new, unsaved document in a tab.
    protected void openNewDocument ()
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        FileElement element = (FileElement)nodeInfo;
        SwiftlyDocument doc = new SwiftlyDocument("", "", element.getParent());
        _applet.showSelectFilenameDialog(doc);
        addDocumentToTree(doc);
        _applet.getEditor().addEditorTab(doc);
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
        }
    }

    protected void setupToolbar ()
    {
        _toolbar.add(new JButton(createPlusButtonAction()));
        _toolbar.add(new JButton(createMinusButtonAction()));
        _toolbar.add(new JButton("Foo"));

        _toolbar.setFloatable(false);
    }

    protected SwiftlyApplet _applet;
    protected SwiftlyProject _project;
    protected DefaultMutableTreeNode _top;
    protected JTree _tree;
    protected JToolBar _toolbar = new JToolBar();
    protected JScrollPane _scrollPane = new JScrollPane();
}
