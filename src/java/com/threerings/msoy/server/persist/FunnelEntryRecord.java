/**
 *
 */
package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

@Computed @Entity
public class FunnelEntryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FunnelEntryRecord> _R = FunnelEntryRecord.class;
    public static final ColumnExp VECTOR = colexp(_R, "vector");
    public static final ColumnExp DATE = colexp(_R, "date");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    @Id
    public String vector;

    @Id
    public Timestamp date;

    @Computed(fieldDefinition="count(*)")
    public int count;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FunnelEntryRecord}
     * with the supplied key values.
     */
    public static Key<FunnelEntryRecord> getKey (String vector, Timestamp date)
    {
        return newKey(_R, vector, date);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(VECTOR, DATE); }
    // AUTO-GENERATED: METHODS END
}
