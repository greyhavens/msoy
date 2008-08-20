//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Computed(shadowOf=GameTraceLogRecord.class)
public class GameTraceLogEnumerationRecord
    extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #logId} field. */
    public static final String LOG_ID = "logId";

    /** The qualified column identifier for the {@link #logId} field. */
    public static final ColumnExp LOG_ID_C =
        new ColumnExp(GameTraceLogEnumerationRecord.class, LOG_ID);

    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameTraceLogEnumerationRecord.class, GAME_ID);

    /** The column identifier for the {@link #recorded} field. */
    public static final String RECORDED = "recorded";

    /** The qualified column identifier for the {@link #recorded} field. */
    public static final ColumnExp RECORDED_C =
        new ColumnExp(GameTraceLogEnumerationRecord.class, RECORDED);
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
