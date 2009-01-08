//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Bars
 */
@Entity
public class BarPoolRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<BarPoolRecord> _R = BarPoolRecord.class;
    public static final ColumnExp ID = colexp(_R, "id");
    public static final ColumnExp BAR_POOL = colexp(_R, "barPool");
    public static final ColumnExp COIN_BALANCE = colexp(_R, "coinBalance");
    // AUTO-GENERATED: FIELDS END

    /** This can be incremented when we change this record. */
    public static final int SCHEMA_VERSION = 2;

    /** The singleton record id. */
    public static final int RECORD_ID = 1;

    /** The singleton Key. */
    public static final Key<BarPoolRecord> KEY = getKey(RECORD_ID);

    /** The Id of the record. {@link #RECORD_ID} */
    @Id
    public int id;

    /** The number of bars in the bar pool. */
    public int barPool;

    /** The number of coins created or destroyed. */
    public int coinBalance;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link BarPoolRecord}
     * with the supplied key values.
     */
    public static Key<BarPoolRecord> getKey (int id)
    {
        return new Key<BarPoolRecord>(
                BarPoolRecord.class,
                new ColumnExp[] { ID },
                new Comparable[] { id });
    }
    // AUTO-GENERATED: METHODS END
}
