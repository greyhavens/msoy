//
// $Id$

package com.threerings.msoy.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.admin.gwt.AffiliateMapping;

/**
 * Maintains a mapping from an affiliate to a memberId.
 * Affiliates must have an account set up to receive their affiliate cuts. These records
 * will be created for all affiliates, initially mapping to memberId 0.
 */
public class AffiliateMapRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AffiliateMapRecord> _R = AffiliateMapRecord.class;
    public static final ColumnExp AFFILIATE = colexp(_R, "affiliate");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    public static Function<AffiliateMapRecord, AffiliateMapping> TO_MAPPING =
        new Function<AffiliateMapRecord, AffiliateMapping>() {
        public AffiliateMapping apply (AffiliateMapRecord record) {
            return record.toMapping();
        }
    };

    /** The affiliate name as submitted by a registering user. {@link ReferralRecord#affiliate} */
    @Id
    public String affiliate;

    /** The memberId that this affiliate maps to, or 0 if there is currenly no association
     * for this affiliate. */
    public int memberId;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AffiliateMapRecord}
     * with the supplied key values.
     */
    public static Key<AffiliateMapRecord> getKey (String affiliate)
    {
        return new Key<AffiliateMapRecord>(
                AffiliateMapRecord.class,
                new ColumnExp[] { AFFILIATE },
                new Comparable[] { affiliate });
    }
    // AUTO-GENERATED: METHODS END

    /**
     * Convert this record into an AffiliateMapping record.
     */
    public AffiliateMapping toMapping ()
    {
        return new AffiliateMapping(affiliate, memberId);
    }
}
