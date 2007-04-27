//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Point;
    
import com.threerings.flash.Vector3;
import com.threerings.msoy.world.client.ClickLocation;
import com.threerings.msoy.world.data.DecorData;


/**
 * Interface for classes that encapsulate different kinds of layout math for rooms.
 */
public interface RoomLayout {

    /**
     * Updates the room layout object with fresh data.
     */
    function update (data :DecorData) :void;

    /**
     * Get the room metrics used in the layout.
     */
    function get metrics () :RoomMetrics;

    /**
     * Finds a room location for avatar movement, based on a screen coordinate.
     *
     * Movement locations can be constrained in particular ways; for example, only allowing
     * movement on the floor or the ceiling.
     *
     *   @param stageX       Mouse x position, in stage coordinate space
     *   @param stageY       Mouse y position, in stage coordinate space
     *   @param anchorPoint  If present, constraints movement to an axis-aligned line passing
     *                       through the anchorPoint. The anchor can either be a Point
     *                       containing screen (stage) coordinates, or an MsoyLocation.
     *   @param anchorAxis   The axis of the constraint line, as one of the following constants:
     *                       RoomMetrics.{N_UP|N_RIGHT|N_AWAY}. This parameter is only
     *                       used in conjunction with anchorPoint.
     *
     * @returns A ClickLocation valid for this room layout, or null if no valid location was found.
     *
     */
    function pointToAvatarLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation;
    
    /**
     * Finds a room location for furni placement.
     *
     * Furni locations are not usually constrained in the same way as avatars can be.
     *
     *   @param stageX       Mouse x position, in stage coordinate space
     *   @param stageY       Mouse y position, in stage coordinate space
     *   @param anchorPoint  If present, constraints movement to an axis-aligned line passing
     *                       through the anchorPoint. The anchor can either be a Point
     *                       containing screen (stage) coordinates, or an MsoyLocation.
     *   @param anchorAxis   The axis of the constraint line, as one of the following constants:
     *                       RoomMetrics.{N_UP|N_RIGHT|N_AWAY}. This parameter is only
     *                       used in conjunction with anchorPoint.
     *
     * @returns A ClickLocation valid for this room layout, or null if no valid location was found.
     *
     */
    function pointToFurniLocation (
        stageX :Number, stageY :Number, anchorPoint :Object = null, anchorAxis :Vector3 = null)
        :ClickLocation;
    
    /**
     * Given a position in room space, this function finds its projection in screen space, and
     * updates the DisplayObject's position and scale appropriately. If the display object
     * participates in screen layout (and most of them do, with the notable exception of decor),
     * it will also ask the room view to recalculate the object's z-ordering.
     *
     * @param target object to be updated
     * @param offset optional Point argument that, if not null, will be used to shift
     *        the object left and up by the specified x and y amounts.
     */
    function updateScreenLocation (target :RoomElement, offset :Point = null) :void;

}
}
