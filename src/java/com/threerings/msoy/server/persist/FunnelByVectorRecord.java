/**
 *
 */
package com.threerings.msoy.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

@Computed @Entity
public class FunnelByVectorRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FunnelByVectorRecord> _R = FunnelByVectorRecord.class;
    public static final ColumnExp<String> VECTOR = colexp(_R, "vector");
    public static final ColumnExp<Integer> COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    @Id
    public String vector;

    @Computed(fieldDefinition="count(*)")
    public int count;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FunnelByVectorRecord}
     * with the supplied key values.
     */
    public static Key<FunnelByVectorRecord> getKey (String vector)
    {
        return newKey(_R, vector);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(VECTOR); }
    // AUTO-GENERATED: METHODS END
}
