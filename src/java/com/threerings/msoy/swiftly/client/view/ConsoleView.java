//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import com.google.common.collect.Maps;

import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.PathElementEditor;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;

/**
 * Implementation of Console.
 */
public class ConsoleView extends JFrame
    implements Console
{
    public ConsoleView (Translator translator, PathElementEditor editor)
    {
        super(translator.xlate("m.dialog.console.title"));
        _translator = translator;
        _editor = editor;

        setDefaultCloseOperation(HIDE_ON_CLOSE);

        _consoleText = new JTextPane(_document = new DefaultStyledDocument());
        _consoleText.setEditable(false);

        // stick the text pane into a scroller
        JScrollPane scroller = new JScrollPane(_consoleText,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setPreferredSize(new Dimension(400, 400));
        setContentPane(scroller);

        StyleConstants.setBold(_error, true);
        StyleConstants.setForeground(_error, Color.red);
        StyleConstants.setBold(_warning, true);
        StyleConstants.setForeground(_warning, Color.yellow.darker());

        pack();
        setVisible(false);
    }

    // from Console
    public void clearConsole ()
    {
        _consoleText.setText("");
    }


    // from Console
    public void appendCompilerOutput (CompilerOutput line)
    {
        appendCompilerOutput(line, null);
    }

    // from Console
    public void appendCompilerOutput (final CompilerOutput line, final PathElement element)
    {
        if (element != null) {
            appendLineNumberButton(line, element, new AbstractAction() {
                public void actionPerformed (ActionEvent e)
                {
                    _editor.openPathElementAt(element,
                        new PositionLocation(line.getLineNumber(), line.getColumnNumber(), true));
                }
            });
        }
        appendMessage(line.getMessage() + "\n", _messageLevels.get(line.getLevel()));
    }

    // from Console
    public void displayConsole ()
    {
        setVisible(true);
    }

    // from Console
    public void destroy ()
    {
        dispose();
    }

    /**
     * Append a string to the console with the given AttributeSet applied for styling the string.
     */
    private void appendMessage (String message, AttributeSet set)
    {
        try {
            _document.insertString(_document.getLength(), message, set);
            _consoleText.setCaretPosition(_document.getLength());
        } catch (BadLocationException e) {
            // nada
        }
    }

    /**
     * Append a LineNumberButton to the console associated with the given PathElement.
     */
    private void appendLineNumberButton (CompilerOutput line, PathElement element, Action action)
    {
        // the button must be wrapped in a style
        Style style = _document.addStyle("LineNumberButton", null);

        // create the button and action
        LineNumberButton button = new LineNumberButton(action);
        button.setToolTipText(_translator.xlate("m.tooltip.line_number"));

        // stick the button into the style
        StyleConstants.setComponent(style, button);

        // this string cannot be blank
        appendMessage("ignored", style);
    }

    private static class LineNumberButton extends JButton
    {
        public LineNumberButton (Action action)
        {
            super(action);
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

        private static final Dimension BUTTON_SIZE = new Dimension(16, 16);
        private static final String LINE_NUMBER_ICON = "/rsrc/icons/swiftly/zoom.gif";
    }

    private static final SimpleAttributeSet _normal = new SimpleAttributeSet();
    private static final SimpleAttributeSet _warning = new SimpleAttributeSet();
    private static final SimpleAttributeSet _error = new SimpleAttributeSet();

    private final Translator _translator;
    private final PathElementEditor _editor;

    private final JTextPane _consoleText;
    private final DefaultStyledDocument _document;

    /** Map CompilerOutput.Level enums to font attributes. */
    private static final Map<CompilerOutput.Level, SimpleAttributeSet> _messageLevels =
        Maps.newHashMap();

    // Initialize Enum level -> font attribute mapping.
    static {
        _messageLevels.put(CompilerOutput.Level.INFO, _normal);
        _messageLevels.put(CompilerOutput.Level.IGNORE, _normal);
        _messageLevels.put(CompilerOutput.Level.UNKNOWN, _normal);
        _messageLevels.put(CompilerOutput.Level.WARNING, _warning);
        _messageLevels.put(CompilerOutput.Level.ERROR, _error);
    }
}
