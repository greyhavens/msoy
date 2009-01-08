//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

/**
 * Contains information about an item that numbers among one member's favorites.
 */
@Entity
public class FavoriteItemRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FavoriteItemRecord> _R = FavoriteItemRecord.class;
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp NOTED_ON = colexp(_R, "notedOn");
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
                new ColumnExp[] { MEMBER_ID, ITEM_TYPE, CATALOG_ID },
                new Comparable[] { memberId, itemType, catalogId });
    }
    // AUTO-GENERATED: METHODS END
}
