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
    public static final Class<CountRecord> _R = CountRecord.class;
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The computed count. */
    @Computed(fieldDefinition="count(*)")
    public int count;
}
