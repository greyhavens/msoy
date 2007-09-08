//
// $Id$
package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.CompilerOutput;

/**
 * A gutter attached to a TextComponent that can show build results.
 */
public class EditorGutter extends JPanel
    implements BuildResultListener
{
    public EditorGutter (SwiftlyEditor editor, SwiftlyTextPane text, JScrollPane pane)
    {
        _editor = editor;
        _text = text;

        setBackground(Color.LIGHT_GRAY);
        setForeground(Color.LIGHT_GRAY);
        setLayout(null);
        setBorder(null);
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

    @Override // from JComponent
    public Dimension getPreferredSize ()
    {
        return new Dimension(DEFAULT_WIDTH, (int) _text.getPreferredSize().getHeight());
    }

    // from BuildResultListener
    public void gotResult (BuildResult result)
    {
        removeAll();

        for (CompilerOutput line : result.getOutput()) {
            // XXX TODO don't check for null and make sure getPath() is going to work how we want
            if (line.getPath() != null && line.getPath().equals(
                _text.getSwiftlyDocument().getPathElement().getAbsolutePath())) {
                addResultIcon(line);
            }
        }
    }

    protected void addResultIcon(CompilerOutput line)
    {
        // TODO pick the most severe icon and show only that which means a lookup to see if
        // there is already a label at this location
        ImageIcon icon = null;
        switch (line.getLevel()) {
            case ERROR:
                icon = new ImageIcon(getClass().getResource(ERROR_ICON));
                break;
            case WARNING:
                icon = new ImageIcon(getClass().getResource(WARN_ICON));
                break;
            case INFO:
                icon = new ImageIcon(getClass().getResource(INFO_ICON));
                break;
            case IGNORE:
            case UNKNOWN:
            default:
                return;
        }
        JLabel label = new JLabel(icon);
        // TODO this should APPEND the message so that multiple errors on the same line
        // are in the tooltip which means we need to track all labels being used in a hashmap
        label.setToolTipText(line.getMessage());

        FontMetrics fm = getFontMetrics(_text.getFont());
        int fontHeight = fm.getHeight();
        int center = Math.max(0, (fontHeight / 2) - (icon.getIconHeight() / 2));
        // slight fudge factor
        if (fontHeight > icon.getIconHeight()) {
            center += 2;
        }
        int labelY = ((fontHeight * line.getLineNumber()) - fontHeight) + center;
        label.setBounds(0, labelY, icon.getIconWidth(), icon.getIconHeight());
        // TODO can we use add(label, row); ?
        add(label);
    }

    protected static final String ERROR_ICON = "/rsrc/icons/swiftly/result-error.png";
    protected static final String WARN_ICON = "/rsrc/icons/swiftly/result-warn.png";
    protected static final String INFO_ICON = "/rsrc/icons/swiftly/result-info.png";
    protected static final int DEFAULT_WIDTH = 16;

    protected final SwiftlyEditor _editor;
    protected final SwiftlyTextPane _text;
}
