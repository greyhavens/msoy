package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class SwiftlyProjectPanel extends JPanel {

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
        _scrollPane = new JScrollPane(_tree);
        add(_scrollPane);
    }

    protected SwiftlyApplet _applet;
    protected JTree _tree;
    protected JScrollPane _scrollPane;
}
