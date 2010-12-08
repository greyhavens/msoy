//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Contains information about an item that numbers among one member's favorites.
 */
@Entity
public class FavoriteItemRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FavoriteItemRecord> _R = FavoriteItemRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<MsoyItemType> ITEM_TYPE = colexp(_R, "itemType");
    public static final ColumnExp<Integer> CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp<Timestamp> NOTED_ON = colexp(_R, "notedOn");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** The member in question. */
    @Id
    public int memberId;

    /** The type of the item that is favorited. */
    @Id @Index(name="ixType")
    public MsoyItemType itemType;

    /** The catalog listing id of the favorited item. */
    @Id
    public int catalogId;

    /** The time at which this favorite was noted. */
    public Timestamp notedOn;

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FavoriteItemRecord}
     * with the supplied key values.
     */
    public static Key<FavoriteItemRecord> getKey (int memberId, MsoyItemType itemType, int catalogId)
    {
        return newKey(_R, memberId, itemType, catalogId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, ITEM_TYPE, CATALOG_ID); }
    // AUTO-GENERATED: METHODS END
}
