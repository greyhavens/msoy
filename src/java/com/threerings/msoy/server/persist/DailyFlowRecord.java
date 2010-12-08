//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

/**
 * Summarizes the amount of each type of flow spent on any given day.
 */
@Entity
public class DailyFlowRecord extends PersistentRecord
{
    public static final int SCHEMA_VERSION = 1;

    // AUTO-GENERATED: FIELDS START
    public static final Class<DailyFlowRecord> _R = DailyFlowRecord.class;
    public static final ColumnExp<String> TYPE = colexp(_R, "type");
    public static final ColumnExp<Date> DATE = colexp(_R, "date");
    public static final ColumnExp<Integer> AMOUNT = colexp(_R, "amount");
    // AUTO-GENERATED: FIELDS END

    /** The type of grant or expenditure  summarized by this entry. */
    @Id
    public String type;

    /** The date for which this entry is a summary. */
    @Id
    public Date date;

    /** The total amount of flow spent or granted. */
    public int amount;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link DailyFlowRecord}
     * with the supplied key values.
     */
    public static Key<DailyFlowRecord> getKey (String type, Date date)
    {
        return newKey(_R, type, date);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(TYPE, DATE); }
    // AUTO-GENERATED: METHODS END
}
