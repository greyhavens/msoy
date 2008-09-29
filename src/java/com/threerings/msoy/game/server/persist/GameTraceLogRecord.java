//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Entity(indices={
    @Index(name="ixGameId", fields={ GameTraceLogRecord.GAME_ID })
})
public class GameTraceLogRecord
    extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #logId} field. */
    public static final String LOG_ID = "logId";

    /** The qualified column identifier for the {@link #logId} field. */
    public static final ColumnExp LOG_ID_C =
        new ColumnExp(GameTraceLogRecord.class, LOG_ID);

    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameTraceLogRecord.class, GAME_ID);

    /** The column identifier for the {@link #recorded} field. */
    public static final String RECORDED = "recorded";

    /** The qualified column identifier for the {@link #recorded} field. */
    public static final ColumnExp RECORDED_C =
        new ColumnExp(GameTraceLogRecord.class, RECORDED);

    /** The column identifier for the {@link #logData} field. */
    public static final String LOG_DATA = "logData";

    /** The qualified column identifier for the {@link #logData} field. */
    public static final ColumnExp LOG_DATA_C =
        new ColumnExp(GameTraceLogRecord.class, LOG_DATA);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /**
     * Returns the number of bytes that can be put into a log data row.
     */
    public static int getMaximumLogLength ()
    {
        // Dig out of annotations
        if (_logLength == null) {
            try {
                Field logData = GameTraceLogRecord.class.getDeclaredField(LOG_DATA);
                Column column = logData.getAnnotation(Column.class);
                _logLength = column.length();

            } catch (NoSuchFieldException nsfe) {
                // stop the server so this will get fixed
                throw new Error("logData field removed");
            }
        }
        return _logLength;
    }
    
    /** The primary key of this log record. */
    @Id
    @GeneratedValue
    public int logId;

    /** The id of the game whose logs we're recording. */
    public int gameId;

    /** The time at which these logs were recorded. */
    public Timestamp recorded;

    /** The log data in question. */
    @Column(length=65535)
    public String logData;

    public GameTraceLogRecord ()
    {
    }

    public GameTraceLogRecord (int gameId, String logData)
    {
        this.gameId = gameId;
        this.recorded = new Timestamp(System.currentTimeMillis());
        this.logData = logData;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameTraceLogRecord}
     * with the supplied key values.
     */
    public static Key<GameTraceLogRecord> getKey (int logId)
    {
        return new Key<GameTraceLogRecord>(
                GameTraceLogRecord.class,
                new String[] { LOG_ID },
                new Comparable[] { logId });
    }
    // AUTO-GENERATED: METHODS END
    
    protected static Integer _logLength;
}
