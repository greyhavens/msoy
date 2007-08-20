//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Font;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class EditorToolBar extends JToolBar
    implements AccessControlListener
{
    public EditorToolBar (ProjectRoomController ctrl, SwiftlyContext ctx, SwiftlyEditor editor)
    {
        _ctrl = ctrl;
        _ctx = ctx;
        _editor = editor;

        // TODO tooltips with keyboard shortcuts
        add(_buildButton = createButton(_ctrl.buildAction, BUILD_ICON));
        add(_buildExportButton = createButton(_ctrl.buildExportAction, PREVIEW_ICON));
        add(_consoleButton = createButton(_editor.createShowConsoleAction(), CONSOLE_ICON));
        add(Box.createHorizontalGlue());

        add(_ctx.getProgressBar());

        _readOnly = new JLabel();
        _readOnly.setIcon(new ImageIcon(getClass().getResource(READ_ONLY_ICON)));
        _readOnly.setToolTipText(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.tooltip.read_only"));
        _readOnly.setVisible(false);
        add(_readOnly);

        // add the toolbar as an access control listener
        _editor.addAccessControlListener(this);

        setFloatable(false);
    }

    // from AccessControlListener
    public void writeAccessGranted ()
    {
        _buildButton.setEnabled(true);
        _buildExportButton.setEnabled(true);
        _readOnly.setVisible(false);
    }

    // from AccessControlListener
    public void readOnlyAccessGranted ()
    {
        _buildButton.setEnabled(false);
        _buildExportButton.setEnabled(false);
        _readOnly.setVisible(true);
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
    protected static final String READ_ONLY_ICON = "/rsrc/icons/swiftly/readonly.png";

    protected ProjectRoomController _ctrl;
    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JButton _buildButton;
    protected JButton _buildExportButton;
    protected JButton _consoleButton;
    protected JLabel _readOnly;
}
