package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class SwiftlyProjectPanel extends JPanel {

    public SwiftlyProjectPanel (SwiftlyApplet applet) {
        super(new BorderLayout());
        _applet = applet;
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("My Awesome Project");
        top.add(new DefaultMutableTreeNode("File 1"));
        top.add(new DefaultMutableTreeNode("File 2"));
        _tree = new JTree(top);
        _scrollPane = new JScrollPane(_tree);
        add(_scrollPane);
    }

    protected SwiftlyApplet _applet;
    protected JTree _tree;
    protected JScrollPane _scrollPane;
}
