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
import com.threerings.msoy.game.gwt.FeaturedGameInfo;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.server.GameUtil;

/**
 * Extends Item with game info.
 */
@Entity(indices={
  @Index(name="ixGenre", fields={ GameRecord.GENRE })
})
@TableGenerator(name="itemId", pkColumnValue="GAME")
public class GameRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #genre} field. */
    public static final String GENRE = "genre";

    /** The qualified column identifier for the {@link #genre} field. */
    public static final ColumnExp GENRE_C =
        new ColumnExp(GameRecord.class, GENRE);

    /** The column identifier for the {@link #config} field. */
    public static final String CONFIG = "config";

    /** The qualified column identifier for the {@link #config} field. */
    public static final ColumnExp CONFIG_C =
        new ColumnExp(GameRecord.class, CONFIG);

    /** The column identifier for the {@link #gameMediaHash} field. */
    public static final String GAME_MEDIA_HASH = "gameMediaHash";

    /** The qualified column identifier for the {@link #gameMediaHash} field. */
    public static final ColumnExp GAME_MEDIA_HASH_C =
        new ColumnExp(GameRecord.class, GAME_MEDIA_HASH);

    /** The column identifier for the {@link #gameMimeType} field. */
    public static final String GAME_MIME_TYPE = "gameMimeType";

    /** The qualified column identifier for the {@link #gameMimeType} field. */
    public static final ColumnExp GAME_MIME_TYPE_C =
        new ColumnExp(GameRecord.class, GAME_MIME_TYPE);

    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(GameRecord.class, GAME_ID);

    /** The column identifier for the {@link #shotMediaHash} field. */
    public static final String SHOT_MEDIA_HASH = "shotMediaHash";

    /** The qualified column identifier for the {@link #shotMediaHash} field. */
    public static final ColumnExp SHOT_MEDIA_HASH_C =
        new ColumnExp(GameRecord.class, SHOT_MEDIA_HASH);

    /** The column identifier for the {@link #shotMimeType} field. */
    public static final String SHOT_MIME_TYPE = "shotMimeType";

    /** The qualified column identifier for the {@link #shotMimeType} field. */
    public static final ColumnExp SHOT_MIME_TYPE_C =
        new ColumnExp(GameRecord.class, SHOT_MIME_TYPE);

    /** The column identifier for the {@link #serverMediaHash} field. */
    public static final String SERVER_MEDIA_HASH = "serverMediaHash";

    /** The qualified column identifier for the {@link #serverMediaHash} field. */
    public static final ColumnExp SERVER_MEDIA_HASH_C =
        new ColumnExp(GameRecord.class, SERVER_MEDIA_HASH);

    /** The column identifier for the {@link #serverMimeType} field. */
    public static final String SERVER_MIME_TYPE = "serverMimeType";

    /** The qualified column identifier for the {@link #serverMimeType} field. */
    public static final ColumnExp SERVER_MIME_TYPE_C =
        new ColumnExp(GameRecord.class, SERVER_MIME_TYPE);

    /** The column identifier for the {@link #groupId} field. */
    public static final String GROUP_ID = "groupId";

    /** The qualified column identifier for the {@link #groupId} field. */
    public static final ColumnExp GROUP_ID_C =
        new ColumnExp(GameRecord.class, GROUP_ID);

    /** The column identifier for the {@link #shopTag} field. */
    public static final String SHOP_TAG = "shopTag";

    /** The qualified column identifier for the {@link #shopTag} field. */
    public static final ColumnExp SHOP_TAG_C =
        new ColumnExp(GameRecord.class, SHOP_TAG);

    /** The qualified column identifier for the {@link #itemId} field. */
    public static final ColumnExp ITEM_ID_C =
        new ColumnExp(GameRecord.class, ITEM_ID);

    /** The qualified column identifier for the {@link #sourceId} field. */
    public static final ColumnExp SOURCE_ID_C =
        new ColumnExp(GameRecord.class, SOURCE_ID);

    /** The qualified column identifier for the {@link #flagged} field. */
    public static final ColumnExp FLAGGED_C =
        new ColumnExp(GameRecord.class, FLAGGED);

    /** The qualified column identifier for the {@link #creatorId} field. */
    public static final ColumnExp CREATOR_ID_C =
        new ColumnExp(GameRecord.class, CREATOR_ID);

    /** The qualified column identifier for the {@link #ownerId} field. */
    public static final ColumnExp OWNER_ID_C =
        new ColumnExp(GameRecord.class, OWNER_ID);

    /** The qualified column identifier for the {@link #catalogId} field. */
    public static final ColumnExp CATALOG_ID_C =
        new ColumnExp(GameRecord.class, CATALOG_ID);

    /** The qualified column identifier for the {@link #rating} field. */
    public static final ColumnExp RATING_C =
        new ColumnExp(GameRecord.class, RATING);

    /** The qualified column identifier for the {@link #ratingCount} field. */
    public static final ColumnExp RATING_COUNT_C =
        new ColumnExp(GameRecord.class, RATING_COUNT);

    /** The qualified column identifier for the {@link #used} field. */
    public static final ColumnExp USED_C =
        new ColumnExp(GameRecord.class, USED);

    /** The qualified column identifier for the {@link #location} field. */
    public static final ColumnExp LOCATION_C =
        new ColumnExp(GameRecord.class, LOCATION);

    /** The qualified column identifier for the {@link #lastTouched} field. */
    public static final ColumnExp LAST_TOUCHED_C =
        new ColumnExp(GameRecord.class, LAST_TOUCHED);

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(GameRecord.class, NAME);

    /** The qualified column identifier for the {@link #description} field. */
    public static final ColumnExp DESCRIPTION_C =
        new ColumnExp(GameRecord.class, DESCRIPTION);

    /** The qualified column identifier for the {@link #mature} field. */
    public static final ColumnExp MATURE_C =
        new ColumnExp(GameRecord.class, MATURE);

    /** The qualified column identifier for the {@link #thumbMediaHash} field. */
    public static final ColumnExp THUMB_MEDIA_HASH_C =
        new ColumnExp(GameRecord.class, THUMB_MEDIA_HASH);

    /** The qualified column identifier for the {@link #thumbMimeType} field. */
    public static final ColumnExp THUMB_MIME_TYPE_C =
        new ColumnExp(GameRecord.class, THUMB_MIME_TYPE);

    /** The qualified column identifier for the {@link #thumbConstraint} field. */
    public static final ColumnExp THUMB_CONSTRAINT_C =
        new ColumnExp(GameRecord.class, THUMB_CONSTRAINT);

    /** The qualified column identifier for the {@link #furniMediaHash} field. */
    public static final ColumnExp FURNI_MEDIA_HASH_C =
        new ColumnExp(GameRecord.class, FURNI_MEDIA_HASH);

    /** The qualified column identifier for the {@link #furniMimeType} field. */
    public static final ColumnExp FURNI_MIME_TYPE_C =
        new ColumnExp(GameRecord.class, FURNI_MIME_TYPE);

    /** The qualified column identifier for the {@link #furniConstraint} field. */
    public static final ColumnExp FURNI_CONSTRAINT_C =
        new ColumnExp(GameRecord.class, FURNI_CONSTRAINT);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION * BASE_MULTIPLIER + 17;

    /** This game's genre. */
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

    /**
     * Returns true if the specified game is in development.
     */
    public static boolean isDeveloperVersion (int gameId)
    {
        return gameId < 0;
    }

    /**
     * Returns true if this is game is in development
     */
    public boolean isDeveloperVersion ()
    {
        return isDeveloperVersion(gameId);
    }

    /**
     * Creates a {@link GameInfo} record for this game.
     */
    public GameInfo toGameInfo ()
    {
        return toGameInfo(new GameInfo());
    }

    /**
     * Populates and returns the supplied {@link GameInfo} record with this game's info.
     */
    public GameInfo toGameInfo (GameInfo info)
    {
        info.gameId = gameId;
        info.name = name;
        info.genre = genre;
        info.thumbMedia = getThumbMediaDesc();
        info.description = description;
        if (info instanceof FeaturedGameInfo && shotMediaHash != null) {
            ((FeaturedGameInfo)info).shotMedia = new MediaDesc(shotMediaHash, shotMimeType);
        }
        int[] players = GameUtil.getMinMaxPlayers((Game)toItem());
        info.minPlayers = players[0];
        info.maxPlayers = players[1];
        info.rating = rating;
        info.ratingCount = ratingCount;
        info.isInWorld = Game.detectIsInWorld(config);
        info.groupId = groupId;
        return info;
    }

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
        genre = game.genre;
        config = game.config;
        if (game.gameMedia != null) {
            gameMediaHash = game.gameMedia.hash;
            gameMimeType = game.gameMedia.mimeType;
        }
        // gameId = not user editable
        if (game.shotMedia != null) {
            shotMediaHash = game.shotMedia.hash;
            shotMimeType = game.shotMedia.mimeType;
        }
        if (game.serverMedia != null) {
            serverMediaHash = game.serverMedia.hash;
            serverMimeType = game.serverMedia.mimeType;
        }
        groupId = game.groupId;
        shopTag = game.shopTag;
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
        object.genre = genre;
        object.config = config;
        object.gameMedia = (gameMediaHash == null) ? null :
            new MediaDesc(gameMediaHash, gameMimeType);
        object.gameId = gameId;
        object.shotMedia = (shotMediaHash == null) ? null :
            new MediaDesc(shotMediaHash, shotMimeType);
        object.serverMedia = (serverMediaHash == null) ? null :
            new MediaDesc(serverMediaHash, serverMimeType);
        object.groupId = groupId;
        object.shopTag = shopTag;
        return object;
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
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
