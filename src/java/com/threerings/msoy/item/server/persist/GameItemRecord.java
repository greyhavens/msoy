//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.persist.ItemRecord;

/**
 * A base class for items that are associated with a game.
 */
@Entity
public abstract class GameItemRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameItemRecord> _R = GameItemRecord.class;
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

    /** The game to which this item belongs. See {@link GameItem#gameId}. */
    @Index(name="ixGameId") public int gameId;

    @Override // from ItemRecord
    public void prepareForListing (ItemRecord oldListing)
    {
        super.prepareForListing(oldListing);

        // we need to flip from the dev game id to the published game id
        gameId = Math.abs(gameId);
    }

    @Override // from ItemRecord
    public Item toItem ()
    {
        GameItem item = (GameItem)super.toItem();
        item.gameId = gameId;
        return item;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        GameItem sitem = (GameItem)item;
        gameId = sitem.gameId;
    }
}
