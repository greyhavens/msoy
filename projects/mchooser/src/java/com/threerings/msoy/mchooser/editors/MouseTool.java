//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

/**
 * A tool that listens for mouse events on the editor.
 */
public abstract class MouseTool extends ImageEditorTool
    implements MouseListener, MouseMotionListener
{
    @Override // from ImageEditorTool
    public void activate ()
    {
        super.activate();
        _editor.addMouseListener(this);
        _editor.addMouseMotionListener(this);
    }

    @Override // from ImageEditorTool
    public void deactivate ()
    {
        super.deactivate();
        _editor.removeMouseListener(this);
        _editor.removeMouseMotionListener(this);
    }

    // from interface MouseListener
    public void mouseClicked (MouseEvent e)
    {
    }

    // from interface MouseListener
    public void mousePressed (MouseEvent e)
    {
    }

    // from interface MouseListener
    public void mouseReleased (MouseEvent e)
    {
    }

    // from interface MouseListener
    public void mouseEntered (MouseEvent e)
    {
    }

    // from interface MouseListener
    public void mouseExited (MouseEvent e)
    {
    }

    // from interface MouseMotionListener
    public void mouseDragged (MouseEvent e)
    {
    }

    // from interface MouseMotionListener
    public void mouseMoved (MouseEvent e)
    {
    }
}
