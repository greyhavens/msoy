//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Toy;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(name="itemId", pkColumnValue="FURNITURE")
public class ToyRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    public ToyRecord ()
    {
        super();
    }

    protected ToyRecord (Toy furniture)
    {
        super(furniture);

        // nothing for now
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.TOY;
    }

    @Override
    protected Item createItem ()
    {
        Toy object = new Toy();
        // nothing for now
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ToyRecord}
     * with the supplied key values.
     */
    public static Key<ToyRecord> getKey (int itemId)
    {
        return new Key<ToyRecord>(
                ToyRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
