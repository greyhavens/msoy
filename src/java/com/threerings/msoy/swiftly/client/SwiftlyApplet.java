package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JApplet;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet {

    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }
        _editor = new SwiftlyEditor();
        // TODO _editor.setMinimumSize(minimumSize);
        _projectPanel = new SwiftlyProjectPanel();
        _toolbar = new SwiftlyToolbar();
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _editor, _projectPanel);
        _splitPane.setOneTouchExpandable(true);
        _splitPane.setDividerLocation(650);

        add(_toolbar, BorderLayout.PAGE_START);
        add(_splitPane, BorderLayout.CENTER);
        setSize(800, 600);

        // XXX temp. add a few example tabs
        _editor.addEditorTab("file #1", "http://localhost:8080/swiftly/index.html");
        _editor.addEditorTab("file #2", "http://localhost:8080/catalog/index.html");
    }

    protected SwiftlyEditor _editor;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected JSplitPane _splitPane;
} 
