//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.Graphics2D;

/**
 * Performs an operation on an image in the image editor.
 */
public abstract class ImageEditorTool
{
    /**
     * Returns the human readable name of this editor.
     */
    public abstract String getName ();

    /**
     * Provides this tool with a reference to its editor.
     */
    public void init (ImageEditor editor)
    {
        _editor = editor;
    }

    /**
     * Called when a tool is selected.
     */
    public void activate ()
    {
    }

    /**
     * Called when a tool is deselected.
     */
    public void deactivate ()
    {
    }

    /**
     * Called on the active tool when the image editor repaints itself.
     */
    public void paint (Graphics2D gfx)
    {
    }

    protected ImageEditor _editor;
}
