package com.threerings.msoy.swiftly.client;

import javax.swing.JButton;
import javax.swing.JToolBar;

public class SwiftlyToolbar extends JToolBar {
    public SwiftlyToolbar () {
        JButton button = new JButton("Save");
        add(button);
        button = new JButton("Compile");
        add(button);
        button = new JButton("Play");
        add(button);
        button = new JButton("Undo");
        add(button);
        button = new JButton("Redo");
        add(button);
        setFloatable(false);
    }
}
