package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

        _scrollPane = new JScrollPane();
        add(_scrollPane);

        add(new JPanel());
    }

    public void loadProject (SwiftlyProject project)
    {
        _project = project;

        _top = new DefaultMutableTreeNode(project.getName());
        for (SwiftlyDocument doc : project.getFiles()) {
            addDocument(doc);
        }
        // TODO GC the old tree?
        _tree = new JTree(_top);
        _tree.setDragEnabled(true);
        _tree.setEditable(true);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);

        _scrollPane.getViewport().setView(_tree);
    }

    public void addDocument (SwiftlyDocument document)
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

    protected SwiftlyApplet _applet;
    protected SwiftlyProject _project;
    protected DefaultMutableTreeNode _top;
    protected JTree _tree;
    protected JScrollPane _scrollPane;
}
