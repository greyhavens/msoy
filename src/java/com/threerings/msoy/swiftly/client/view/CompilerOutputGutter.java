//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;

import com.samskivert.util.HashIntMap;
import com.threerings.msoy.swiftly.data.CompilerOutput;

/**
 * A gutter attached to a JTextComponent that can show CompilerOutput.
 */
public class CompilerOutputGutter extends JPanel
    implements CompilerOutputComponent
{
    public CompilerOutputGutter (JTextComponent text, JScrollPane pane)
    {
        _text = text;

        setBackground(Color.LIGHT_GRAY);
        setForeground(Color.LIGHT_GRAY);
        setLayout(null);
        setBorder(null);
    }

    @Override // from JComponent
    public Dimension getPreferredSize ()
    {
        return new Dimension(DEFAULT_WIDTH, (int) _text.getPreferredSize().getHeight());
    }

    // from CompilerOutputComponent
    public void displayCompilerOutput (CompilerOutput line)
    {
        CompilerOutputIconLabel label = _labels.get(line.getLineNumber());
        if (label == null) {
            label = new CompilerOutputIconLabel(line);
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

    // from CompilerOutputComponent
    public void clearCompilerOutput ()
    {
        removeAll();
        _labels.clear();
    }

    private static final int DEFAULT_WIDTH = 16;

    /** The labels currently attached to the gutter */
    private final HashIntMap<CompilerOutputIconLabel> _labels =
        new HashIntMap<CompilerOutputIconLabel>();

    private final JTextComponent _text;

}
