//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.world.client.FurniSprite;

/**
 * Hotspot that scales the object based on user's mouse movement.
 */
public class ScalingHotspot extends Hotspot
{
    public function ScalingHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    
    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        
        var t :FurniSprite = _editor.target;
        _targetOriginalScale = new Point(t.getMediaScaleX(), t.getMediaScaleY());
        _targetOriginalHotspot = t.localToGlobal(t.getLayoutHotSpot());
//       _targetOriginalBounds =
//            new Rectangle(t.x, t.y, t.getActualWidth(), t.getActualHeight());
    }

    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);

        updateTargetScale(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function endAction (event :MouseEvent) :void
    {
        super.endAction(event);
        
        _targetOriginalScale = null;
        _targetOriginalHotspot = null;
//        _targetOriginalBounds = null;
    }

    /**
     * Computes target's furni scale based on a dragging action, and updates the editor.
     */
    protected function updateTargetScale (sx :Number, sy :Number) :void
    {
        if (_editor.target == null) {
            Log.getLog(this).warning("Scaling hotspot updated without an editing target!");
            return;
        }
        
        // find pixel distance from hotspot to the current and the original mouse pointer
        var mouse :Point = new Point(sx, sy);
        var newoffset :Point = mouse.subtract(_targetOriginalHotspot);
        var oldoffset :Point = _anchor.subtract(_targetOriginalHotspot);
        
        // find scaling factor based on mouse movement
        var ratioX :Number = newoffset.x / oldoffset.x;
        var ratioY :Number = newoffset.y / oldoffset.y;
        
        var x :Number = ratioX * _targetOriginalScale.x;
        var y :Number = ratioY * _targetOriginalScale.y;
        
        /*
          if (_panel.advanced.proportionalScaling) {
          var max :Number = Math.max(x, y);
          x = y = max;
          }
        */
        
        _editor.updateTargetScale(x, y);
    }

//    /** Sprite size at the beginning of modifications. Only valid during action. */
//    protected var _targetOriginalBounds :Rectangle;

    /** Sprite scale at the beginning of modifications. Only valid during action. */
    protected var _targetOriginalScale :Point;

    /** Sprite center in stage coordinates. Only valid during action. */
    protected var _targetOriginalHotspot :Point;

}
}
