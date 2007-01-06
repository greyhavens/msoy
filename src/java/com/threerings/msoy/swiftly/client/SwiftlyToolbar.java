package com.threerings.msoy.swiftly.client;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;

public class SwiftlyToolbar extends JToolBar
{
    public SwiftlyToolbar (SwiftlyApplet applet)
    {
        _applet = applet;
        setupToolbar();
    }

    protected void setupToolbar ()
    {
        addButton("New", null);
        addButton("Save", null);
        addButton("Compile", null);
        addButton("Play", null);
        addButton("Undo", null);
        addButton("Redo", null);
        // TODO let's try to get close buttons on the tabs
        addButton("Close", null);
        setFloatable(false);
    }

    protected JButton addButton (String title, Action action)
    {
        JButton button = new JButton(title);
        if (action != null) {
            // TODO
            // button.addActionListener(action);
        }
        add(button);
        return button;
    }

    protected SwiftlyApplet _applet;
}
