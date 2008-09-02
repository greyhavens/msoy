//
// $Id$

package com.threerings.msoy.money.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;

/**
 * Used to count the number of available transactions in a history query.
 */
@Entity
@Computed
public class HistoryCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(HistoryCountRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    @Computed(fieldDefinition="count(*)")
    public int count;
}
