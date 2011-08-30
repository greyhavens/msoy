//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains the details of person's membership in a group.
 */
@Entity
public class BrandShareRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BrandShareRecord> _R = BrandShareRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp<Integer> SHARES = colexp(_R, "shares");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The id of the member with brand shares. */
    @Id public int memberId;

    /** The id of the group of the brand. */
    @Id @Index(name="ixGroup")
    public int groupId;

    /** The number of shares the member has in the brand. */
    public int shares;

    /**
     * Generates a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Constructor for unserializing. */
    public BrandShareRecord ()
    {
    }

    public BrandShareRecord (int memberId, int groupId, int shares)
    {
        this.memberId = memberId;
        this.groupId = groupId;
        this.shares = shares;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BrandShareRecord}
     * with the supplied key values.
     */
    public static Key<BrandShareRecord> getKey (int memberId, int groupId)
    {
        return newKey(_R, memberId, groupId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, GROUP_ID); }
    // AUTO-GENERATED: METHODS END
}
