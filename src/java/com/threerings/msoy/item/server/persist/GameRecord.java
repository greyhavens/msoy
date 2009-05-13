//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.TagCodes;

/**
 * Extends Item with game info.
 */
@Entity
@TableGenerator(name="itemId", pkColumnValue="GAME")
public class GameRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameRecord> _R = GameRecord.class;
    public static final ColumnExp GENRE = colexp(_R, "genre");
    public static final ColumnExp CONFIG = colexp(_R, "config");
    public static final ColumnExp GAME_MEDIA_HASH = colexp(_R, "gameMediaHash");
    public static final ColumnExp GAME_MIME_TYPE = colexp(_R, "gameMimeType");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp SHOT_MEDIA_HASH = colexp(_R, "shotMediaHash");
    public static final ColumnExp SHOT_MIME_TYPE = colexp(_R, "shotMimeType");
    public static final ColumnExp SPLASH_MEDIA_HASH = colexp(_R, "splashMediaHash");
    public static final ColumnExp SPLASH_MIME_TYPE = colexp(_R, "splashMimeType");
    public static final ColumnExp SERVER_MEDIA_HASH = colexp(_R, "serverMediaHash");
    public static final ColumnExp SERVER_MIME_TYPE = colexp(_R, "serverMimeType");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp SHOP_TAG = colexp(_R, "shopTag");
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

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 18;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** This game's genre. */
    @Index
    public byte genre;

    /** The XML game configuration. */
    @Column(length=65535)
    public String config;

    /** A hash code identifying the game media. */
    public byte[] gameMediaHash;

    /** The MIME type of the {@link #gameMediaHash} media. */
    public byte gameMimeType;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    /** A hash code identifying the screenshot media. */
    @Column(nullable=true)
    public byte[] shotMediaHash;

    /** The MIME type of the {@link #shotMediaHash} media. */
    public byte shotMimeType;

    /** A hash code identifying the splash media. */
    @Column(nullable=true)
    public byte[] splashMediaHash;

    /** The MIME type of the {@link #splashMediaHash} media. */
    public byte splashMimeType;

    /** A hash code identifying the server code media. */
    @Column(nullable=true)
    public byte[] serverMediaHash;

    /** The MIME type of the {@link #serverMediaHash} media. */
    public byte serverMimeType;

    /** Group associated with this game, required */
    public int groupId;

    /** The tag used to identify (non-pack) items in this game's shop. */
    @Column(length=TagCodes.MAX_TAG_LENGTH, nullable=true)
    public String shopTag;

    @Override // from ItemRecord
    public void prepareForListing (ItemRecord oldListing)
    {
        super.prepareForListing(oldListing);

        // the original from which this game is being listed will have -gameId as its game
        // identifier because it is an original; all non-originals will use the positive id
        gameId = Math.abs(gameId);
    }

    // TODO: this is dormant. See ItemServlet.remixItem
//    @Override // from ItemRecord
//    public void prepareForRemixing ()
//    {
//        super.prepareForRemixing();
//
//        // clear out our game id; this is now a totally separate game
//        gameId = 0;
//    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.GAME;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Game game = (Game)item;
        // gameId = not user editable
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return gameMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return gameMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        gameMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Game object = new Game();
        object.gameId = gameId;
        return object;
    }

    protected MediaDesc makeMediaDesc (byte[] mediaHash, byte mimeType)
    {
        return (mediaHash == null) ? null : new MediaDesc(mediaHash, mimeType);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameRecord}
     * with the supplied key values.
     */
    public static Key<GameRecord> getKey (int itemId)
    {
        return new Key<GameRecord>(
                GameRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
