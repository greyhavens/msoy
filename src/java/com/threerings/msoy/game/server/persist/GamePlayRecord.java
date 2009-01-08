//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Notes information on a played game.
 */
@Entity
public class GamePlayRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GamePlayRecord> _R = GamePlayRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp RECORDED = colexp(_R, "recorded");
    public static final ColumnExp MULTI_PLAYER = colexp(_R, "multiPlayer");
    public static final ColumnExp PLAYER_GAMES = colexp(_R, "playerGames");
    public static final ColumnExp PLAYER_MINS = colexp(_R, "playerMins");
    public static final ColumnExp FLOW_AWARDED = colexp(_R, "flowAwarded");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The game that was played. */
    @Index public int gameId;

    /** The time at which the gameplay was recorded. */
    @Index public Timestamp recorded;

    /** Whether or not this gameplay session was single or multiplayer. */
    public boolean multiPlayer;

    /** The number of player games accumulated by this gameplay (the number of players). */
    public int playerGames;

    /** The total number of player minutes accumulated by this gameplay (for all players). */
    public int playerMins;

    /** The total flow awarded for this gameplay (to all players). */
    public int flowAwarded;

}
