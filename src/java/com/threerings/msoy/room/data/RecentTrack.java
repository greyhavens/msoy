//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.item.data.all.Audio;

/**
 * A track with some extra historical data.
 */
public class RecentTrack extends Track
{
    /** Name of the DJ who played this track, even if they've left the room. */
    public VizMemberName deejay;

    @Override
    public Comparable<?> getKey ()
    {
        return order;
    }
}
