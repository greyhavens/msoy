//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import flash.geom.Point;

import com.threerings.flash.Vector3;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;


/**
 * This class factors out room layout math that converts between 3D room coordinate space
 * and screen coordinates.
 */
public class RoomLayoutFlatworld extends RoomLayoutStandard {

    /** Constructor. */
    public function RoomLayoutFlatworld (view :AbstractRoomView)
    {
        super(view);
    }

    // from interface RoomLayout
    override public function pointToAvatarLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation
    {
        // this version drops everything onto the front plane. 
        var cloc :ClickLocation = pointToFurniLocation(stageX, stageY, anchorPoint, anchorAxis);
        cloc.click = ClickLocation.FLOOR;
        if (anchorPoint == null) {
            cloc.loc.y = 0;
        }
        return cloc;
    }
    
    // from interface RoomLayout
    override public function pointToFurniLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation
    {
        // this version drops everything onto the front plane, completely ignoring constraints.
        // there's no depth at all.

        var p :Point = _parentView.globalToLocal(new Point(stageX, stageY));
        var v :Vector3 = _metrics.screenToWallProjection(p.x, p.y, ClickLocation.FRONT_WALL);

        var cloc :ClickLocation =
            new ClickLocation(ClickLocation.FRONT_WALL, _metrics.toMsoyLocation(v));
        clampClickLocation(cloc);
        return cloc;
    }

    override public function updateScreenLocation (target :RoomElement, offset :Point = null) :void
    {
        var loc :MsoyLocation = target.getLocation();

        offset = (offset != null) ? offset : NO_OFFSET;

        target.setScreenLocation(loc.x * _metrics.sceneWidth - offset.x,
           ((1 - loc.y) * _metrics.sceneHeight) - offset.y, 1);
        adjustZOrder(target as DisplayObject);
    }

    // from interface RoomLayout
    override public function recommendedChatHeight () :Number
    {
        // horizon doesn't matter - just fill up the lower quarter
        return 0.25;
    }

}
}
    
