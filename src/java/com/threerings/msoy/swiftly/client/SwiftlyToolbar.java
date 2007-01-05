package com.threerings.msoy.swiftly.client;

import javax.swing.JButton;
import javax.swing.JToolBar;

public class SwiftlyToolbar extends JToolBar {
    public SwiftlyToolbar (SwiftlyApplet applet) {
        _applet = applet;
        setupToolbar();
    }

    protected void setupToolbar () {
        addButton("Save");
        addButton("Compile");
        addButton("Play");
        addButton("Undo");
        addButton("Redo");
        // TODO let's try to get close buttons on the tabs
        addButton("Close");
        setFloatable(false);
    }

    protected JButton addButton (String title) {
        JButton button = new JButton(title);
        add(button);
        return button;
    }

    protected SwiftlyApplet _applet;
}
