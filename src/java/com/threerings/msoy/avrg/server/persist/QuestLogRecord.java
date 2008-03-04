//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Maintains a per-member-and-game log of awarded flow.
 */
@Entity(indices={
    @Index(name="ixGame", fields={ QuestLogRecord.GAME_ID })
})
public class QuestLogRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(QuestLogRecord.class, GAME_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(QuestLogRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #questId} field. */
    public static final String QUEST_ID = "questId";

    /** The qualified column identifier for the {@link #questId} field. */
    public static final ColumnExp QUEST_ID_C =
        new ColumnExp(QuestLogRecord.class, QUEST_ID);

    /** The column identifier for the {@link #payoutFactor} field. */
    public static final String PAYOUT_FACTOR = "payoutFactor";

    /** The qualified column identifier for the {@link #payoutFactor} field. */
    public static final ColumnExp PAYOUT_FACTOR_C =
        new ColumnExp(QuestLogRecord.class, PAYOUT_FACTOR);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The id of the game for which we're logging flow. */
    public int gameId;

    /** The id of the member who just completed the quest. */
    public int memberId;
    
    /** The id of the quest that was just completed. */
    public String questId;

    /** The payout factor that was associated with the quest copmletion. */
    public float payoutFactor;

    /** Empty constructor for deserializing. */
    public QuestLogRecord ()
    {
    }
    
    /** Create a new QuestLogRecord configured with the supplied values. */
    public QuestLogRecord (int gameId, int memberId, String questId, float payoutFactor)
    {
        this.gameId = gameId;
        this.memberId = memberId;
        this.questId = questId;
        this.payoutFactor = payoutFactor;
    }
}
