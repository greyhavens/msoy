//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Contains information on an occupant of a room (member, pet, monster, who knows?).
 */
public class ActorInfo extends OccupantInfo
{
    /**
     * Returns the item that was used to create this occupant.
     */
    public function getItemIdent () :ItemIdent
    {
        return _ident;
    }    

    // from OccupantInfo
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _ident = (ins.readObject() as ItemIdent);
    }

    /** The item identifier that uniquely identifies this occupant. This is either the identifier
     * of the avatar or pet item that makes up the actor, or a specially constructed Item.OCCUPANT
     * identifier. */
    protected var _ident :ItemIdent;
}
}
