//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;

import java.net.URL;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
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
     * Sets the room object, which must be called when it is actually available in the editor.
     */
    public void setRoomObject (ProjectRoomObject roomObj)
    {
        _roomObj = roomObj;
    }

    /**
     * Iterates over the supplied CompilerOutput and prints messages to the console.
     */
    public void displayCompilerOutput (List<CompilerOutput> output)
    {
        for (CompilerOutput line : output) {
            switch (line.getLevel()) {
                case ERROR:
                case WARNING:
                    if (line.getLineNumber() != -1 && line.getPath() != null) {
                        appendLineNumberButton(line);
                    }
                    appendMessage(line.getMessage() + "\n", _error);
                    break;
                case INFO:
                    appendMessage(line.getMessage() + "\n", _normal);
                    break;
                case IGNORE:
                case UNKNOWN:
                    break;
            }
        }
    }

    /**
     * Appends a message to the console with the supplied style.
     */
    protected void appendMessage (String message, AttributeSet set)
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
     * Appends a button to the console which goes to a line number in a document.
     */
    protected void appendLineNumberButton (final CompilerOutput line)
    {
        // the button must be wrapped in a style
        Style style = _document.addStyle("LineNumberButton", null);

        // look the path element up
        final PathElement pathElement = _roomObj.findPathElementByPath(line.getPath());
        // we didn't find the path element so don't display a broken button
        if (pathElement == null) {
            return;
        }

        // create the button and action
        LineNumberButton button = new LineNumberButton();
        button.addActionListener(new AbstractAction() {
            public void actionPerformed (ActionEvent e)
            {
                _editor.openPathElement(pathElement, line.getLineNumber(), line.getColumnNumber());
            }
        });
        button.setToolTipText(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.tooltip.line_number"));

        // stick the button into the style
        StyleConstants.setComponent(style, button);

        // this string cannot be blank
        appendMessage("ignored", style);
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

    protected static class LineNumberButton extends JButton
    {
        public LineNumberButton ()
        {
            setIcon(new ImageIcon(getClass().getResource(LINE_NUMBER_ICON)));
            setAlignmentY(0.8f);
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        @Override // from JButton
        public Dimension getPreferredSize()
        {
            return BUTTON_SIZE;
        }

        @Override // from JButton
        public Dimension getMinimumSize()
        {
            return BUTTON_SIZE;
        }

        @Override // from JButton
        public Dimension getMaximumSize()
        {
            return BUTTON_SIZE;
        }

        protected static final Dimension BUTTON_SIZE = new Dimension(16, 16);
        protected static final String LINE_NUMBER_ICON = "/rsrc/icons/swiftly/zoom.gif";
    }

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected ProjectRoomObject _roomObj;

    protected JTextPane _consoleText;
    protected DefaultStyledDocument _document;
    protected SimpleAttributeSet _normal = new SimpleAttributeSet();
    protected SimpleAttributeSet _error = new SimpleAttributeSet();

}
