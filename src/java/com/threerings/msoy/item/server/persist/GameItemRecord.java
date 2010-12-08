//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

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

    /**
     * Check if this item needs to be relisted to incorporate changes.
     * TODO: move to base class if useful for items other than those used in games
     */
    public boolean isListingOutOfDate (GameItemRecord master)
    {
        return master == null ||
            !name.equals(master.name) || !description.equals(master.description) ||
            !getThumbMediaDesc().equals(master.getThumbMediaDesc()) ||
            !getFurniMediaDesc().equals(master.getFurniMediaDesc());
    }
}
