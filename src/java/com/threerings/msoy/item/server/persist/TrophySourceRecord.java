//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.Comparator;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.Comparators;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.data.all.TrophySource;

/**
 * Contains the persistent data for a TrophySource item.
 */
@TableGenerator(name="itemId", pkColumnValue="TROPHYSOURCE")
public class TrophySourceRecord extends IdentGameItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TrophySourceRecord> _R = TrophySourceRecord.class;
    public static final ColumnExp<Integer> SORT_ORDER = colexp(_R, "sortOrder");
    public static final ColumnExp<Boolean> SECRET = colexp(_R, "secret");
    public static final ColumnExp<String> IDENT = colexp(_R, "ident");
    public static final ColumnExp<Integer> GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp<Integer> ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp<Integer> SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp<Integer> CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Integer> CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp<Integer> RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp<Integer> RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp<Item.UsedAs> USED = colexp(_R, "used");
    public static final ColumnExp<Integer> LOCATION = colexp(_R, "location");
    public static final ColumnExp<Timestamp> LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<Boolean> MATURE = colexp(_R, "mature");
    public static final ColumnExp<byte[]> THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp<Byte> THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp<Byte> THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp<byte[]> FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp<Byte> FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp<Byte> FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    /** A comparator that sorts trophy source records by {@link #sortOrder}. */
    public static final Comparator<TrophySourceRecord> BY_SORT_ORDER =
        new Comparator<TrophySourceRecord>() {
        public int compare (TrophySourceRecord t1, TrophySourceRecord t2) {
            int cmp = Comparators.compare(t1.sortOrder, t2.sortOrder);
            if (cmp == 0) {
                // we end up storing these in a tree map, cmping at 0 will cause that to
                // think the two elents are equal.
                cmp = Comparators.compare(t1.itemId, t2.itemId);
            }
            return cmp;
        }
    };

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 3;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The order in which to display this trophy compared to other trophies. */
    public int sortOrder;

    /** Whether or not this trophy's description is a secret. */
    public boolean secret;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.TROPHY_SOURCE;
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

    @Override // from GameItemRecord
    public boolean isListingOutOfDate (GameItemRecord master)
    {
        TrophySourceRecord tmaster = (TrophySourceRecord)master;
        return super.isListingOutOfDate(master) || secret != tmaster.secret ||
            sortOrder != tmaster.sortOrder;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link TrophySourceRecord}
     * with the supplied key values.
     */
    public static Key<TrophySourceRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
