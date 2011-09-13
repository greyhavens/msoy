//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.all.VizMemberName;

/**
 * A track with some extra historical data.
 */
public class RecentTrack extends Track
{
    /** Name of the DJ who played this track, even if they've left the room. */
    public VizMemberName dj;

    /** How this track was rated by listeners. */
    public int rating;

    @Override
    public Comparable<?> getKey ()
    {
        return order;
    }
}
