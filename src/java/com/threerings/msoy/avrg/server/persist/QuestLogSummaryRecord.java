//
// $Id$

package com.threerings.msoy.avrg.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Computed
@Entity
public class QuestLogSummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(QuestLogSummaryRecord.class, GAME_ID);

    /** The column identifier for the {@link #payoutCount} field. */
    public static final String PAYOUT_COUNT = "payoutCount";

    /** The qualified column identifier for the {@link #payoutCount} field. */
    public static final ColumnExp PAYOUT_COUNT_C =
        new ColumnExp(QuestLogSummaryRecord.class, PAYOUT_COUNT);

    /** The column identifier for the {@link #payoutFactorTotal} field. */
    public static final String PAYOUT_FACTOR_TOTAL = "payoutFactorTotal";

    /** The qualified column identifier for the {@link #payoutFactorTotal} field. */
    public static final ColumnExp PAYOUT_FACTOR_TOTAL_C =
        new ColumnExp(QuestLogSummaryRecord.class, PAYOUT_FACTOR_TOTAL);
    // AUTO-GENERATED: FIELDS END

    /** The id of the game that did the flow granting. */
    public int gameId;

    /** The number of payouts in this summary. */
    public int payoutCount;
    
    /** The sum of payout factors this AVRG has granted for this summary. */
    public float payoutFactorTotal;
}
