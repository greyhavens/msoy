//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

@Entity(indices = {
    @Index(name="ixCore", fields = { CharityRecord.CORE })
})
public class CharityRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(CharityRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #core} field. */
    public static final String CORE = "core";

    /** The qualified column identifier for the {@link #core} field. */
    public static final ColumnExp CORE_C =
        new ColumnExp(CharityRecord.class, CORE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;
    
    /** Member ID for this charity. */
    @Id
    public int memberId;
    
    /** Whether or not this charity is a core charity. */
    public boolean core;

    /** For depot */
    public CharityRecord ()
    {
    }
    
    /**
     * Constructs a new charity.
     * 
     * @param memberId ID of the member for this charity.
     * @param core True if this is a "core" charity, 
     */
    public CharityRecord (int memberId, boolean core)
    {
        this.memberId = memberId;
        this.core = core;
    }
    
    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link CharityRecord}
     * with the supplied key values.
     */
    public static Key<CharityRecord> getKey (int memberId)
    {
        return new Key<CharityRecord>(
                CharityRecord.class,
                new String[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
