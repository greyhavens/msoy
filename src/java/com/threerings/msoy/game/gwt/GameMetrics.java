//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Information displayed to the creator of a game.
 */
public class GameMetrics
    implements IsSerializable
{
    /** The id of the game for which we're reporting metrics. */
    public int gameId;

    /** The total number of reported single-player scores. */
    public long singleTotalCount;

    /** The counts for each bucket in the single-player score percentiler. */
    public int[] singleCounts;

    /** The single-player score required for each percentile from 0 to 99. */
    public float[] singleScores;

    /** The highest reported single-player score. */
    public int singleMaxScore;

    /** The total number of reported multiplayer scores. */
    public long multiTotalCount;

    /** The counts for each bucket in the multiplayer score percentiler. */
    public int[] multiCounts;

    /** The multiplayer score required for each percentile from 0 to 99. */
    public float[] multiScores;

    /** The highest reported multi player score. */
    public int multiMaxScore;
}
