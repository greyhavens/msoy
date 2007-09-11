//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
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

import com.threerings.msoy.swiftly.client.event.BuildResultListener;
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.PathElement;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.data.CompilerOutput.Level;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

public class Console extends JFrame
    implements BuildResultListener
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

    /**
     * Sets the room object, which must be called when it is actually available in the editor.
     */
    public void setRoomObject (ProjectRoomObject roomObj)
    {
        _roomObj = roomObj;
    }

    @Override // from JComponent
    public void addNotify ()
    {
        super.addNotify();
        _editor.addBuildResultListener(this);
    }

    @Override // from JComponent
    public void removeNotify ()
    {
        super.removeNotify();
        _editor.removeBuildResultListener(this);
    }

    // from BuildResultListener
    public void gotResult (BuildResult result)
    {
        // first, clear the console
        _consoleText.setText("");

        // then display each line of output
        for (CompilerOutput line : result.getOutput()) {
            if (line.getLevel() == Level.IGNORE || line.getLevel() == Level.UNKNOWN) {
                continue;
            }
            if (line.getLineNumber() != -1 && line.getPath() != null) {
                appendLineNumberButton(line);
            }
            appendMessage(line.getMessage() + "\n", _messageLevels.get(line.getLevel()));
        }
    }

    /**
     * Appends a message to the console with the supplied style.
     */
    protected void appendMessage (String message, AttributeSet set)
    {
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
                _editor.openPathElement(
                    pathElement, line.getLineNumber(), line.getColumnNumber(), true);
            }
        });
        button.setToolTipText(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.tooltip.line_number"));

        // stick the button into the style
        StyleConstants.setComponent(style, button);

        // this string cannot be blank
        appendMessage("ignored", style);
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


    protected static final SimpleAttributeSet _normal = new SimpleAttributeSet();
    protected static final SimpleAttributeSet _warning = new SimpleAttributeSet();
    protected static final SimpleAttributeSet _error = new SimpleAttributeSet();

    protected SwiftlyContext _ctx;
    protected SwiftlyEditor _editor;
    protected ProjectRoomObject _roomObj;

    protected JTextPane _consoleText;
    protected DefaultStyledDocument _document;

    /** Map CompilerOutput.Level enums to font attributes. */
    protected static final Map<CompilerOutput.Level, SimpleAttributeSet> _messageLevels =
        new HashMap<CompilerOutput.Level, SimpleAttributeSet>();

    // Initialize Enum level -> font attribute mapping.
    static {
        _messageLevels.put(CompilerOutput.Level.INFO, _normal);
        _messageLevels.put(CompilerOutput.Level.IGNORE, _normal);
        _messageLevels.put(CompilerOutput.Level.UNKNOWN, _normal);
        _messageLevels.put(CompilerOutput.Level.WARNING, _warning);
        _messageLevels.put(CompilerOutput.Level.ERROR, _error);
    }
}
