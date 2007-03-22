//
// $Id$

package com.threerings.msoy.swiftly.client;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class Console extends JScrollPane
{
    public Console (SwiftlyContext ctx, SwiftlyEditor editor)
    {
        super(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        _ctx = ctx;
        _editor = editor;

        _consoleText = new JTextArea();
        setViewportView(_consoleText);
        _consoleText.setEditable(false);
    }

    /**
     * Appends a message to the console. A newline is added.
     */
    public void consoleMessage (String message)
    {
        _consoleText.append(message + "\n");
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;

    protected JTextArea _consoleText;
}
