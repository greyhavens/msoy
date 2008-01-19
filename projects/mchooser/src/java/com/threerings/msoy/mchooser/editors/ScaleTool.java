//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Scales the image.
 */
public class ScaleTool extends MouseTool
{
    @Override // from EditorTool
    public String getName ()
    {
        return "Scale";
    }

    @Override // from MouseTool
    public void mousePressed (MouseEvent e)
    {
        _downPos = new Point(e.getX(), e.getY());
        _downScaleX = _editor.getModel().getScaleX();
        _downScaleY = _editor.getModel().getScaleY();
    }

    @Override // from MouseTool
    public void mouseReleased (MouseEvent e)
    {
        final double scaleX = computeScaleX(e), scaleY = computeScaleY(e);
        _editor.applyOp(new EditorOp() {
            public void apply (EditorModel model) {
                model.setScale(scaleX, scaleY);
            }
        });

        _downPos = null;
    }

    @Override // from MouseTool
    public void mouseDragged (MouseEvent e)
    {
        if (_downPos != null) {
            // while we're interactively dragging we modify the model directly, when they release
            // the mouse we'll record the change to the model state in a single operation
            _editor.getModel().setScale(computeScaleX(e), computeScaleY(e));
        }
    }

    protected double computeScaleX (MouseEvent e)
    {
        int delta = isFreeScaling(e) ? (e.getX() - _downPos.x) : (e.getY() - _downPos.y);
        return _downScaleX * computeScale(delta);
    }

    protected double computeScaleY (MouseEvent e)
    {
        return _downScaleY * computeScale(e.getY() - _downPos.y);
    }

    protected double computeScale (int delta)
    {
        if (delta > 0) {
            return Math.min(2, 1 + delta / SCALE_DISTANCE);
        } else {
            return Math.max(0.5, 1 - 0.5 * -delta / SCALE_DISTANCE);
        }
    }

    protected boolean isFreeScaling (MouseEvent e)
    {
        return (e.getModifiers() & MouseEvent.SHIFT_DOWN_MASK) != 0;
    }

    protected Point _downPos;
    protected double _downScaleX, _downScaleY;

    protected static final double SCALE_DISTANCE = 150;
}
