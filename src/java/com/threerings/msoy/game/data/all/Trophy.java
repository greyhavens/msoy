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
    /** The game for which this trophy was awarded. */
    public int gameId;

    /** The name of the trophy. */
    public String name;

    /** The media for the trophy image. */
    public MediaDesc trophyMedia;

    /** When this trophy was earned. */
    public Long whenEarned;

    // from interface Comparable
    public int compareTo (Object other)
    {
        return ((Trophy)other).whenEarned.compareTo(whenEarned);
    }
}
