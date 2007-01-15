//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.data.ActorInfo;

/**
 * Contains extra information for an occupant when they are in the virtual world.
 */
public class WorldActorInfo extends ActorInfo
    implements WorldOccupantInfo
{
    /** Used for unserialization. */
    public WorldActorInfo ()
    {
    }

    // from interface WorldOccupantInfo
    public MediaDesc getMedia ()
    {
        return _media;
    }

    /**
     * Creates a world actor info record for the specified actor.
     */
    protected WorldActorInfo (BodyObject body, ItemIdent ident, MediaDesc media)
    {
        super(body, ident);
        _media = media;
    }

    /** The media that represents this occupant. */
    protected MediaDesc _media;
}
