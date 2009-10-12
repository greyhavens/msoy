//
// $Id: $

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.*; // for Depot annotations
import com.samskivert.depot.expression.ColumnExp;

public abstract class MogMarkRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<MogMarkRecord> _R = MogMarkRecord.class;
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp STAMPER_ID = colexp(_R, "stamperId");
    public static final ColumnExp LAST_STAMPED = colexp(_R, "lastStamped");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** A function for extract this persistent record's itemId. */
    public static Function<MogMarkRecord, Integer> TO_ITEM_ID =
        new Function<MogMarkRecord, Integer>() {
        public Integer apply (MogMarkRecord record) {
            return record.itemId;
        }
    };

    /** The stamped item's id. */
    @Id
    public int itemId;

    /** The mog group with which this item is stamped. */
    @Id @Index(name="ixGroup")
    public int groupId;

    /** The memberId of the person who last stamped this item. */
    public int stamperId;

    /** When this item was most recently stamped. */
    public Timestamp lastStamped;

    @Override
    public int hashCode ()
    {
        return itemId + 31*groupId;
    }

    @Override
    public boolean equals (Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MogMarkRecord other = (MogMarkRecord)obj;
        return groupId == other.groupId && itemId == other.itemId;
    }
}
