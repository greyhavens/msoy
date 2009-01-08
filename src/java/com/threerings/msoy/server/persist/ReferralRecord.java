//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Stores referral information for a given user.
 *
 * Nota bene: this class has been deprecated, and will be removed in a future release,
 * after migrating its tracker data to MemberRecord.
 */
@Entity
// @Deprecated
public class ReferralRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ReferralRecord> _R = ReferralRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp AFFILIATE = colexp(_R, "affiliate");
    public static final ColumnExp VECTOR = colexp(_R, "vector");
    public static final ColumnExp CREATIVE = colexp(_R, "creative");
    public static final ColumnExp TRACKER = colexp(_R, "tracker");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     *  will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The member id of the player. */
    @Id @Index(name="ixMemberId")
    public int memberId;

    /** Identifies the affiliate who referred them to us. */
    @Index(name="ixAffiliate")
    public String affiliate;

    /** Identifies the entry vector type. */
    @Index(name="ixVector")
    public String vector;

    /** Identifies the creative element variant (banner etc). */
    @Index(name="ixCreative")
    public String creative;

    /** Persistent tracking number, used to split players into various test groups. */
    public String tracker;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ReferralRecord}
     * with the supplied key values.
     */
    public static Key<ReferralRecord> getKey (int memberId)
    {
        return new Key<ReferralRecord>(
                ReferralRecord.class,
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
