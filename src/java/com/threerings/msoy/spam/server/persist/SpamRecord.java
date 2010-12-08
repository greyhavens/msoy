//
// $Id$

package com.threerings.msoy.spam.server.persist;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains persistent data related to how and how often we send members unsolicited email.
 */
@Entity
public class SpamRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<SpamRecord> _R = SpamRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Date> RETENTION_SENT = colexp(_R, "retentionSent");
    public static final ColumnExp<Integer> RETENTION_STATUS = colexp(_R, "retentionStatus");
    public static final ColumnExp<Integer> RETENTION_COUNT = colexp(_R, "retentionCount");
    public static final ColumnExp<Integer> RETENTION_COUNT_SINCE_LOGIN = colexp(_R, "retentionCountSinceLogin");
    // AUTO-GENERATED: FIELDS END

    /** Member id that is the target of the emails. */
    @Id public int memberId;

    /** Last time we sent a retention email for this user, or null if never. */
    @Column(nullable = true)
    public Date retentionSent;

    /** Status of last sending (only valid if the last sent date is non-null). */
    public int retentionStatus;

    /** Number of retention emails sent. */
    public int retentionCount;

    /** Number of retention emails sent since last login (may not be updated until retention cron
     * job runs). */
    public int retentionCountSinceLogin;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SpamRecord}
     * with the supplied key values.
     */
    public static Key<SpamRecord> getKey (int memberId)
    {
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
