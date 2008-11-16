//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

@Computed
@Entity
public class CountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(CountRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    /** The computed count. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
