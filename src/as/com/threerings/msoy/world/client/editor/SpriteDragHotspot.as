//
// $Id: MovementXZHotspot.as 7528 2008-01-31 23:45:03Z mdb $

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Hotspot that moves the target object either along its current XZ plane or along the Y axis.
 *
 * It works very similarly to its parent class, except it doesn't assume the user is clicking
 * on the floor - instead, since the user will be dragging the furni itself, it will try
 * to perform intersection against the XZ-plane at the furni's current height.
 */
public class SpriteDragHotspot extends MovementXZHotspot
{
    public function SpriteDragHotspot (editor :FurniEditor)
    {
        super(editor);
    }

    /** Moves the furni over to the new location. */
    override protected function updateTargetLocation (sx :Number, sy :Number) :void
    {
        sx -= (_anchor.x - _originalHotspot.x);
        sy -= (_anchor.y - _originalHotspot.y);
        if (_currentDisplay == _displayYMouseOver) {
            var cloc :ClickLocation = _editor.roomView.layout.pointToFurniLocation(
                sx, sy, _editor.target.getLocation(), RoomMetrics.N_UP, false);
            _editor.updateTargetLocation(cloc.loc);

        } else {
            var fy :Number = _editor.target.getLocation().y;
            var loc :MsoyLocation = _editor.roomView.layout.pointToLocationAtHeight(sx, sy, fy);
            if (loc != null) {
                _editor.updateTargetLocation(loc);
            }
        }
    }
}
}
