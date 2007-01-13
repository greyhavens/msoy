package com.threerings.msoy.swiftly.client;

import javax.swing.JButton;
import javax.swing.JToolBar;

public class EditorToolBar extends JToolBar
{
    public EditorToolBar (ProjectRoomController ctrl, SwiftlyEditor editor)
    {
        _ctrl = ctrl;
        _editor = editor;
        setupToolbar();
    }

    public void updateEditorActions(SwiftlyTextPane textPane)
    {
        _saveButton.setAction(textPane.getSaveAction());
        _undoButton.setAction(textPane.getUndoAction());
        _redoButton.setAction(textPane.getRedoAction());
    }

    protected void setupToolbar ()
    {
        // TODO replace as many of these with icons as makes sense
        add(_saveButton);
        add(_editor.createCloseCurrentTabAction());

        addSeparator();
        add(_buildButton);
        _buildButton.setAction(_ctrl.buildAction);
        add(new JButton("Play"));

        addSeparator();
        // TODO add mini icons for these + tooltips with keyboard shortcuts
        // These actions get set by updateEditorActions()
        add(_undoButton);
        add(_redoButton);

        setFloatable(false);
    }

    protected ProjectRoomController _ctrl;
    protected SwiftlyEditor _editor;

    protected JButton _saveButton = new JButton("Save");
    protected JButton _undoButton = new JButton("Undo");
    protected JButton _redoButton = new JButton("Redo");
    protected JButton _buildButton = new JButton("Build");
}
