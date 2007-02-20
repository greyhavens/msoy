//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

@Entity
@Table
public abstract class CloneRecord<T extends ItemRecord> extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(CloneRecord.class, ITEM_ID);

    /** The column identifier for the {@link #originalItemId} field. */
    public static final String ORIGINAL_ITEM_ID = "originalItemId";

    /** The qualified column identifier for the {@link #originalItemId} field. */
    public static final ColumnExp ORIGINAL_ITEM_ID_C =
        new ColumnExp(CloneRecord.class, ORIGINAL_ITEM_ID);

    /** The column identifier for the {@link #ownerId} field. */
    public static final String OWNER_ID = "ownerId";

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(CloneRecord.class, OWNER_ID);

    /** The column identifier for the {@link #used} field. */
    public static final String USED = "used";

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(CloneRecord.class, USED);

    /** The column identifier for the {@link #location} field. */
    public static final String LOCATION = "location";

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(CloneRecord.class, LOCATION);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** This clone's ID, unique relative all items of the same type. */
    @Id
    @GeneratedValue(generator="cloneId", strategy=GenerationType.TABLE)
    public int itemId;

    /** The ID of the immutable item from which this was cloned. */
    public int originalItemId;

    /** The owner of this clone. */
    public int ownerId;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    public byte used;

    /** Where it's being used. */
    public int location;
}
