//
// $Id$

package com.threerings.msoy.game.gwt;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a game's score distributions.
 */
public class GameDistribs
    implements IsSerializable
{
    public static class TilerSummary
        implements IsSerializable
    {
        /** The total number of reported scores. */
        public long totalCount;

        /** The counts for each bucket in the score percentiler. */
        public int[] counts;

        /** The score required for each percentile from 0 to 99. */
        public float[] scores;

        /** The highest reported single-player score. */
        public int maxScore;
    }

    /** The id of the game for which we're reporting metrics. */
    public int gameId;

    /** The single-player game mode metrics for this game, mapped by game mode. */
    public Map<Integer, TilerSummary> singleDistribs;

    /** The multi-player game mode metrics for this game, mapped by game mode. */
    public Map<Integer, TilerSummary> multiDistribs;
}
