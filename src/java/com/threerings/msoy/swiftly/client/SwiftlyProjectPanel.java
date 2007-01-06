package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.util.ArrayList;

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
    public SwiftlyProjectPanel (SwiftlyApplet applet, String projectName,
                                ArrayList<SwiftlyDocument> fileList) 
    {
        super(new BorderLayout());
        _applet = applet;

        DefaultMutableTreeNode top = new DefaultMutableTreeNode(projectName);
        for (SwiftlyDocument doc : fileList) {
            top.add(new DefaultMutableTreeNode(doc));
        }

        _tree = new JTree(top);
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        _tree.addTreeSelectionListener(this);
        _scrollPane = new JScrollPane(_tree);
        add(_scrollPane);
    }

    public void valueChanged(TreeSelectionEvent e)
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _tree.getLastSelectedPathComponent();

        if (node == null) return;

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            SwiftlyDocument doc = (SwiftlyDocument)nodeInfo;
            _applet.editor.addEditorTab(doc);
        }
    }

    protected SwiftlyApplet _applet;
    protected JTree _tree;
    protected JScrollPane _scrollPane;
}
