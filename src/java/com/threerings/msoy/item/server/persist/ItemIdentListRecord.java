//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;

import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
    
@Table
public class ItemIdentListRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #listId} field. */
    public static final String LIST_ID = "listId";

    /** The qualified column identifier for the {@link #listId} field. */
    public static final ColumnExp LIST_ID_C =
        new ColumnExp(ItemIdentListRecord.class, LIST_ID);

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(ItemIdentListRecord.class, TYPE);

    /** The column identifier for the {@link #itemId} field. */
    public static final String ITEM_ID = "itemId";

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(ItemIdentListRecord.class, ITEM_ID);

    /** The column identifier for the {@link #sequence} field. */
    public static final String SEQUENCE = "sequence";

    /** The qualified column identifier for the {@link #sequence} field. */
    public static final ColumnExp SEQUENCE_C =
        new ColumnExp(ItemIdentListRecord.class, SEQUENCE);
    // AUTO-GENERATED: FIELDS END

    /**
     * The schema version of this record.
     */
    public static final int SCHEMA_VERSION = 1;

    /** The list identifier for this list to which we belong. */
    @Id
    public int listId;

    /** The item type. */
    public byte type;

    /** The item id. */
    public int itemId;

    /** The sequence number of this list item. */
    public short sequence;

    /**
     * Default constructor.
     */
    public ItemIdentListRecord ()
    {
    }

    /**
     * Construct an ItemIdentListRecord from an ItemIdent.
     */
    public ItemIdentListRecord (int listId, ItemIdent ident, int sequence)
    {
        this.listId = listId;
        this.type = ident.type;
        this.itemId = ident.itemId;
        this.sequence = (short) sequence;
    }

    public ItemIdentListRecord (int listId, Item item, int sequence)
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
     * Create and return a primary {@link Key} to identify a {@link #ItemIdentListRecord}
     * with the supplied key values.
     */
    public static Key<ItemIdentListRecord> getKey (int listId)
    {
        return new Key<ItemIdentListRecord>(
                ItemIdentListRecord.class,
                new String[] { LIST_ID },
                new Comparable[] { listId });
    }
    // AUTO-GENERATED: METHODS END
}
