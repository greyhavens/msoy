//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.io.ObjectInputStream;

/**
 * Extends {@link WorldActorInfo} with pet specific bits.
 */
public class WorldPetInfo extends WorldActorInfo
{
    /** The member id of this pet's owner. */
    public var ownerId :int;

    /** Set to true if the pet is following its owner around the world, false if it is in this room
     * because it has been permanently "let out" in this room. */
    public var isFollowing :Boolean;

    // from WorldActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        ownerId = ins.readInt();
        isFollowing = ins.readBoolean();
    }
}
}
