//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Hotspot that moves the target object along the Y axis.
 */
public class MovementXZHotspot extends Hotspot
{
    public function MovementXZHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);

        // find stem height (in screen coords)
        var oldloc :MsoyLocation = _editor.target.getLocation();
        var stem :Point = _editor.roomView.layout.metrics.roomDistanceToPixelDistance(
            new Point(0, oldloc.y), oldloc.z);

        // show this hotspot on the floor
        this.x = targetWidth / 2;
        this.y = targetHeight + stem.y;
    }
    
    // @Override from Hotspot
    override protected function updateAction (event :MouseEvent) :void
    {
        super.updateAction(event);

        updateTargetLocation(event.stageX, event.stageY);
    }

    // @Override from Hotspot
    override protected function initializeDisplay () :void
    {
        // do not call super - we're providing different bitmaps
        _displayStandard = new HOTSPOT() as DisplayObject;
        _displayMouseOver = new HOTSPOT_OVER() as DisplayObject;
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        var loc :MsoyLocation = _editor.roomView.layout.pointToLocationAtHeight(sx, sy, 0);

        if (loc != null) {
            // and since click location is now on the floor, don't forget to restore stem height
            loc.y = _editor.target.getLocation().y;
            _editor.updateTargetLocation(loc);
        }
    }

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_xz.png")]
    public static const HOTSPOT :Class;
    [Embed(source="../../../../../../../../rsrc/media/skins/button/furniedit/hotspot_move_xz_over.png")]
    public static const HOTSPOT_OVER :Class;
}
}
