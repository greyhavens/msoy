//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.annotation.Computed;

/**
 * Used to summarize data from the {@link EntryVectorRecord} table.
 */
@Computed(shadowOf=EntryVectorRecord.class)
public class EntrySummaryRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EntrySummaryRecord> _R = EntrySummaryRecord.class;
    public static final ColumnExp VECTOR = colexp(_R, "vector");
    public static final ColumnExp ENTRIES = colexp(_R, "entries");
    // AUTO-GENERATED: FIELDS END

    /** The vector in question. */
    public String vector;

    /** The number of entries for this vector that meet our criteria. */
    @Computed(fieldDefinition="count(*)")
    public int entries;
}
