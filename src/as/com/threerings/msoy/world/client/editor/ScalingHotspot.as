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
        
        /*
          if (_panel.advanced.proportionalScaling) {
          var max :Number = Math.max(x, y);
          x = y = max;
          }
        */
        
        _editor.updateTargetScale(x, y);
    }

    /** Sprite scale at the beginning of modifications. Only valid during action. */
    protected var _originalScale :Point;

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale_over.png")]
    public static const HOTSPOT_OVER :Class;
}
}
