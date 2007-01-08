package com.threerings.msoy.swiftly.client;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
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
        addButton("New", _applet.editor.createNewTabAction());
        addButton("Open Project", _applet.createShowProjectWindowAction());
        addButton("Save", _applet.editor.createSaveCurrentTabAction());
        addButton("Compile", null);
        addButton("Play", null);
        addButton("Undo", null);
        addButton("Redo", null);
        // TODO let's try to get close buttons on the tabs
        addButton("Close", _applet.editor.createCloseCurrentTabAction());
        setFloatable(false);
    }

    protected JButton addButton (String title, Action action)
    {
        JButton button = new JButton(title);
        if (action != null) {
            button.addActionListener(action);
        }
        add(button);
        return button;
    }

    protected SwiftlyApplet _applet;
}
