//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.threerings.micasa.client.ChatPanel;
import com.threerings.micasa.client.OccupantList;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class TabbedConsole extends JTabbedPane
{
    public TabbedConsole (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(JTabbedPane.BOTTOM);
        _ctx = ctx;
        _editor = editor;

        addChangeListener(new TabChangedListener());
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // add the console tab
        _consoleText = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(_consoleText,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        _consoleText.setEditable(false);
        add(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.console.tab.console"), scrollPane);
        // Alt-O gives you the console TODO what do we want to set here?
        setMnemonicAt(0, KeyEvent.VK_O);
        // underline C_o_nsole to give a hint
        setDisplayedMnemonicIndexAt(0, 1);

        // add the chat tab
        JPanel panel = new JPanel(
            new HGroupLayout(HGroupLayout.STRETCH, HGroupLayout.STRETCH, 5, HGroupLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel.setPreferredSize(new Dimension(0, 200));
        panel.add(new ChatPanel(_ctx, true));
        OccupantList ol;
        panel.add(ol = new OccupantList(_ctx), HGroupLayout.FIXED);
        ol.setPreferredSize(new Dimension(100, 0));
        add(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.console.tab.chat"), panel);
        // Alt-C gives you the console TODO what do we want to set here?
        setMnemonicAt(1, KeyEvent.VK_C);
        // underline _C_hat to give a hint
        setDisplayedMnemonicIndexAt(1, 0);
    }

    /**
     * Appends a message to the console. A newline is added.
     */
    public void consoleMessage (String message)
    {
        _consoleText.append(message + "\n");
    }

    // TODO what, if anything do we need this to do?
    protected class TabChangedListener implements ChangeListener
    {
        // from interface ChangeListener
        public void stateChanged(ChangeEvent evt) {
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JTextArea _consoleText;
}
