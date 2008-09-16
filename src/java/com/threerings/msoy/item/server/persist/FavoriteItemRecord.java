//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

/**
 * Contains information about an item that numbers among one member's favorites.
 */
@Entity
public class FavoriteItemRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(FavoriteItemRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #itemType} field. */
    public static final String ITEM_TYPE = "itemType";

    /** The qualified column identifier for the {@link #itemType} field. */
    public static final ColumnExp ITEM_TYPE_C =
        new ColumnExp(FavoriteItemRecord.class, ITEM_TYPE);

    /** The column identifier for the {@link #catalogId} field. */
    public static final String CATALOG_ID = "catalogId";

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(FavoriteItemRecord.class, CATALOG_ID);

    /** The column identifier for the {@link #notedOn} field. */
    public static final String NOTED_ON = "notedOn";

    /** The qualified column identifier for the {@link #notedOn} field. */
    public static final ColumnExp NOTED_ON_C =
        new ColumnExp(FavoriteItemRecord.class, NOTED_ON);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The member in question. */
    @Id public int memberId;

    /** The type of the item that is favorited. */
    @Id public byte itemType;

    /** The catalog listing id of the favorited item. */
    @Id public int catalogId;

    /** The time at which this favorite was noted. */
    public Timestamp notedOn;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FavoriteItemRecord}
     * with the supplied key values.
     */
    public static Key<FavoriteItemRecord> getKey (int memberId, byte itemType, int catalogId)
    {
        return new Key<FavoriteItemRecord>(
                FavoriteItemRecord.class,
                new String[] { MEMBER_ID, ITEM_TYPE, CATALOG_ID },
                new Comparable[] { memberId, itemType, catalogId });
    }
    // AUTO-GENERATED: METHODS END
}
