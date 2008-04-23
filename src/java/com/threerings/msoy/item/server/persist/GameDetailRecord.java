//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.web.data.GameDetail;

/**
 * Contains details on a single game "title" which may span multiple versions and therefore
 * multiple item ids. Some day additional information like screenshots and instructions may also be
 * contained in the detail record, but for now it serves simply to track a unique game identifier
 * that is shared by all versions of the same game.
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

    /** The column identifier for the {@link #instructions} field. */
    public static final String INSTRUCTIONS = "instructions";

    /** The qualified column identifier for the {@link #instructions} field. */
    public static final ColumnExp INSTRUCTIONS_C =
        new ColumnExp(GameDetailRecord.class, INSTRUCTIONS);

    /** The column identifier for the {@link #singlePlayerGames} field. */
    public static final String SINGLE_PLAYER_GAMES = "singlePlayerGames";

    /** The qualified column identifier for the {@link #singlePlayerGames} field. */
    public static final ColumnExp SINGLE_PLAYER_GAMES_C =
        new ColumnExp(GameDetailRecord.class, SINGLE_PLAYER_GAMES);

    /** The column identifier for the {@link #singlePlayerMinutes} field. */
    public static final String SINGLE_PLAYER_MINUTES = "singlePlayerMinutes";

    /** The qualified column identifier for the {@link #singlePlayerMinutes} field. */
    public static final ColumnExp SINGLE_PLAYER_MINUTES_C =
        new ColumnExp(GameDetailRecord.class, SINGLE_PLAYER_MINUTES);

    /** The column identifier for the {@link #multiPlayerGames} field. */
    public static final String MULTI_PLAYER_GAMES = "multiPlayerGames";

    /** The qualified column identifier for the {@link #multiPlayerGames} field. */
    public static final ColumnExp MULTI_PLAYER_GAMES_C =
        new ColumnExp(GameDetailRecord.class, MULTI_PLAYER_GAMES);

    /** The column identifier for the {@link #multiPlayerMinutes} field. */
    public static final String MULTI_PLAYER_MINUTES = "multiPlayerMinutes";

    /** The qualified column identifier for the {@link #multiPlayerMinutes} field. */
    public static final ColumnExp MULTI_PLAYER_MINUTES_C =
        new ColumnExp(GameDetailRecord.class, MULTI_PLAYER_MINUTES);

    /** The column identifier for the {@link #payoutFactor} field. */
    public static final String PAYOUT_FACTOR = "payoutFactor";

    /** The qualified column identifier for the {@link #payoutFactor} field. */
    public static final ColumnExp PAYOUT_FACTOR_C =
        new ColumnExp(GameDetailRecord.class, PAYOUT_FACTOR);

    /** The column identifier for the {@link #lastPayoutRecalc} field. */
    public static final String LAST_PAYOUT_RECALC = "lastPayoutRecalc";

    /** The qualified column identifier for the {@link #lastPayoutRecalc} field. */
    public static final ColumnExp LAST_PAYOUT_RECALC_C =
        new ColumnExp(GameDetailRecord.class, LAST_PAYOUT_RECALC);

    /** The column identifier for the {@link #flowSinceLastRecalc} field. */
    public static final String FLOW_SINCE_LAST_RECALC = "flowSinceLastRecalc";

    /** The qualified column identifier for the {@link #flowSinceLastRecalc} field. */
    public static final ColumnExp FLOW_SINCE_LAST_RECALC_C =
        new ColumnExp(GameDetailRecord.class, FLOW_SINCE_LAST_RECALC);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 8;

    /** The default payout factor for newly added games. */
    public static final int DEFAULT_PAYOUT_FACTOR = 128;

    /** The unique identifier for this game. */
    @Id @GeneratedValue(strategy=GenerationType.TABLE, generator="gameId")
    public int gameId;

    /** The canonical game item for this game, which has been listed in the catalog. */
    public int listedItemId;

    /** The mutable item which is edited by the developer(s) working on this game. */
    public int sourceItemId;

    /** The creator supplied instructions for this game. */
    @Column(length=GameDetail.MAX_INSTRUCTIONS_LENGTH, nullable=true)
    public String instructions;

    /** Contains the total number of "player games" accumulated for this game in single player.
     * Each time a game is played to completion, this field is incremented by the number of players
     * in the game. See {@link #singlePlayerMinutes} for a note on extremely popular games. */
    public int singlePlayerGames;

    /** The total number of minutes spent playing this game in single player. Note: if a game
     * becomes close to overflowing this field (>2 billion player minutes), this field and {@link
     * #singlePlayerGames} will no longer be updated. */
    public int singlePlayerMinutes;

    /** Contains the total number of "player games" accumulated for this game in multiplayer. */
    public int multiPlayerGames;

    /** The total number of multiplayer minutes spent playing this game. */
    public int multiPlayerMinutes;

    /** The current payout factor for this game: for standalone games, this is a tuning factor
     * between 0 and 255, for AVRGs it is the amount of flow a player gets for a 100% quest. */
    @Column(defaultValue=(""+DEFAULT_PAYOUT_FACTOR))
    public int payoutFactor;

    /** The value of {@link #singlePlayerMinutes} + {@link #multiPlayerMinutes} when we last
     * recalculated the payout factor. */
    public int lastPayoutRecalc;

    /** The amount of flow awarded since our last payout recalculation. */
    public int flowSinceLastRecalc;

    /**
     * Returns the current payout factor for this game, in [0, 1).
     */
    public float getPayoutFactor ()
    {
        return payoutFactor / 256f;
    }

    /**
     * Called to update an in-memory record after we update our record in the database. This is
     * needed so that multiple games don't stomp on one another if they end at the same time.
     */
    public void noteGamePlayed (int playerGames, int playerMins, int flowAwarded, int newFactor)
    {
        if (playerGames > 1) {
            singlePlayerGames += playerGames;
            singlePlayerMinutes += playerMins;
        } else {
            multiPlayerGames += playerGames;
            multiPlayerMinutes += playerMins;
        }
        if (newFactor == 0) {
            flowSinceLastRecalc += flowAwarded;
        } else {
            payoutFactor = newFactor;
            flowSinceLastRecalc = 0;
            lastPayoutRecalc = singlePlayerMinutes + multiPlayerMinutes;
        }
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public GameDetail toGameDetail ()
    {
        GameDetail detail = new GameDetail();
        detail.gameId = gameId;
        detail.instructions = instructions;
        detail.singlePlayerGames = singlePlayerGames;
        detail.singlePlayerMinutes = singlePlayerMinutes;
        detail.multiPlayerGames = multiPlayerGames;
        detail.multiPlayerMinutes = multiPlayerMinutes;
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
