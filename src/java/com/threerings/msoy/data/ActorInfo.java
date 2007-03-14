//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.item.web.ItemIdent;

/**
 * Contains information on an occupant of a room (member, pet, monster, who knows?).
 */
public class ActorInfo extends OccupantInfo
{
    /** Used when unserializing. */
    public ActorInfo ()
    {
    }

    /**
     * Creates an info record for the specified actor.
     */
    public ActorInfo (MsoyBodyObject body, ItemIdent ident)
    {
        super(body);
        _ident = ident;
    }

    /**
     * Returns the item that was used to create this occupant.
     */
    public ItemIdent getItemIdent ()
    {
        return _ident;
    }    

    /** The item identifier that uniquely identifies this occupant. This is either the identifier
     * of the avatar or pet item that makes up the actor, or a specially constructed Item.OCCUPANT
     * identifier. */
    protected ItemIdent _ident;
}
