//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.expression.ColumnExp;

@Computed(shadowOf=GameTraceLogRecord.class)
public class GameTraceLogEnumerationRecord
    extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameTraceLogEnumerationRecord> _R = GameTraceLogEnumerationRecord.class;
    public static final ColumnExp LOG_ID = colexp(_R, "logId");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp RECORDED = colexp(_R, "recorded");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The primary key of this log record. */
    public int logId;

    /** The id of the game whose logs we're recording. */
    public int gameId;

    /** The time at which these logs were recorded. */
    public Timestamp recorded;
}
