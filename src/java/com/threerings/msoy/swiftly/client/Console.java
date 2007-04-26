//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class Console extends JFrame
{
    public Console (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.dialog.console.title"));
        _ctx = ctx;
        _editor = editor;

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        _consoleText = new JTextPane(_document = new DefaultStyledDocument());
        _consoleText.setEditable(false);

        // stick the text pane into a scroller
        JScrollPane scroller = new JScrollPane(_consoleText,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(400, 400));

        // setup the toolbar
        JToolBar toolbar = new JToolBar();
        JButton button = new JButton(
            new AbstractAction(ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.clear")) {
            public void actionPerformed (ActionEvent e) {
                clearConsole();
            }
        });
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        // push the buttons into the center of the toolbar
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(button);
        button = new JButton(
            new AbstractAction(ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.close")) {
            public void actionPerformed (ActionEvent e) {
                hideWindow();
            }
        });
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        toolbar.add(button);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.setFloatable(false);

        // add the scroller and toolbar to a panel and set that as the content pane
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scroller, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.PAGE_END);
        setContentPane(panel);

        StyleConstants.setBold(_error, true);
        StyleConstants.setForeground(_error, Color.red);

        pack();
        setVisible(false);
    }

    /**
     * Appends a message to the console. A newline is added.
     */
    public void consoleMessage (String message)
    {
        appendMessage(message + "\n", _normal);
    }

    /**
     * Appends an error message to the console, styled bold and red. A newline is added.
     */
    public void errorMessage (String message)
    {
        appendMessage(message + "\n", _error);
    }

    /**
     * Appends a message to the console with the supplied style.
     */
    protected void appendMessage (String message, SimpleAttributeSet set)
    {
        setVisible(true);
        try {
            _document.insertString(_document.getLength(), message, set);
            _consoleText.setCaretPosition(_document.getLength());
        } catch (BadLocationException e) {
            // nada
        }
    }

    /**
     * Hides the console window.
     */
    protected void hideWindow ()
    {
        setVisible(false);
    }

    /**
     * Clears the console.
     */
    protected void clearConsole ()
    {
        _consoleText.setText("");
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JTextPane _consoleText;
    protected DefaultStyledDocument _document;
    protected SimpleAttributeSet _normal = new SimpleAttributeSet();
    protected SimpleAttributeSet _error = new SimpleAttributeSet();

}
