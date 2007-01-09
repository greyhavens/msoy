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

    public void updateEditorActions()
    {
        SwiftlyTextPane textPane =  _applet.getEditor().getCurrentTextPane();
        if (textPane != null) {
            _cutButton.setAction(textPane.getCutAction());
            _copyButton.setAction(textPane.getCopyAction());
            _pasteButton.setAction(textPane.getPasteAction());
            _selectAllButton.setAction(textPane.getSelectAllAction());
            _undoButton.setAction(textPane.getUndoAction());
            _redoButton.setAction(textPane.getRedoAction());
        }
    }

    protected void setupToolbar ()
    {
        // TODO replace as many of these with icons as makes sense
        addButton("Switch Project", _applet.createShowProjectDialogAction());
        addButton("Create Project", _applet.createNewProjectDialogAction());
        addButton("New Tab", _applet.getEditor().createNewTabAction());
        addButton("Save Document", _applet.getEditor().createSaveCurrentTabAction());
        // TODO let's try to get close buttons on the tabs
        addButton("Close Tab", _applet.getEditor().createCloseCurrentTabAction());

        addSeparator();
        addButton("Compile", null);
        addButton("Play", null);

        addSeparator();
        // TODO add mini icons for these + tooltips with keyboard shortcuts
        // These actions get set by updateEditorActions()
        _cutButton = addButton("Cut", null);
        _copyButton = addButton("Copy", null);
        _pasteButton = addButton("Paste", null);
        _selectAllButton = addButton("Select All", null);
        _undoButton = addButton("Undo", null);
        _redoButton = addButton("Redo", null);

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
    protected JButton _cutButton;
    protected JButton _copyButton;
    protected JButton _pasteButton;
    protected JButton _selectAllButton;
    protected JButton _undoButton;
    protected JButton _redoButton;
}
