//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Updated rating information in response to the user rating something.
 */
public class RatingResult
    implements Streamable, IsSerializable
{
    /** The target's current rating. */
    public float rating;

    /** The number of players who have rated the target. */
    public int ratingCount;

    public RatingResult (float rating, int ratingCount)
    {
        this.rating = rating;
        this.ratingCount = ratingCount;
    }

    /** Keeps GWT hap-hap-happy. */
    public RatingResult () { }
}
