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
        _cutButton.setAction(textPane.getCutAction());
        _copyButton.setAction(textPane.getCopyAction());
        _pasteButton.setAction(textPane.getPasteAction());
        _selectAllButton.setAction(textPane.getSelectAllAction());
        _undoButton.setAction(textPane.getUndoAction());
        _redoButton.setAction(textPane.getRedoAction());
    }

    protected void setupToolbar ()
    {
        // TODO replace as many of these with icons as makes sense
        add(_applet.createShowProjectDialogAction());
        add(_applet.createNewProjectDialogAction());
        add(_applet.getEditor().createNewTabAction());
        add(_applet.getEditor().createSaveCurrentTabAction());
        add(_applet.getEditor().createCloseCurrentTabAction());

        addSeparator();
        add(new JButton("Compile"));
        add(new JButton("Play"));

        addSeparator();
        // TODO add mini icons for these + tooltips with keyboard shortcuts
        // These actions get set by updateEditorActions()
        add(_cutButton);
        add(_copyButton);
        add(_pasteButton);
        add(_selectAllButton);
        add(_undoButton);
        add(_redoButton);

        setFloatable(false);
    }

    protected SwiftlyApplet _applet;
    protected JButton _cutButton = new JButton("Cut");
    protected JButton _copyButton = new JButton("Copy");
    protected JButton _pasteButton = new JButton("Paste");
    protected JButton _selectAllButton = new JButton("Select All");
    protected JButton _undoButton = new JButton("Undo");
    protected JButton _redoButton = new JButton("Redo");
}
