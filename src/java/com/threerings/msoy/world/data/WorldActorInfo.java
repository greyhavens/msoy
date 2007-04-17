//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.data.ActorInfo;
import com.threerings.msoy.data.MsoyBodyObject;

/**
 * Contains extra information for an occupant when they are in the virtual world.
 */
public class WorldActorInfo extends ActorInfo
    implements WorldOccupantInfo
{
    /** The current state of the occupant's avatar. */
    public String state;

    /** Used for unserialization. */
    public WorldActorInfo ()
    {
    }

    /**
     * Creates a world actor info record for the specified actor.
     */
    public WorldActorInfo (MsoyBodyObject body, ItemIdent ident, MediaDesc media)
    {
        super(body, ident);

        state = body.avatarState;
        _media = media;
    }

    // from interface WorldOccupantInfo
    public MediaDesc getMedia ()
    {
        return _media;
    }

    // from interface WorldOccupantInfo
    public String getState ()
    {
        return state;
    }

    /** The media that represents this occupant. */
    protected MediaDesc _media;
}
