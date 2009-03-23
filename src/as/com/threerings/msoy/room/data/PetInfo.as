//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.io.ObjectInputStream;

/**
 * Contains published information on a pet in a scene.
 */
public class PetInfo extends ActorInfo
{
    // statically reference classes we require
    PetName;

//    /**
//     * Returns the member id of this pet's owner.
//     */
//    public function getOwnerId () :int
//    {
//        return PetName(username).getOwnerId();
//    }

    // from ActorInfo
    override public function clone () :Object
    {
        var that :PetInfo = super.clone() as PetInfo;
        // presently: nothing else to copy
        return that;
    }
}
}
