//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.ColorTransform;
import flash.geom.Point;

import com.threerings.flash.MathUtil;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.room.client.ClickLocation;
import com.threerings.msoy.room.client.RoomMetrics;
import com.threerings.msoy.room.data.MsoyLocation;

/**
 * Hotspot that moves the target object along the Y axis.
 */
public class MovementXZHotspot extends Hotspot
{
    /** The hotspot won't go beyond this depth, even in advanced mode. */
    public static const MAX_DEPTH :int = 10;
    
    public function MovementXZHotspot (editor :FurniEditor)
    {
        super(editor, true);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);
        // show this hotspot on the floor
        this.x = targetWidth / 2;
        this.y = targetHeight + getStemHeight();
    }

    // @Override from Hotspot
    override protected function startAction (event :MouseEvent) :void
    {
        super.startAction(event);
        
        // make sure we're showing the proper action icon
        switchDisplay(event.shiftKey ? _displayYMouseOver : _displayMouseOver);
    }

    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);
        updateTargetLocation(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function endAction (event :MouseEvent) :void
    {
        super.endAction(event);
        // switch back to the non-shifted action icon
        switchDisplay(_displayMouseOver);
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        // do not call super - we're providing different bitmaps
        _displayStandard = new HOTSPOTXZ() as DisplayObject;
        const bd :BitmapData = (_displayStandard as Bitmap).bitmapData;

        _displayMouseOver = new Bitmap(bd);
        _displayMouseOver.transform.colorTransform = new ColorTransform(1.25, 1.25, 1.25);

        _displayYMouseOver = new Bitmap(bd);
        _displayYMouseOver.transform.colorTransform = new ColorTransform(.5, .5, .5);
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        var loc :MsoyLocation = null;
        if (_currentDisplay == _displayYMouseOver) {
            sx -= (_anchor.x - _originalHotspot.x);
            sy -= (_anchor.y - _originalHotspot.y);
            var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
                sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);
            loc = cloc.loc;

        } else {
            loc = _editor.roomView.layout.pointToLocationAtHeight(sx, sy, 0);
            if (loc != null) {
                // since click location is now on the floor, don't forget to restore stem height
                loc.y = _editor.target.getLocation().y;
            }
        }

        // see if the user specified an invalid location, or one that's too far away
        if (loc == null || (_advancedMode && loc.z >= MAX_DEPTH)) {
            return; 
        }

        // in default mode, clamp to room boundaries
        if (! _advancedMode) {
            loc.x = MathUtil.clamp(loc.x, 0, 1);
            loc.z = MathUtil.clamp(loc.z, 0, 1);
        }
        
        _editor.updateTargetLocation(loc);
    }

    protected function getStemHeight () :Number
    {
        var oldloc :MsoyLocation = _editor.target.getLocation();
        return _editor.roomView.layout.metrics.roomDistanceToPixelDistance(
            new Point(0, oldloc.y), oldloc.z).y;
    }

    override protected function getToolTip () :String
    {
        return Msgs.EDITING.get("i.moving");
    }

    /** Bitmap used for hotspot with mouseover when shift pressed. */
    protected var _displayYMouseOver :DisplayObject;

    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_move_xz.png")]
    public static const HOTSPOTXZ :Class;
}
}
