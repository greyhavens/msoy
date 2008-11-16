//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

@Computed
@Entity
public class MemberActionCountRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #actionId} field. */
    public static final String ACTION_ID = "actionId";

    /** The qualified column identifier for the {@link #actionId} field. */
    public static final ColumnExp ACTION_ID_C =
        new ColumnExp(MemberActionCountRecord.class, ACTION_ID);

    /** The column identifier for the {@link #count} field. */
    public static final String COUNT = "count";

    /** The qualified column identifier for the {@link #count} field. */
    public static final ColumnExp COUNT_C =
        new ColumnExp(MemberActionCountRecord.class, COUNT);
    // AUTO-GENERATED: FIELDS END

    /** The id of the action this entry counts. */
    public int actionId;

    /** The number of times this action was performed (by the implicit member). */
    public int count;
}
