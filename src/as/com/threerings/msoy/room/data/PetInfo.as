//
// $Id$

package com.threerings.msoy.room.data {

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

    // from ActorInfo
    override public function clone () :Object
    {
        var that :PetInfo = super.clone() as PetInfo;
        that._ownerId = this._ownerId;
        return that;
    }

    // from ActorInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _ownerId = ins.readInt();
    }

    protected var _ownerId :int;
}
}
