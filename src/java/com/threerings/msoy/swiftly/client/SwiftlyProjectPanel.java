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
                                ArrayList<String> fileList) 
    {
        super(new BorderLayout());
        _applet = applet;

        DefaultMutableTreeNode top = new DefaultMutableTreeNode(projectName);
        for (String fileName : fileList) {
            top.add(new DefaultMutableTreeNode(fileName));
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
            _applet.editor.addEditorTab(
                "file #3", _applet.getDocumentBase() + "swiftly.nocache.html");
        } else {
        }
    }

    protected SwiftlyApplet _applet;
    protected JTree _tree;
    protected JScrollPane _scrollPane;
}
