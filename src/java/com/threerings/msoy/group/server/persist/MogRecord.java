//
// $Id: $

package com.threerings.msoy.group.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 *  Contains data specific to the MOG aspect of a group.
 */
@Entity
public class MogRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MogRecord> _R = MogRecord.class;
    public static final ColumnExp<Integer> GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp<String> MOG_ID = colexp(_R, "mogId");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The groupId of this theme. */
    @Id
    public int groupId;

    /** The globally unique string identifier of this MOG. */
    @Column(unique=true)
    public String mogId;

    public MogRecord ()
    {
    }

    public MogRecord (int groupId, String mogId)
    {
        this.groupId = groupId;
        this.mogId = mogId;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link MogRecord}
     * with the supplied key values.
     */
    public static Key<MogRecord> getKey (int groupId)
    {
        return newKey(_R, groupId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(GROUP_ID); }
    // AUTO-GENERATED: METHODS END
}
