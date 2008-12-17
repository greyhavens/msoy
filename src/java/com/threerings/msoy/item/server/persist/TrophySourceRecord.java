//
// $Id$

package com.threerings.msoy.item.server.persist;

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
    /** The column identifier for the {@link #sortOrder} field. */
    public static final String SORT_ORDER = "sortOrder";

    /** The qualified column identifier for the {@link #sortOrder} field. */
    public static final ColumnExp SORT_ORDER_C =
        new ColumnExp(TrophySourceRecord.class, SORT_ORDER);

    /** The column identifier for the {@link #secret} field. */
    public static final String SECRET = "secret";

    /** The qualified column identifier for the {@link #secret} field. */
    public static final ColumnExp SECRET_C =
        new ColumnExp(TrophySourceRecord.class, SECRET);

    /** The qualified column identifier for the {@link #suiteId} field. */
    public static final ColumnExp SUITE_ID_C =
        new ColumnExp(TrophySourceRecord.class, SUITE_ID);

    /** The qualified column identifier for the {@link #ident} field. */
    public static final ColumnExp IDENT_C =
        new ColumnExp(TrophySourceRecord.class, IDENT);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(TrophySourceRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(TrophySourceRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(TrophySourceRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(TrophySourceRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(TrophySourceRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(TrophySourceRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(TrophySourceRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(TrophySourceRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(TrophySourceRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(TrophySourceRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(TrophySourceRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(TrophySourceRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(TrophySourceRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(TrophySourceRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(TrophySourceRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(TrophySourceRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(TrophySourceRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(TrophySourceRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(TrophySourceRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

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
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
