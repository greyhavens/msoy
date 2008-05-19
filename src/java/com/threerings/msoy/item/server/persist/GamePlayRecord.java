//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Notes information on a played game.
 */
@Entity(indices={
    @Index(name="gameIdIndex", fields={ GamePlayRecord.GAME_ID } ),
    @Index(name="recordedIndex", fields={ GamePlayRecord.RECORDED } )
})
public class GamePlayRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GamePlayRecord.class, GAME_ID);

    /** The column identifier for the {@link #recorded} field. */
    public static final String RECORDED = "recorded";

    /** The qualified column identifier for the {@link #recorded} field. */
    public static final ColumnExp RECORDED_C =
        new ColumnExp(GamePlayRecord.class, RECORDED);

    /** The column identifier for the {@link #playerGames} field. */
    public static final String PLAYER_GAMES = "playerGames";

    /** The qualified column identifier for the {@link #playerGames} field. */
    public static final ColumnExp PLAYER_GAMES_C =
        new ColumnExp(GamePlayRecord.class, PLAYER_GAMES);

    /** The column identifier for the {@link #playerMins} field. */
    public static final String PLAYER_MINS = "playerMins";

    /** The qualified column identifier for the {@link #playerMins} field. */
    public static final ColumnExp PLAYER_MINS_C =
        new ColumnExp(GamePlayRecord.class, PLAYER_MINS);

    /** The column identifier for the {@link #flowAwarded} field. */
    public static final String FLOW_AWARDED = "flowAwarded";

    /** The qualified column identifier for the {@link #flowAwarded} field. */
    public static final ColumnExp FLOW_AWARDED_C =
        new ColumnExp(GamePlayRecord.class, FLOW_AWARDED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The game that was played. */
    public int gameId;

    /** The time at which the gameplay was recorded. */
    public Timestamp recorded;

    /** The number of player games accumulated by this gameplay (the number of players). */
    public int playerGames;

    /** The total number of player minutes accumulated by this gameplay (for all players). */
    public int playerMins;

    /** The total flow awarded for this gameplay (to all players). */
    public int flowAwarded;

}
