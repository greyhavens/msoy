//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.IdentGameItem;

/**
 * Contains additional fields required by sub-items.
 */
@Entity
public abstract class IdentGameItemRecord extends GameItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<IdentGameItemRecord> _R = IdentGameItemRecord.class;
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
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
