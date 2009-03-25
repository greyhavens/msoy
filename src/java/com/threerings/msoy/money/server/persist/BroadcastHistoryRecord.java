//
// $Id$

package com.threerings.msoy.money.server.persist;

import java.util.Date;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.money.gwt.BroadcastHistory;

/**
 * Data recorded when a member pays for and sends a global message to the Whirled.
 */
@Entity
public class BroadcastHistoryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BroadcastHistoryRecord> _R = BroadcastHistoryRecord.class;
    public static final ColumnExp TIME_SENT = colexp(_R, "timeSent");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp BARS_PAID = colexp(_R, "barsPaid");
    public static final ColumnExp MESSAGE = colexp(_R, "message");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** Time the message was posted. */
    @Id public Timestamp timeSent;

    /** Member who posted it. */
    public int memberId;

    /** Number of bars paid for it. */
    public int barsPaid;

    /** The content of the message. */
    public String message;

    /**
     * Converts this broadcast history record into a runtime class.
     */
    public BroadcastHistory toBroadcastHistory ()
    {
        BroadcastHistory hist = new BroadcastHistory();
        hist.barsPaid = barsPaid;
        hist.memberId = memberId;
        hist.message = message;
        hist.timeSent = new Date(timeSent.getTime());
        return hist;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BroadcastHistoryRecord}
     * with the supplied key values.
     */
    public static Key<BroadcastHistoryRecord> getKey (Timestamp timeSent)
    {
        return new Key<BroadcastHistoryRecord>(
                BroadcastHistoryRecord.class,
                new ColumnExp[] { TIME_SENT },
                new Comparable[] { timeSent });
    }
    // AUTO-GENERATED: METHODS END
}
