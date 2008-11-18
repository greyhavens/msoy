package com.threerings.msoy.world.tour.data;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.RatingInfo;

/** Represents a destination along the tour. */
public class TourStop
    implements Streamable
{
    public int sceneId;
    public RatingInfo rating;
}
