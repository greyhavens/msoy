//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.Color;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class Console extends JScrollPane
{
    public Console (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        _ctx = ctx;
        _editor = editor;

        _consoleText = new JTextPane(_document = new DefaultStyledDocument());
        setViewportView(_consoleText);
        _consoleText.setEditable(false);

        StyleConstants.setBold(_error, true);
        StyleConstants.setForeground(_error, Color.red);
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
        try {
            _document.insertString(_document.getLength(), message, set);
        } catch (BadLocationException e) {
            // nada
        }
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JTextPane _consoleText;
    protected DefaultStyledDocument _document;
    protected SimpleAttributeSet _normal = new SimpleAttributeSet();
    protected SimpleAttributeSet _error = new SimpleAttributeSet();

}
