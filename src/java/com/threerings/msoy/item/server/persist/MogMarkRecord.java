//
// $Id: $

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

public abstract class MogMarkRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MogMarkRecord> _R = MogMarkRecord.class;
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The marked item's id. */
    @Id
    public int itemId;

    /** The mog group with which this item is marked. */
    @Id @Index(name="ixGroup")
    public int groupId;
}
