//
// $Id$

package com.threerings.msoy.room.client.layout {

import flash.display.DisplayObject;

import flash.geom.Point;

import com.threerings.flash.Vector3;
import com.threerings.msoy.room.client.ClickLocation;
import com.threerings.msoy.room.client.RoomElement;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyLocation;

public class RoomLayoutTopdown extends RoomLayoutStandard
{
    public function RoomLayoutTopdown (view :RoomView)
    {
        super(view);
    }

    public override function locationToPoint (location :MsoyLocation) :Point
    {
        return new Point(location.x * _metrics.sceneWidth, (1-location.z) * _metrics.sceneHeight);
    }

    public override function pointToLocationAtDepth (
        stageX :Number, stageY :Number, depth :Number) :MsoyLocation
    {
        var cloc :ClickLocation = pointToLocation(stageX, stageY);
        return (cloc != null) ? cloc.loc : null;
    }

    // from interface RoomLayout
    public override function pointToLocationAtHeight (
        stageX :Number, stageY :Number, height :Number) :MsoyLocation
    {
        var cloc :ClickLocation = pointToLocation(stageX, stageY);
        return (cloc != null) ? cloc.loc : null;
    }

    /**
     * Project a click against the front wall and use that as the "floor", with Z being on the
     * screen vertical. Anchchoring not supported in this layout.
     */
    protected override function pointToLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null,
        anchorAxis :Vector3 = null, clampToRoom :Boolean = true) :ClickLocation
    {
        var p :Point = _parentView.globalToLocal(new Point(stageX, stageY));
        var cloc :ClickLocation =
            _metrics.screenToWallProjection(p.x, p.y, ClickLocation.FRONT_WALL);

        if (clampToRoom) {
            clampClickLocation(cloc);
        }

        cloc.click = ClickLocation.FLOOR;
        cloc.loc.z = cloc.loc.y;
        cloc.loc.y = 0;
        return cloc;
    }

    // from interface RoomLayout
    public override function updateScreenLocation (target :RoomElement, offset :Point = null) :void
    {
        var loc :MsoyLocation = target.getLocation();
        offset = (offset != null) ? offset : NO_OFFSET;

        // simply position them at their straightforward pixel location, default scale.
        target.setScreenLocation(loc.x * _metrics.sceneWidth - offset.x,
           ((1 - loc.z) * _metrics.sceneHeight) - offset.y, getDecorScale(target));

        adjustZOrder(target as DisplayObject);
    }

    // from interface RoomLayout
    public override function recommendedChatHeight () :Number
    {
        // horizon doesn't matter - just fill up the lower quarter
        return 0.25;
    }
}
}
