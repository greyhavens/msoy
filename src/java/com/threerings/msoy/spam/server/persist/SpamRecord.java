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
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp LAST_RETENTION_EMAIL_SENT = colexp(_R, "lastRetentionEmailSent");
    public static final ColumnExp LAST_RETENTION_EMAIL_RESULT = colexp(_R, "lastRetentionEmailResult");
    public static final ColumnExp RETENTION_EMAIL_COUNT = colexp(_R, "retentionEmailCount");
    public static final ColumnExp RETENTION_EMAIL_COUNT_SINCE_LAST_LOGIN = colexp(_R, "retentionEmailCountSinceLastLogin");
    // AUTO-GENERATED: FIELDS END

    /** Member id that is the target of the emails. */
    @Id public int memberId;

    /** Last time we sent a retention email for this user, or null if never. */
    @Column(nullable = true)
    public Date lastRetentionEmailSent;

    /** Result of last sending (-1 if cleared). */
    public int lastRetentionEmailResult;

    /** Number of retention emails sent. */
    public int retentionEmailCount;

    /** Number of retention emails sent since last login. */
    public int retentionEmailCountSinceLastLogin;

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link SpamRecord}
     * with the supplied key values.
     */
    public static Key<SpamRecord> getKey (int memberId)
    {
        return new Key<SpamRecord>(
                SpamRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
