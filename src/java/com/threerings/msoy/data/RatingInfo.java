//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.Streamable;

/** The rating summary of some rateable thing. */
public class RatingInfo
    implements Streamable
{
    /** The average rating between all users. */
    public float averageRating;

    /** The number of ratings that make up the average. */
    public int count;

    /** The rating assigned by this user, or 0 if none. */
    public float myRating;
}
