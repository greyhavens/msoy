//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Stores affiliate information for the given user.
 */
@Entity(indices={
    @Index(name="ixAffiliate", fields={ AffiliateRecord.AFFILIATE })
})
public class AffiliateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(AffiliateRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #affiliate} field. */
    public static final String AFFILIATE = "affiliate";

    /** The qualified column identifier for the {@link #affiliate} field. */
    public static final ColumnExp AFFILIATE_C =
        new ColumnExp(AffiliateRecord.class, AFFILIATE);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The member id */
    @Id public int memberId;

    /** The member's affiliate String at registration time. If none, this record
     * isn't even created. */
    public String affiliate;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AffiliateRecord}
     * with the supplied key values.
     */
    public static Key<AffiliateRecord> getKey (int memberId)
    {
        return new Key<AffiliateRecord>(
                AffiliateRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
