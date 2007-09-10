//
// $Id$
package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.samskivert.util.HashIntMap;
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
        _labels = new HashIntMap<BuildResultIconLabel>();

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
        _labels.clear();

        for (CompilerOutput line : result.getOutput()) {
            // if the line has a file associated with it and that path is equivalent to the
            // PathElement being displayed in the text pane this gutter is attached to
            if (line.getPath() != null && line.getPath().equals(
                _text.getSwiftlyDocument().getPathElement().getAbsolutePath())) {
                BuildResultIconLabel label = _labels.get(line.getLineNumber());
                if (label == null) {
                    label = new BuildResultIconLabel(line);
                    _labels.put(line.getLineNumber(), label);
                    add(label);

                } else {
                    label.appendLine(line);
                }

                // position the label on the gutter
                int fontHeight = getFontMetrics(_text.getFont()).getHeight();
                int center = Math.max(0, (fontHeight / 2) - (label.getIcon().getIconHeight() / 2));
                // slight fudge factor
                if (fontHeight > label.getIcon().getIconHeight()) {
                    center += 2;
                }
                int labelY = ((fontHeight * line.getLineNumber()) - fontHeight) + center;
                label.setBounds(
                    0, labelY, label.getIcon().getIconWidth(), label.getIcon().getIconHeight());
            }
        }
    }

    protected static final int DEFAULT_WIDTH = 16;

    /** The labels currently attached to the gutter */
    protected final HashIntMap<BuildResultIconLabel> _labels;

    protected final SwiftlyEditor _editor;
    protected final SwiftlyTextPane _text;
}
