//
// $Id$

package com.threerings.msoy.room.data;

import com.google.common.primitives.Ints;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.item.data.all.Audio;

/**
 * An audio track that was added to a room by a DJ.
 */
public class Track
    implements DSet.Entry, Comparable<Track>
{
    public Audio audio;

    /** How this track has been rated by listeners. */
    public int rating;

    /** Used for list sorting. */
    public int order;

    public Comparable<?> getKey ()
    {
        return audio.getKey();
    }

    public int compareTo (Track that)
    {
        // Most recent tracks first
        return Ints.compare(this.order, that.order);
    }
}
