/**
 *
 */
package com.threerings.msoy.swiftly.client.view;

import java.util.ArrayList;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import com.google.common.collect.Maps;

import com.threerings.msoy.swiftly.data.CompilerOutput;
import com.threerings.msoy.swiftly.data.CompilerOutput.Level;

/**
 * A label that displays an icon reflecting a CompilerOutput line.
 */
public class CompilerOutputIconLabel extends JLabel
{
    public CompilerOutputIconLabel (CompilerOutput line)
    {
        _lines = new ArrayList<CompilerOutput>();
        _highestLevel = line.getLevel();
        appendLine(line);
    }

    /**
     * Add additional compiler output to an existing BuildResultIconLabel.
     */
    public void appendLine (CompilerOutput newLine)
    {
        _lines.add(newLine);
        setToolTipText("<html>");

        for (CompilerOutput line : _lines) {
            if (line.getLevel().compareTo(_highestLevel) > 0) {
                _highestLevel = line.getLevel();
            }
            // TODO: replace with this with a new multiline tooltip class
            setToolTipText(getToolTipText() + "- " + line.getMessage() + "<br>");
        }
        setToolTipText(getToolTipText() + "</html>");

        setIcon(_levelIcons.get(_highestLevel));
    }

    /** Map CompilerOutput.Level enums to icons. */
    private static final Map<CompilerOutput.Level, ImageIcon> _levelIcons = Maps.newHashMap();

    // Initialize Enum level -> icon mapping.
    // TODO add icon for UNKNOWN and IGNORE. question mark icon?
    static {
        _levelIcons.put(CompilerOutput.Level.INFO, new ImageIcon(
            CompilerOutputIconLabel.class.getResource("/rsrc/icons/swiftly/result-info.png")));
        _levelIcons.put(CompilerOutput.Level.WARNING, new ImageIcon(
            CompilerOutputIconLabel.class.getResource("/rsrc/icons/swiftly/result-warn.png")));
        _levelIcons.put(CompilerOutput.Level.ERROR, new ImageIcon(
            CompilerOutputIconLabel.class.getResource("/rsrc/icons/swiftly/result-error.png")));
    }

    /** The CompilerOutput lines being displayed by this label. */
    private final ArrayList<CompilerOutput> _lines;

    /** Stores the most severe Level associated with this line of output */
    private Level _highestLevel;
}
