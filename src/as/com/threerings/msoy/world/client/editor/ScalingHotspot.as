//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.util.Log;

import com.threerings.msoy.client.Msgs;

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

        this.x = (_editor.target.getMediaScaleX() > 0) ? targetWidth : 0;
        this.y = (_editor.target.getMediaScaleY() > 0) ? 0 : targetHeight;

        // figure out if this hotspot is leaving the display area
        var roomPos :Point = _editor.roomView.globalToLocal(
            _editor.target.localToGlobal(new Point(this.x, this.y)));

        if (roomPos.y < _currentDisplay.height / 2) {
            roomPos.y = _currentDisplay.height / 2;
            var localPos :Point = _editor.target.globalToLocal(
                _editor.roomView.localToGlobal(roomPos));
            this.y = localPos.y;
        }

    }

    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        _originalScale = new Point(_editor.target.getMediaScaleX(), _editor.target.getMediaScaleY());
    }

    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);
        updateTargetScale(event);
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
        _displayStandard.alpha = 0.35;
        _displayMouseOver = new HOTSPOT_OVER() as DisplayObject;
    }

    /**
     * Computes target's furni scale based on a dragging action, and updates the editor.
     */
    protected function updateTargetScale (event :MouseEvent) :void
    {
        if (_editor.target == null) {
            Log.getLog(this).warning("Scaling hotspot updated without an editing target!");
            return;
        }

        // find pixel distance from hotspot to the current and the original mouse pointer
        var mouse :Point = new Point(event.stageX, event.stageY);
        var newoffset :Point = mouse.subtract(_originalHotspot);
        var oldoffset :Point = _anchor.subtract(_originalHotspot);

        // find scaling factor based on mouse movement
        var ratioX :Number = newoffset.x / oldoffset.x;
        var ratioY :Number = newoffset.y / oldoffset.y;

        var x :Number = ratioX * _originalScale.x;
        var y :Number = ratioY * _originalScale.y;

        // should we snap?
        var noSnap :Boolean = event.shiftKey || event.altKey || event.ctrlKey;
        if (!noSnap) {
            var scale :Number = Math.min(Math.abs(x), Math.abs(y));
            x = scale * ((x < 0) ? -1 : 1);
            y = scale * ((y < 0) ? -1 : 1);
        }

        _editor.updateTargetScale(x, y);
    }

    override protected function getToolTip () :String
    {
        return Msgs.EDITING.get("i.scaling");
    }

    /** Sprite scale at the beginning of modifications. Only valid during action. */
    protected var _originalScale :Point;

    /** Bitmap used for hotspot with no scale locking. */
    protected var _displayUnlocked :DisplayObject;

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_scale_over.png")]
    public static const HOTSPOT_OVER :Class;
}
}
