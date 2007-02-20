//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains a per-member-and-game log of awarded flow.
 */
@Entity
public class GameFlowGrantLogRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String GAME_ID = "gameId";
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameFlowGrantLogRecord.class, GAME_ID);
    public static final String MEMBER_ID = "memberId";
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(GameFlowGrantLogRecord.class, MEMBER_ID);
    public static final String AMOUNT = "amount";
    public static final ColumnExp AMOUNT_C = new ColumnExp(GameFlowGrantLogRecord.class, AMOUNT);

    /** The id of the game for which we're logging flow. */
    public int gameId;

    /** The id of the member whose flow we're logging. */
    public int memberId;

    /** The amount of flow that's been awarded, normalized into the range 0 to 255. */
    public int amount;
}
