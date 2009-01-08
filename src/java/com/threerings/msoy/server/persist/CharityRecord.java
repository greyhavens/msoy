//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

@Entity
public class CharityRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<CharityRecord> _R = CharityRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp CORE = colexp(_R, "core");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** Member ID for this charity. */
    @Id
    public int memberId;

    /** Whether or not this charity is a core charity. */
    @Index(name="ixCore")
    public boolean core;

    /** Description of the charity to display to members. */
    @Column(defaultValue="")
    public String description;

    /** For depot */
    public CharityRecord ()
    {
    }

    /**
     * Constructs a new charity.
     *
     * @param memberId ID of the member for this charity.
     * @param core True if this is a "core" charity,
     * @param description Description of the charity that members can see.
     */
    public CharityRecord (int memberId, boolean core, String description)
    {
        this.memberId = memberId;
        this.core = core;
        this.description = description;
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
                new ColumnExp[] { MEMBER_ID },
                new Comparable[] { memberId });
    }
    // AUTO-GENERATED: METHODS END
}
