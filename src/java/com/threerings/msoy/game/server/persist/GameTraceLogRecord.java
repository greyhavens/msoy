//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.lang.reflect.Field;
import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

@Entity
public class GameTraceLogRecord
    extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameTraceLogRecord> _R = GameTraceLogRecord.class;
    public static final ColumnExp LOG_ID = colexp(_R, "logId");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp RECORDED = colexp(_R, "recorded");
    public static final ColumnExp LOG_DATA = colexp(_R, "logData");
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
                Field logData = GameTraceLogRecord.class.getDeclaredField(LOG_DATA.name);
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
    @Index(name="ixGameId")
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
                new ColumnExp[] { LOG_ID },
                new Comparable[] { logId });
    }
    // AUTO-GENERATED: METHODS END

    protected static Integer _logLength;
}
