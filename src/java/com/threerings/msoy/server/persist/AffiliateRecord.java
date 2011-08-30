//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Stores affiliate information for the given user.
 */
@Entity
public class AffiliateRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AffiliateRecord> _R = AffiliateRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<String> AFFILIATE = colexp(_R, "affiliate");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The member id */
    @Id public int memberId;

    /** The member's affiliate String at registration time. If none, this record isn't even
     * created. */
    @Index(name="ixAffiliate")
    public String affiliate;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AffiliateRecord}
     * with the supplied key values.
     */
    public static Key<AffiliateRecord> getKey (int memberId)
    {
        return newKey(_R, memberId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID); }
    // AUTO-GENERATED: METHODS END
}
