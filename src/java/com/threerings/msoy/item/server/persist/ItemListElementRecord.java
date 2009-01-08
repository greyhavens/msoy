//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Represents an element of a list of items.
 *
 * @author mjensen
 */
@Entity
public class ItemListElementRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ItemListElementRecord> _R = ItemListElementRecord.class;
    public static final ColumnExp LIST_ID = colexp(_R, "listId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SEQUENCE = colexp(_R, "sequence");
    // AUTO-GENERATED: FIELDS END

    /** The schema version of this record. */
    public static final int SCHEMA_VERSION = 2;

    /** Transforms a persistent record to an {@link ItemIdent}. */
    public static Function<ItemListElementRecord, ItemIdent> TO_IDENT =
        new Function<ItemListElementRecord, ItemIdent>() {
        public ItemIdent apply (ItemListElementRecord record) {
            return record.toItemIdent();
        }
    };

    /** The identifier for this list to which we belong. */
    @Id
    public int listId;

    /** The item type. */
    @Id
    public byte type;

    /** The item id. */
    @Id
    public int itemId;

    /** Use to sort the elements of a list. */
    public short sequence;

    /**
     * Default constructor.
     */
    public ItemListElementRecord ()
    {
    }

    /**
     * Construct an ItemListElementRecord from an ItemIdent.
     */
    public ItemListElementRecord (int listId, ItemIdent ident, int sequence)
    {
        this.listId = listId;
        this.type = ident.type;
        this.itemId = ident.itemId;
        this.sequence = (short) sequence;
    }

    public ItemListElementRecord (int listId, Item item, int sequence)
    {
        this(listId, item.getIdent(), sequence);
    }

    public ItemIdent toItemIdent ()
    {
        return new ItemIdent(type, itemId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ItemListElementRecord}
     * with the supplied key values.
     */
    public static Key<ItemListElementRecord> getKey (int listId, byte type, int itemId)
    {
        return new Key<ItemListElementRecord>(
                ItemListElementRecord.class,
                new ColumnExp[] { LIST_ID, TYPE, ITEM_ID },
                new Comparable[] { listId, type, itemId });
    }
    // AUTO-GENERATED: METHODS END
}

