//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.world.client.FurniSprite;

/**
 * Hotspot that scales the object based on user's mouse movement.
 */
public class ScalingHotspot extends Hotspot
{
    /**
     * Scaling operation will be snapped to original proportions if the difference between
     * horizontal and vertical scale is less than this ratio of the greater of the two.
     */
    public static const SNAP_RATIO :Number = 0.2;

    /**
     * Proportional snap will not be applied until the mouse has moved at least
     * this number of pixels away from where it was clicked.
     */
    public static const SNAP_DEAD_RADIUS :Number = 20;
    
    
    public function ScalingHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);
        
        this.x = targetWidth;
        this.y = 0;
    }
    
    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        
        _originalScale =
            new Point(_editor.target.getMediaScaleX(), _editor.target.getMediaScaleY());
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
        
        _originalScale = null;
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        _displayStandard = new HOTSPOT() as DisplayObject;
        _displayMouseOver = new HOTSPOT_OVER() as DisplayObject;
        _displayLocked = new HOTSPOT_LOCKED() as DisplayObject;
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
        var newoffset :Point = mouse.subtract(_originalHotspot);
        var oldoffset :Point = _anchor.subtract(_originalHotspot);
        
        // find scaling factor based on mouse movement
        var ratioX :Number = newoffset.x / oldoffset.x;
        var ratioY :Number = newoffset.y / oldoffset.y;
        
        var x :Number = ratioX * _originalScale.x;
        var y :Number = ratioY * _originalScale.y;

        // should we snap?
        if (Point.distance(mouse, _anchor) > SNAP_DEAD_RADIUS) {
            var signx :Number = x / Math.abs(x);
            var signy :Number = y / Math.abs(y);
            var maxScale :Number = Math.max(Math.abs(x), Math.abs(y));
            var delta :Number = Math.abs(Math.abs(x) - Math.abs(y));
            if (delta < maxScale * SNAP_RATIO) {
                x = maxScale * signx;
                y = maxScale * signy;
                switchDisplay(_displayLocked);
            } else {
                switchDisplay(_displayMouseOver);
            }
        }
        
        _editor.updateTargetScale(x, y);
    }
    
    /** Sprite scale at the beginning of modifications. Only valid during action. */
    protected var _originalScale :Point;

    /** Bitmap used for hotspot with mouseover. */
    protected var _displayLocked :DisplayObject;


    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale_over.png")]
    public static const HOTSPOT_OVER :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale_locked.png")]
    public static const HOTSPOT_LOCKED :Class;
}
}
