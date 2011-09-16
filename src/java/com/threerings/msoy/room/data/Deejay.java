//
// $Id$

package com.threerings.msoy.room.data;

import com.google.common.primitives.Longs;

import com.threerings.presents.dobj.DSet;

/**
 * A player who has stepped up to DJ.
 */
public class Deejay
    implements Comparable<Deejay>, DSet.Entry
{
    /** The DJ's memberId. */
    public int memberId;

    /** A timestamp of when they started DJ-ing. Used for sorting. */
    public long startedAt;

    /** The rating of their last song. */
    public int lastRating;

    public int compareTo (Deejay that)
    {
        return Longs.compare(this.startedAt, that.startedAt);
    }

    public Comparable<?> getKey ()
    {
        return memberId;
    }
}
