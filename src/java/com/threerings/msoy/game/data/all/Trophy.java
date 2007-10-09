//
// $Id$

package com.threerings.msoy.game.data.all;

import com.threerings.io.SimpleStreamableObject;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains information on a trophy held by a player.
 */
public class Trophy extends SimpleStreamableObject
    implements Comparable, IsSerializable
{
    /** The name of the trophy. */
    public String name;

    /** The media for the trophy image. */
    public MediaDesc trophyMedia;

    /** When this trophy was earned. */
    public long whenEarned;

    // from interface Comparable
    public int compareTo (Object other)
    {
        long otherEarned = ((Trophy)other).whenEarned;
        // avoid int and/or long overflow
        return (whenEarned == otherEarned) ? 0 : ((whenEarned > otherEarned) ? -1 : 1);
    }
}
