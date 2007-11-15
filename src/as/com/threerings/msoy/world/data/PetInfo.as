//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

/**
 * Contains published information on a pet in a scene.
 */
public class PetInfo extends ActorInfo
{
    /**
     * Returns the member id of this pet's owner.
     */
    public function getOwnerId () :int
    {
        return _ownerId;
    }

    /**
     * Returns true if this pet is following its owner around the world, false if it is in the room
     * because it has been permanently "let out" in the room.
     */
    public function isFollowing () :Boolean
    {
        return _isFollowing;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _ownerId = ins.readInt();
        _isFollowing = ins.readBoolean();
    }

    protected var _ownerId :int;
    protected var _isFollowing :Boolean;
}
}
