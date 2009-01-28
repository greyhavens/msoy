//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.util.Comparator;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.TrophySource;

/**
 * Contains the persistent data for a TrophySource item.
 */
@TableGenerator(name="itemId", pkColumnValue="TROPHYSOURCE")
public class TrophySourceRecord extends SubItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TrophySourceRecord> _R = TrophySourceRecord.class;
    public static final ColumnExp SORT_ORDER = colexp(_R, "sortOrder");
    public static final ColumnExp SECRET = colexp(_R, "secret");
    public static final ColumnExp SUITE_ID = colexp(_R, "suiteId");
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    /** A comparator that sorts trophy source records by {@link #sortOrder}. */
    public static final Comparator<TrophySourceRecord> BY_SORT_ORDER =
        new Comparator<TrophySourceRecord>() {
        public int compare (TrophySourceRecord t1, TrophySourceRecord t2) {
            if (t1.sortOrder != t2.sortOrder) {
                return t1.sortOrder - t2.sortOrder;
            }
            return t1.itemId - t2.itemId;
        }
    };

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2 + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The order in which to display this trophy compared to other trophies. */
    public int sortOrder;

    /** Whether or not this trophy's description is a secret. */
    public boolean secret;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.TROPHY_SOURCE;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        TrophySource source = (TrophySource)item;
        sortOrder = source.sortOrder;
        secret = source.secret;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        TrophySource object = new TrophySource();
        object.sortOrder = sortOrder;
        object.secret = secret;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TrophySourceRecord}
     * with the supplied key values.
     */
    public static Key<TrophySourceRecord> getKey (int itemId)
    {
        return new Key<TrophySourceRecord>(
                TrophySourceRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
