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
    public static final Class<MemberActionCountRecord> _R = MemberActionCountRecord.class;
    public static final ColumnExp ACTION_ID = colexp(_R, "actionId");
    public static final ColumnExp COUNT = colexp(_R, "count");
    // AUTO-GENERATED: FIELDS END

    /** The id of the action this entry counts. */
    public int actionId;

    /** The number of times this action was performed (by the implicit member). */
    public int count;
}
