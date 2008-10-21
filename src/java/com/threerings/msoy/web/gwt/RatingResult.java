//
// $Id$

package com.threerings.msoy.web.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Represents a catalog listing of an item.
 */
public class RatingResult
    implements IsSerializable
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
