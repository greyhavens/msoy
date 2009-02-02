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
    /** The sum of all ratings this target has received. */
    public int ratingSum;

    /** The number of players who have rated the target. */
    public int ratingCount;

    public RatingResult (int ratingSum, int ratingCount)
    {
        this.ratingSum = ratingSum;
        this.ratingCount = ratingCount;
    }
    
    public float getRating ()
    {
        return (ratingCount > 0) ? (float) ratingSum / ratingCount : 0.0f;
    }

    /** Keeps GWT hap-hap-happy. */
    public RatingResult () { }
}
