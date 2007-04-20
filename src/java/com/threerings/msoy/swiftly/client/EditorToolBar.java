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
        JButton build = new JButton(_ctrl.buildAction);
        build.setFont(build.getFont().deriveFont(Font.BOLD));
        add(build);

        JButton preview = new JButton(_editor.getPreviewAction());
        preview.setFont(preview.getFont().deriveFont(Font.BOLD));
        add(preview);

        JButton export = new JButton(_editor.getExportAction());
        export.setFont(export.getFont().deriveFont(Font.BOLD));
        add(export);

        setFloatable(false);
    }

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JButton _saveButton;
}
