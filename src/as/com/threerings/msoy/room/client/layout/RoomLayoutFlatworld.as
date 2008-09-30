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

/**
 * This class factors out room layout math that converts between 3D room coordinate space
 * and screen coordinates.
 */
public class RoomLayoutFlatworld extends RoomLayoutStandard
{
    /** Constructor. */
    public function RoomLayoutFlatworld (view :RoomView)
    {
        super(view);
    }

    // from interface RoomLayout
    override public function pointToAvatarLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null,
        anchorAxis :Vector3 = null, clampToRoom :Boolean = true) :ClickLocation
    {
        var cloc :ClickLocation = pointToFurniLocation(
            stageX, stageY, anchorPoint, anchorAxis, clampToRoom);
        // Take the location on the front plane and call it a floor location instead.
        cloc.click = ClickLocation.FLOOR;
        // If the user isn't selecting a custom Y, make it an actual floor location..
        if (anchorPoint == null) {
            cloc.loc.y = 0;
        }
        // set our Z to .5 so that we might go behind some furniture...
        cloc.loc.z = .5;
        return cloc;
    }
    
    // from interface RoomLayout
    override public function pointToFurniLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null,
        anchorAxis :Vector3 = null, clampToRoom :Boolean = true) :ClickLocation
    {
        // this version drops everything onto the front plane, completely ignoring constraints.
        // there's no depth at all.
        var p :Point = _parentView.globalToLocal(new Point(stageX, stageY));
        var cloc :ClickLocation =
            _metrics.screenToWallProjection(p.x, p.y, ClickLocation.FRONT_WALL);

        if (clampToRoom) {
            clampClickLocation(cloc);
        }
        return cloc;
    }

    // from interface RoomLayout
    override public function updateScreenLocation (target :RoomElement, offset :Point = null) :void
    {
        var loc :MsoyLocation = target.getLocation();
        offset = (offset != null) ? offset : NO_OFFSET;

        // simply position them at their straightforward pixel location, default scale.
        target.setScreenLocation(loc.x * _metrics.sceneWidth - offset.x,
           ((1 - loc.y) * _metrics.sceneHeight) - offset.y, getDecorScale(target));
        // z ordering can still be used for layering
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
