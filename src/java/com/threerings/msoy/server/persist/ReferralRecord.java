//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.data.all.ReferralInfo;

/**
 * Stores referral information for a given user.
 */
@Entity(indices={
    @Index(name="ixMemberId", fields={ ReferralRecord.MEMBER_ID }),
    @Index(name="ixAffiliate", fields={ ReferralRecord.AFFILIATE }),
    @Index(name="ixVector", fields={ ReferralRecord.VECTOR }),
    @Index(name="ixCreative", fields={ ReferralRecord.CREATIVE }) })
public class ReferralRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ReferralRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #affiliate} field. */
    public static final String AFFILIATE = "affiliate";

    /** The qualified column identifier for the {@link #affiliate} field. */
    public static final ColumnExp AFFILIATE_C =
        new ColumnExp(ReferralRecord.class, AFFILIATE);

    /** The column identifier for the {@link #vector} field. */
    public static final String VECTOR = "vector";

    /** The qualified column identifier for the {@link #vector} field. */
    public static final ColumnExp VECTOR_C =
        new ColumnExp(ReferralRecord.class, VECTOR);

    /** The column identifier for the {@link #creative} field. */
    public static final String CREATIVE = "creative";

    /** The qualified column identifier for the {@link #creative} field. */
    public static final ColumnExp CREATIVE_C =
        new ColumnExp(ReferralRecord.class, CREATIVE);

    /** The column identifier for the {@link #tracker} field. */
    public static final String TRACKER = "tracker";

    /** The qualified column identifier for the {@link #tracker} field. */
    public static final ColumnExp TRACKER_C =
        new ColumnExp(ReferralRecord.class, TRACKER);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     *  will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The member id of the player. */
    @Id public int memberId;

    /** Identifies the affiliate who referred them to us. */
    public String affiliate;

    /** Identifies the entry vector type. */
    public String vector;

    /** Identifies the creative element variant (banner etc). */
    public String creative;

    /** Persistent tracking number, used to split players into various test groups. */
    public String tracker;

    /**
     * Convenience function to convert into a ReferralInfo object.
     */
    public ReferralInfo toInfo ()
    {
        return ReferralInfo.makeInstance(affiliate, vector, creative, tracker);
    }

    /**
     * Convenience function to convert from a ReferralInfo object.
     */
    public static ReferralRecord fromInfo (int memberId, ReferralInfo info)
    {
        ReferralRecord rec = new ReferralRecord();
        rec.memberId = memberId;
        rec.affiliate = info.affiliate;
        rec.vector = info.vector;
        rec.creative = info.creative;
        rec.tracker = info.tracker;
        return rec;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ReferralRecord}
     * with the supplied key values.
     */
    public static Key<ReferralRecord> getKey (int memberId)
    {
        return new Key<ReferralRecord>(
                ReferralRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
