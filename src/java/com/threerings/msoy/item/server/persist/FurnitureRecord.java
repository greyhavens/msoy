//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="FURNITURE")
public class FurnitureRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #action} field. */
    public static final String ACTION = "action";

    /** The qualified column identifier for the {@link #action} field. */
    public static final ColumnExp ACTION_C =
        new ColumnExp(FurnitureRecord.class, ACTION);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** An action associated with this furniture which is dispatched to the
     * virtual world client when the furniture is clicked on (max length 255
     * characters). */
    public String action = "";

    public FurnitureRecord ()
    {
        super();
    }

    protected FurnitureRecord (Furniture furniture)
    {
        super(furniture);

        action = furniture.action;
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.FURNITURE;
    }

    @Override
    protected Item createItem ()
    {
        Furniture object = new Furniture();
        object.action = action;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #FurnitureRecord}
     * with the supplied key values.
     */
    public static Key<FurnitureRecord> getKey (int itemId)
    {
        return new Key<FurnitureRecord>(
                FurnitureRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
