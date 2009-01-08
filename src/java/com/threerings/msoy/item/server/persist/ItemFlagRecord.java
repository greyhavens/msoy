//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Records the information from when a user flags an item.
 */
@Entity
public class ItemFlagRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemFlagRecord> _R = ItemFlagRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp KIND = colexp(_R, "kind");
    public static final ColumnExp TIMESTAMP = colexp(_R, "timestamp");
    public static final ColumnExp COMMENT = colexp(_R, "comment");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 3;

    /** Member who entered the flag. */
    @Id public int memberId;

    /** Type of item flagged. */
    @Id public byte itemType;

    /** Id of item flagged. */
    @Id public int itemId;

    /** Type of flag (from {@link ItemFlag.Kind}. */
    @Id public byte kind;

    /** When the flag was entered. */
    public Timestamp timestamp;

    @Column(length=2048)
    public String comment;

    public ItemFlag toItemFlag ()
    {
        ItemFlag itemFlag = new ItemFlag();
        itemFlag.memberId = memberId;
        itemFlag.itemIdent = new ItemIdent(itemType, itemId);
        itemFlag.kind = ItemFlag.Kind.values()[kind];
        itemFlag.comment = comment;
        itemFlag.timestamp = new Date(timestamp.getTime());
        return itemFlag;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ItemFlagRecord}
     * with the supplied key values.
     */
    public static Key<ItemFlagRecord> getKey (int memberId, byte itemType, int itemId, byte kind)
    {
        return new Key<ItemFlagRecord>(
                ItemFlagRecord.class,
                new ColumnExp[] { MEMBER_ID, ITEM_TYPE, ITEM_ID, KIND },
                new Comparable[] { memberId, itemType, itemId, kind });
    }
    // AUTO-GENERATED: METHODS END
}
