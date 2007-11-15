//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;

import com.threerings.util.Log;
import com.threerings.util.Util;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Handles sprites for actors (members and pets).
 */
public class ActorSprite extends OccupantSprite
{
    /**
     * Creates an actor sprite for the supplied actor.
     */
    public function ActorSprite (occInfo :ActorInfo)
    {
        super(occInfo);
    }

    /**
     * Update the actor's state. Called by user code when it wants to change the actor's state.
     */
    public function setState (state :String) :void
    {
        if (_ident != null && (parent is RoomView) && validateUserData(state, null)) {
            (parent as RoomView).getRoomController().setActorState(
                _ident, _occInfo.bodyOid, state);
        }
    }

    /**
     * Get the actor's current state.  Called by user code.
     */
    public function getState () :String
    {
        return getActorInfo().getState();
    }

    /**
     * Returns the actor info for this actor.
     */
    public function getActorInfo () :ActorInfo
    {
        return (_occInfo as ActorInfo);
    }

    // from OccupantSprite
    override public function getDesc () :String
    {
        return "m.actor";
    }

    // from OccupantSprite
    override public function setOccupantInfo (newInfo :OccupantInfo) :void
    {
        // note whether our state changed (we don't consider it a change if the old info was null,
        // as getting our initial state isn't a "change"); this is reason to have a special
        // state-changed dobj event
        var stateChanged :Boolean =
            (_occInfo != null && !Util.equals((_occInfo as ActorInfo).getState(),
                                              (newInfo as ActorInfo).getState()));

        super.setOccupantInfo(newInfo);

        // if the state has changed, dispatch an event 
        if (stateChanged) {
            callUserCode("stateSet_v1", getActorInfo().getState());
        }
    }

    // from OccupantSprite
    override public function toString () :String
    {
        return "ActorSprite[" + _occInfo.username + " (oid=" + _occInfo.bodyOid + ")]";
    }

    // from OccupantSprite
    override protected function configureDisplay (newInfo :OccupantInfo) :Boolean
    {
        var newMedia :MediaDesc = (newInfo as ActorInfo).getMedia();
        if (!newMedia.equals(_desc)) {
            setItemIdent((newInfo as ActorInfo).getItemIdent());
            setMediaDesc(newMedia);
            return true;
        }
        return false;
    }

    // from OccupantSprite
    override protected function appearanceChanged () :void
    {
        var locArray :Array = [ _loc.x, _loc.y, _loc.z ];
        if (hasUserCode("appearanceChanged_v2")) {
            callUserCode("appearanceChanged_v2", locArray, _loc.orient, isMoving(), isIdle());
        } else {
            callUserCode("appearanceChanged_v1", locArray, _loc.orient, isMoving());
        }
    }

    // from MsoySprite
    override protected function createBackend () :EntityBackend
    {
        return new ActorBackend();
    }

    /**
     * Update the actor's scene location. Called by our backend in response to a request from
     * usercode.
     */
    internal function setLocationFromUser (x :Number, y :Number, z: Number, orient :Number) :void
    {
        if (_ident != null && parent is RoomView) {
            (parent as RoomView).getRoomController().requestMove(
                _ident, new MsoyLocation(x, y, z, orient));
        }
    }

    /**
     * Update the actor's orientation.
     * Called by user code when it wants to change the actor's scene location.
     */
    internal function setOrientationFromUser (orient :Number) :void
    {
        // TODO
        Log.getLog(this).debug("user-set orientation is currently TODO.");
    }

    internal function setMoveSpeedFromUser (speed :Number) :void
    {
        if (!isNaN(speed)) {
            // don't worry, it'll be bounded by the minimum at the appropriate place
            _moveSpeed = speed;
        }
    }
}
}
