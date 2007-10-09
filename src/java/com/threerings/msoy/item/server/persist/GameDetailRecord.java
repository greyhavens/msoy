//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.data.GameDetail;

/**
 * Contains details on a single game "title" which may span multiple versions and therefore
 * multiple item ids. Some day additional information like screenshots and instructions may also be
 * contained in the detail record, but for now it serves simply to track a unique game identifier
 * that is shared by all versions of the same game.
 */
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

    /** The column identifier for the {@link #playerGames} field. */
    public static final String PLAYER_GAMES = "playerGames";

    /** The qualified column identifier for the {@link #playerGames} field. */
    public static final ColumnExp PLAYER_GAMES_C =
        new ColumnExp(GameDetailRecord.class, PLAYER_GAMES);

    /** The column identifier for the {@link #playerMinutes} field. */
    public static final String PLAYER_MINUTES = "playerMinutes";

    /** The qualified column identifier for the {@link #playerMinutes} field. */
    public static final ColumnExp PLAYER_MINUTES_C =
        new ColumnExp(GameDetailRecord.class, PLAYER_MINUTES);

    /** The column identifier for the {@link #abuseFactor} field. */
    public static final String ABUSE_FACTOR = "abuseFactor";

    /** The qualified column identifier for the {@link #abuseFactor} field. */
    public static final ColumnExp ABUSE_FACTOR_C =
        new ColumnExp(GameDetailRecord.class, ABUSE_FACTOR);

    /** The column identifier for the {@link #lastAbuseRecalc} field. */
    public static final String LAST_ABUSE_RECALC = "lastAbuseRecalc";

    /** The qualified column identifier for the {@link #lastAbuseRecalc} field. */
    public static final ColumnExp LAST_ABUSE_RECALC_C =
        new ColumnExp(GameDetailRecord.class, LAST_ABUSE_RECALC);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The default abuse factor for newly added games. */
    public static final int DEFAULT_ABUSE_FACTOR = 100;

    /** The unique identifier for this game. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY, initialValue=100)
    public int gameId;

    /** The canonical game item for this game, which has been listed in the catalog. */
    public int listedItemId;

    /** The mutable item which is edited by the developer(s) working on this game. */
    public int sourceItemId;

    /** Contains the total number of "player games" accumulated for this game. Each time a game is
     * played to completion, this field is incremented by the number of players in the game. See
     * {@link #playerMinutes} for a note on extremely popular games. */
    public int playerGames;

    /** The total number of minutes spent playing this game. Note: if a game becomes close to
     * overflowing this field (>2 billion player minutes), this field and {@link #playerGames} will
     * no longer be updated. */
    public int playerMinutes;

    /** The current abuse factor, from 0 to 255. */
    @Column(defaultValue=(""+DEFAULT_ABUSE_FACTOR))
    public int abuseFactor;

    /** The value of {@link #playerMinutes} when we last recalculated the abuse factor. */
    public int lastAbuseRecalc;

    /**
     * Return the current anti-abuse factor for this game, in [0, 1).
     */
    public float getAntiAbuseFactor ()
    {
        return abuseFactor / 256f;
    }

    /**
     * Returns true if we should recaculate our abuse factor based on the supplied additional
     * player minutes and the specified recalculation interval.
     */
    public boolean shouldRecalcAbuse (int playerMins, int recalcMinutes)
    {
        return (playerMinutes - lastAbuseRecalc) + playerMins > recalcMinutes;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public GameDetail toGameDetail ()
    {
        GameDetail detail = new GameDetail();
        detail.gameId = gameId;
        detail.playerGames = playerGames;
        detail.playerMinutes = playerMinutes;
        detail.abuseFactor = getAntiAbuseFactor();
        detail.lastAbuseRecalc = lastAbuseRecalc;
        return detail;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #GameDetailRecord}
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
