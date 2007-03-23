package com.threerings.msoy.swiftly.client;

import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JToolBar;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class EditorToolBar extends JToolBar
{
    public EditorToolBar (ProjectRoomController ctrl, SwiftlyContext ctx, SwiftlyEditor editor)
    {
        _ctrl = ctrl;
        _ctx = ctx;
        _editor = editor;
        setupToolbar();
    }

    public void updateEditorActions(SwiftlyTextPane textPane)
    {
        _undoButton.setAction(textPane.getUndoAction());
        _redoButton.setAction(textPane.getRedoAction());
    }

    protected void setupToolbar ()
    {
        // TODO add mini icons for these + tooltips with keyboard shortcuts
        JButton build = new JButton(_ctrl.buildAction);
        build.setFont(build.getFont().deriveFont(Font.BOLD));
        add(build);

        // TODO: enable this when we can actually do something here.
        // add(new JButton(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.play")));

        // undo/redo actions get set by updateEditorActions()
        addSeparator();
        _undoButton = new JButton(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.undo"));
        add(_undoButton);
        _redoButton = new JButton(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.redo"));
        add(_redoButton);

        setFloatable(false);
    }

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JButton _saveButton;
    protected JButton _undoButton;
    protected JButton _redoButton;
}
