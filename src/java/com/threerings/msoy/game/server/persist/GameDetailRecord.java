//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.game.gwt.GameDetail;

/**
 * Contains details on a single game "title" including the development and published game item ids
 * and other metrics.
 */
@Entity
@TableGenerator(name="gameId", pkColumnValue="GAME_ID")
public class GameDetailRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameDetailRecord.class, GAME_ID);

    /** The column identifier for the {@link #listedItemId} field. */
    public static final String LISTED_ITEM_ID = "listedItemId";

    /** The qualified column identifier for the {@link #listedItemId} field. */
    public static final ColumnExp LISTED_ITEM_ID_C =
        new ColumnExp(GameDetailRecord.class, LISTED_ITEM_ID);

    /** The column identifier for the {@link #sourceItemId} field. */
    public static final String SOURCE_ITEM_ID = "sourceItemId";

    /** The qualified column identifier for the {@link #sourceItemId} field. */
    public static final ColumnExp SOURCE_ITEM_ID_C =
        new ColumnExp(GameDetailRecord.class, SOURCE_ITEM_ID);

    /** The column identifier for the {@link #gamesPlayed} field. */
    public static final String GAMES_PLAYED = "gamesPlayed";

    /** The qualified column identifier for the {@link #gamesPlayed} field. */
    public static final ColumnExp GAMES_PLAYED_C =
        new ColumnExp(GameDetailRecord.class, GAMES_PLAYED);

    /** The column identifier for the {@link #avgSingleDuration} field. */
    public static final String AVG_SINGLE_DURATION = "avgSingleDuration";

    /** The qualified column identifier for the {@link #avgSingleDuration} field. */
    public static final ColumnExp AVG_SINGLE_DURATION_C =
        new ColumnExp(GameDetailRecord.class, AVG_SINGLE_DURATION);

    /** The column identifier for the {@link #avgMultiDuration} field. */
    public static final String AVG_MULTI_DURATION = "avgMultiDuration";

    /** The qualified column identifier for the {@link #avgMultiDuration} field. */
    public static final ColumnExp AVG_MULTI_DURATION_C =
        new ColumnExp(GameDetailRecord.class, AVG_MULTI_DURATION);

    /** The column identifier for the {@link #payoutFactor} field. */
    public static final String PAYOUT_FACTOR = "payoutFactor";

    /** The qualified column identifier for the {@link #payoutFactor} field. */
    public static final ColumnExp PAYOUT_FACTOR_C =
        new ColumnExp(GameDetailRecord.class, PAYOUT_FACTOR);

    /** The column identifier for the {@link #flowToNextRecalc} field. */
    public static final String FLOW_TO_NEXT_RECALC = "flowToNextRecalc";

    /** The qualified column identifier for the {@link #flowToNextRecalc} field. */
    public static final ColumnExp FLOW_TO_NEXT_RECALC_C =
        new ColumnExp(GameDetailRecord.class, FLOW_TO_NEXT_RECALC);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 10;

    /** The default payout factor for newly added games. */
    public static final int DEFAULT_PAYOUT_FACTOR = 128;

    /** The quantity of flow to be awarded before our first recalc. */
    public static final int INITIAL_RECALC_FLOW = 6000;

    /** The unique identifier for this game. */
    @Id @GeneratedValue(strategy=GenerationType.TABLE, generator="gameId")
    public int gameId;

    /** The canonical game item for this game, which has been listed in the catalog. */
    public int listedItemId;

    /** The mutable item which is edited by the developer(s) working on this game. */
    public int sourceItemId;

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
    public GameDetail toGameDetail ()
    {
        GameDetail detail = new GameDetail();
        detail.gameId = gameId;
        detail.sourceItemId = sourceItemId;
        detail.averageDuration = getAverageDuration();
        detail.gamesPlayed = gamesPlayed;
        return detail;
    }

    /**
     * Returns the reported average duration of this game in seconds (greater of single and multi).
     */
    public int getAverageDuration ()
    {
        return Math.max(1, Math.max(avgSingleDuration, avgMultiDuration));
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameDetailRecord}
     * with the supplied key values.
     */
    public static Key<GameDetailRecord> getKey (int gameId)
    {
        return new Key<GameDetailRecord>(
                GameDetailRecord.class,
                new String[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
