//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
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
    public static final Class<GameDetailRecord> _R = GameDetailRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp LISTED_ITEM_ID = colexp(_R, "listedItemId");
    public static final ColumnExp SOURCE_ITEM_ID = colexp(_R, "sourceItemId");
    public static final ColumnExp GAMES_PLAYED = colexp(_R, "gamesPlayed");
    public static final ColumnExp AVG_SINGLE_DURATION = colexp(_R, "avgSingleDuration");
    public static final ColumnExp AVG_MULTI_DURATION = colexp(_R, "avgMultiDuration");
    public static final ColumnExp PAYOUT_FACTOR = colexp(_R, "payoutFactor");
    public static final ColumnExp FLOW_TO_NEXT_RECALC = colexp(_R, "flowToNextRecalc");
    public static final ColumnExp LAST_PAYOUT = colexp(_R, "lastPayout");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 11;

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

    /** When this game last paid out coins, or null if has never paid out. */
    @Column(nullable=true)
    public Timestamp lastPayout;

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
        detail.lastPayout = (lastPayout != null) ? lastPayout.getTime() : 0;
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
                new ColumnExp[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
