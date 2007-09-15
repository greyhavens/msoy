//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Font;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.EditorActionProvider;

/**
 * Implementation of EditorToolBar.
 */
public class EditorToolBarView extends JToolBar
    implements EditorToolBar
{
    public EditorToolBarView (EditorActionProvider actions, Translator translator,
                              ProgressBarView progressBar)
    {
        _translator = translator;

        add(createButton(actions.getBuildAction()));
        add(createButton(actions.getBuildExportAction()));
        add(createButton(actions.getShowConsoleAction()));
        add(Box.createHorizontalGlue());

        add(progressBar);

        _readOnly = new JLabel();
        _readOnly.setIcon(new ImageIcon(getClass().getResource(READ_ONLY_ICON)));
        _readOnly.setToolTipText(_translator.xlate("m.tooltip.read_only"));
        _readOnly.setVisible(false);
        add(_readOnly);

        setFloatable(false);
    }

    // from AccessControlComponent
    public void showWriteAccess ()
    {
        _readOnly.setVisible(false);
    }

    // from AccessControlComponent
    public void showReadOnlyAccess ()
    {
        _readOnly.setVisible(true);
    }

    private JButton createButton (Action action)
    {
        JButton button = new JButton(action);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        // TODO: button.setMargin(new Insets(15, 0, 0, 0));
        // text below and centered on icons
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        return button;
    }

    /** The location of various icons */
    private static final String READ_ONLY_ICON = "/rsrc/icons/swiftly/readonly.png";

    private final Translator _translator;
    private final JLabel _readOnly;
}
