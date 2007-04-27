//
// $Id$

package com.threerings.msoy.world.client {

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
        return pointToFurniLocation(stageX, stageY, anchorPoint, anchorAxis);
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

    // from interface RoomLayout
    override public function recommendedChatHeight () :Number
    {
        // horizon doesn't matter - just fill up the lower quarter
        return 0.25;
    }

    // from superclass RoomLayoutStandard
    override protected function getZOfChildAt (index :int) :Number
    {
        // in this version, we use vertical position for z-ordering -
        // higher items in the back, lower up front
        
        var re :RoomElement = RoomElement(_parentView.getChildAt(index));

        // we multiply the layer constant by 1000 to spread out the z values that
        // normally lie in the 0 -> 1 range.
        return re.getLocation().y + (1000 * re.getRoomLayer());
    }
}
}
    
