//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Font;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class EditorToolBar extends JToolBar
{
    public EditorToolBar (ProjectRoomController ctrl, SwiftlyContext ctx, SwiftlyEditor editor)
    {
        _ctrl = ctrl;
        _ctx = ctx;
        _editor = editor;

        // TODO tooltips with keyboard shortcuts
        add(createButton(_ctrl.buildAction, BUILD_ICON));
        add(createButton(_ctrl.buildExportAction, PREVIEW_ICON));
        add(createButton(_editor.createShowConsoleAction(), CONSOLE_ICON));

        setFloatable(false);
    }

    protected JButton createButton (Action action, String icon)
    {
        JButton button = new JButton(action);
        button.setIcon(new ImageIcon(getClass().getResource(icon)));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        // TODO: button.setMargin(new Insets(15, 0, 0, 0));
        // text below and centered on icons
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    }

    /** The location of various icons */
    protected static final String BUILD_ICON = "/rsrc/icons/swiftly/build.png";
    protected static final String PREVIEW_ICON = "/rsrc/icons/swiftly/preview.png";
    protected static final String CONSOLE_ICON = "/rsrc/icons/swiftly/console.png";

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
}
