//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Allows the image to be moved around the editor.
 */
public class MoveTool extends MouseTool
{
    @Override // from XXX
    public String getName ()
    {
        return "Move";
    }

    @Override // from MouseTool
    public void mousePressed (MouseEvent e)
    {
        _downPos = new Point(e.getX(), e.getY());
        _downOffset = _editor.getModel().getOffset();
    }

    @Override // from MouseTool
    public void mouseReleased (MouseEvent e)
    {
        final Point offset = getOffset(e);
        if (!offset.equals(_downOffset)) {
            _editor.applyOp(new EditorOp() {
                public void apply (EditorModel model) {
                    model.setOffset(offset);
                }
            });
        }

        _downPos = null;
        _downOffset = null;
    }

    @Override // from MouseTool
    public void mouseDragged (MouseEvent e)
    {
        if (_downPos != null) {
            // while we're interactively dragging we modify the model directly, when they release
            // the mouse we'll record the change to the model state in a single operation
            _editor.getModel().setOffset(getOffset(e));
        }
    }

    protected Point getOffset (MouseEvent e)
    {
        return new Point(_downOffset.x + e.getX() - _downPos.x,
                         _downOffset.y + e.getY() - _downPos.y);
    }

    protected Point _downPos, _downOffset;
}
