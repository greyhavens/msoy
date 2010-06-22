//
// $Id: RatingResult.java 14622 2009-02-02 15:25:32Z zell $

package com.threerings.msoy.data.all;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Updated rating information in response to the user rating something.
 */
public class RatingHistoryResult
    implements Streamable, IsSerializable
{
    public static class RatingHistoryEntry implements Streamable, IsSerializable
    {
        public MemberName member;
        public byte rating;
        public Date timestamp;

        /** Deserializing constructor. */
        public RatingHistoryEntry () { }

        /** Create a new {@link RatingHistoryEntry) with the given data. */
        public RatingHistoryEntry (MemberName member, byte rating, Date timestamp)
        {
            this.member = member;
            this.rating = rating;
            this.timestamp = timestamp;
        }
    }

    public List<RatingHistoryEntry> ratings;

    /** Deserializing constructor. */
    public RatingHistoryResult () { }

    /** Create a new {@link RatingHistoryResult} with the given ratings. */
    public RatingHistoryResult (List<RatingHistoryEntry> ratings)
    {
        this.ratings = ratings;
    }
}
