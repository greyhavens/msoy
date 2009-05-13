//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.game.gwt.GameMetrics;

/**
 * Contains metrics on game plays.
 */
public class GameMetricsRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameMetricsRecord> _R = GameMetricsRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp GAMES_PLAYED = colexp(_R, "gamesPlayed");
    public static final ColumnExp AVG_SINGLE_DURATION = colexp(_R, "avgSingleDuration");
    public static final ColumnExp AVG_MULTI_DURATION = colexp(_R, "avgMultiDuration");
    public static final ColumnExp PAYOUT_FACTOR = colexp(_R, "payoutFactor");
    public static final ColumnExp FLOW_TO_NEXT_RECALC = colexp(_R, "flowToNextRecalc");
    public static final ColumnExp LAST_PAYOUT = colexp(_R, "lastPayout");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The default payout factor for newly added games. */
    public static final int DEFAULT_PAYOUT_FACTOR = 128;

    /** The quantity of flow to be awarded before our first recalc. */
    public static final int INITIAL_RECALC_FLOW = 6000;

    /** The id of the game for which we track metrics. */
    @Id public int gameId;

    /** The total number of player games accumulated for this game. */
    public int gamesPlayed;

    /** Our average single player duration (in seconds). */
    public int avgSingleDuration;

    /** Our average multi player duration (in seconds). */
    public int avgMultiDuration;

    /** The current payout factor for this game: for standalone games, this is a tuning factor
     * between 0 and 255, for AVRGs it is the amount of flow a player gets for a 100% quest. */
    public int payoutFactor;

    /** The amount of flow remaining for this game to award before our next payout recalc. */
    public int flowToNextRecalc;

    /** When this game last paid out coins, or null if has never paid out. */
    @Column(nullable=true)
    public Timestamp lastPayout;

    /**
     * Returns the reported average duration of this game in seconds (greater of single and multi).
     */
    public int getAverageDuration ()
    {
        return Math.max(1, Math.max(avgSingleDuration, avgMultiDuration));
    }

    /**
     * Returns the current payout factor for this game, in [0, 1).
     */
    public float getPayoutFactor ()
    {
        return this.payoutFactor / 256f;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public GameMetrics toGameMetrics ()
    {
        GameMetrics metrics = new GameMetrics();
        metrics.gameId = gameId;
        metrics.gamesPlayed = gamesPlayed;
        metrics.averageDuration = getAverageDuration();
        metrics.lastPayout = (lastPayout != null) ? lastPayout.getTime() : 0;
        return metrics;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameMetricsRecord}
     * with the supplied key values.
     */
    public static Key<GameMetricsRecord> getKey (int gameId)
    {
        return new Key<GameMetricsRecord>(
                GameMetricsRecord.class,
                new ColumnExp[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
