//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.threerings.io.Streamable;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.GeneratedValue;
import com.samskivert.jdbc.depot.annotation.GenerationType;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Index;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.ItemListInfo;

/**
 * Meta-data defining a list of items.
 */
@Entity(indices={ @Index(name="ixMember", fields={"memberId"}) })
@TableGenerator(name="listId", pkColumnValue="ITEM_LIST_INFO")
public class ItemListInfoRecord extends PersistentRecord
    implements Streamable
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #listId} field. */
    public static final String LIST_ID = "listId";

    /** The qualified column identifier for the {@link #listId} field. */
    public static final ColumnExp LIST_ID_C =
        new ColumnExp(ItemListInfoRecord.class, LIST_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(ItemListInfoRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #type} field. */
    public static final String TYPE = "type";

    /** The qualified column identifier for the {@link #type} field. */
    public static final ColumnExp TYPE_C =
        new ColumnExp(ItemListInfoRecord.class, TYPE);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(ItemListInfoRecord.class, NAME);
    // AUTO-GENERATED: FIELDS END

    /** Our depot schema version. */
    public static final int SCHEMA_VERSION = 2;

    @Id
    @GeneratedValue(generator="listId", strategy=GenerationType.TABLE, allocationSize=1)
    public int listId;

    public int memberId;

    public byte type;

    public String name;

    /**
     * Default constructor.
     */
    public ItemListInfoRecord ()
    {
    }

    /**
     * Construct a record from an ItemListInfo.
     */
    public ItemListInfoRecord (ItemListInfo info, int memberId)
    {
        this.listId = info.listId;
        this.memberId = memberId;
        this.type = info.type;
        this.name = info.name;
    }

    public ItemListInfo toItemListInfo ()
    {
        ItemListInfo info = new ItemListInfo();
        info.listId = this.listId;
        info.type = this.type;
        info.name = this.name;

        return info;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #ItemListInfoRecord}
     * with the supplied key values.
     */
    public static Key<ItemListInfoRecord> getKey (int listId)
    {
        return new Key<ItemListInfoRecord>(
                ItemListInfoRecord.class,
                new String[] { LIST_ID },
                new Comparable[] { listId });
    }
    // AUTO-GENERATED: METHODS END
}
