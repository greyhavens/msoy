package com.threerings.msoy.swiftly.client;

import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JToolBar;

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

    protected void setupToolbar ()
    {
        // TODO add mini icons for these + tooltips with keyboard shortcuts
        JButton button = new JButton(_ctrl.buildAction);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        add(button);

        button = new JButton(_editor.getPreviewAction());
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        add(button);

        button = new JButton(_editor.getExportAction());
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        add(button);

        button = new JButton(_editor.createShowConsoleAction());
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        add(button);

        setFloatable(false);
    }

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
}
