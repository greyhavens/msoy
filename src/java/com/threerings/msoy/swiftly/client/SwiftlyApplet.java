package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;

import javax.swing.JApplet;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet {

    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }
        editor = new SwiftlyEditor();
        toolbar = new SwiftlyToolbar();

        add(toolbar, BorderLayout.PAGE_START);
        add(editor, BorderLayout.CENTER);
        setSize(800, 600);

        // XXX temp. add a few example tabs
        editor.addEditorTab("file #1", "http://localhost:8080/swiftly/index.html");
        editor.addEditorTab("file #2", "http://localhost:8080/catalog/index.html");
    }

    protected SwiftlyEditor editor;
    protected SwiftlyToolbar toolbar;
} 
