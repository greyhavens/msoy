//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.Table;

import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemList;

@Table
public class ItemListRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #listId} field. */
    public static final String LIST_ID = "listId";

    /** The qualified column identifier for the {@link #listId} field. */
    public static final ColumnExp LIST_ID_C =
        new ColumnExp(ItemListRecord.class, LIST_ID);

    /** The column identifier for the {@link #items} field. */
    public static final String ITEMS = "items";

    /** The qualified column identifier for the {@link #items} field. */
    public static final ColumnExp ITEMS_C =
        new ColumnExp(ItemListRecord.class, ITEMS);
    // AUTO-GENERATED: FIELDS END

    /**
     * The schema version of this record.
     */
    public static final int SCHEMA_VERSION = 1;

    /** The globally-unique identifier for this list. */
    @Id
    public int listId;

    /** All the items in this list, encoded as a byte[]. */
    public byte[] items;

    /**
     * Default constructor.
     */
    public ItemListRecord ()
    {
    }

    /**
     * Construct an ItemListRecord from the ItemList.
     */
    public ItemListRecord (int listId, ItemIdent[] idents)
        throws PersistenceException
    {
        this.listId = listId;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeInt(idents.length);
            for (ItemIdent ident : idents) {
                oos.writeBareObject(ident);
            }
            oos.flush();

        } catch (IOException ioe) {
            throw new PersistenceException("Error serializing item list", ioe);
        }
        items = baos.toByteArray();
    }

    /**
     * Create an ItemList from this record.
     */
    public ItemIdent[] toItemList ()
        throws PersistenceException
    {
        ItemIdent[] idents;

        ByteArrayInputStream bain = new ByteArrayInputStream(items);
        try {
            ObjectInputStream ois = new ObjectInputStream(bain);
            idents = new ItemIdent[ois.readInt()];
            for (int ii = 0; ii < idents.length; ii++) {
                ois.readBareObject(idents[ii] = new ItemIdent());
            }
        } catch (ClassNotFoundException cnfe) {
            throw new PersistenceException("Error serializing item list", cnfe);

        } catch (IOException ioe) {
            throw new PersistenceException("Error serializing item list", ioe);
        }
        return idents;
    }

    @Override
    public int hashCode ()
    {
        return listId;
    }

    @Override
    public boolean equals (Object other)
    {
        return (other instanceof ItemListRecord) &&
            (((ItemListRecord) other).listId == this.listId);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ItemListRecord}
     * with the supplied key values.
     */
    public static Key<ItemListRecord> getKey (int listId)
    {
        return new Key<ItemListRecord>(
                ItemListRecord.class,
                new String[] { LIST_ID },
                new Comparable[] { listId });
    }
    // AUTO-GENERATED: METHODS END
}
