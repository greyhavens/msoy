//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Represents an element of a list of items.
 * 
 * @author mjensen
 */
@Entity
public class ItemListElementRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #listId} field. */
    public static final String LIST_ID = "listId";

    /** The qualified column identifier for the {@link #listId} field. */
    public static final ColumnExp LIST_ID_C =
        new ColumnExp(ItemListElementRecord.class, LIST_ID);

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(ItemListElementRecord.class, TYPE);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(ItemListElementRecord.class, ITEM_ID);

    /** The column identifier for the {@link #sequence} field. */
    public static final String SEQUENCE = "sequence";

    /** The qualified column identifier for the {@link #sequence} field. */
    public static final ColumnExp SEQUENCE_C =
        new ColumnExp(ItemListElementRecord.class, SEQUENCE);
    // AUTO-GENERATED: FIELDS END
 
    /** The schema version of this record. */
    public static final int SCHEMA_VERSION = 2;
    
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
        this.listId = listId;
        this.type = item.getType();
        this.itemId = item.itemId;
        this.sequence = (short) sequence;
    }

    public ItemIdent toItemIdent ()
    {
        return new ItemIdent(type, itemId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ItemListElementRecord}
     * with the supplied key values.
     */
    public static Key<ItemListElementRecord> getKey (int listId, byte type, int itemId)
    {
        return new Key<ItemListElementRecord>(
                ItemListElementRecord.class,
                new String[] { LIST_ID, TYPE, ITEM_ID },
                new Comparable[] { listId, type, itemId });
    }
    // AUTO-GENERATED: METHODS END
}

