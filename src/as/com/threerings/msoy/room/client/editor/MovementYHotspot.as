//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.Bitmap;
import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.ColorTransform;

import com.threerings.flash.MathUtil;
import com.threerings.msoy.room.client.ClickLocation;
import com.threerings.msoy.room.client.RoomMetrics;

/**
 * Hotspot that moves the target object along the Y axis.
 */
public class MovementYHotspot extends Hotspot
{
    public function MovementYHotspot (editor :FurniEditor)
    {
        super(editor, true);
    }

    // @Override from Hotspot
    override public function updateDisplay (targetWidth :Number, targetHeight :Number) :void
    {
        super.updateDisplay(targetWidth, targetHeight);

        this.x = 0;
        this.y = targetHeight / 2;
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

        _displayMouseOver = new Bitmap((_displayStandard as Bitmap).bitmapData);
        _displayMouseOver.transform.colorTransform = new ColorTransform(1.25, 1.25, 1.25);
    }

    /** Moves the furni over to the new location. */
    protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        sx -= (_anchor.x - _originalHotspot.x);
        sy -= (_anchor.y - _originalHotspot.y);

        var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
            sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);

        if (! _advancedMode) {
            cloc.loc.y = MathUtil.clamp(cloc.loc.y, 0, 1);
        }

        _editor.updateTargetLocation(cloc.loc);
    }

    // Bitmaps galore!
    [Embed(source="../../../../../../../../rsrc/media/skins/button/roomeditor/hotspot_move_y.png")]
    public static const HOTSPOT :Class;
}
}
