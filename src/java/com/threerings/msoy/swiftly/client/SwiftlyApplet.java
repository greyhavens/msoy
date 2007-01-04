package com.threerings.msoy.swiftly.client;

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
        setContentPane(editor);
        editor.setSize(800, 600);

        // XXX temp. add a new example tabs local to my machine
        editor.addEditorTab("file #1", "http://salton-billing.puzzlepirates.com/billing");
        editor.addEditorTab("file #2", "http://salton-billing.banghowdy.com/billing");
    }

    protected SwiftlyEditor editor;
} 
