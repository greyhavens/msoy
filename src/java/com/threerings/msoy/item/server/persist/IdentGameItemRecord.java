//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.Item;

/**
 * Contains additional fields required by sub-items.
 */
@Entity
public abstract class IdentGameItemRecord extends GameItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<IdentGameItemRecord> _R = IdentGameItemRecord.class;
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

    /** A function that extracts {@link #ident}. */
    public static Function<IdentGameItemRecord, String> GET_IDENT =
        new Function<IdentGameItemRecord, String>() {
        public String apply (IdentGameItemRecord record) {
            return record.ident;
        }
    };

    /** An identifier for this level pack, used by the game code. */
    public String ident;

    @Override // from ItemRecord
    public Item toItem ()
    {
        IdentGameItem item = (IdentGameItem)super.toItem();
        item.ident = ident;
        return item;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        IdentGameItem sitem = (IdentGameItem)item;
        ident = sitem.ident;
    }

    @Override // from GameItemRecord
    public boolean isListingOutOfDate (GameItemRecord master)
    {
        return super.isListingOutOfDate(master) ||
            !ident.equals(((IdentGameItemRecord)master).ident);
    }
}
