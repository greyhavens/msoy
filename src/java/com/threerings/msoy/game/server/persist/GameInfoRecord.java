//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.data.all.TagCodes;

import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.Prop;
import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.TrophySource;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.gwt.GameCard;
import com.threerings.msoy.game.gwt.GameInfo;

/**
 * Contains details on a single game "title" including the development and published game item ids
 * and other metrics.
 */
@Entity
@TableGenerator(name="gameId", pkColumnValue="GAME_ID")
public class GameInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameInfoRecord> _R = GameInfoRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp GENRE = colexp(_R, "genre");
    public static final ColumnExp IS_AVRG = colexp(_R, "isAVRG");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp SHOT_MEDIA_HASH = colexp(_R, "shotMediaHash");
    public static final ColumnExp SHOT_MIME_TYPE = colexp(_R, "shotMimeType");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp SHOP_TAG = colexp(_R, "shopTag");
    public static final ColumnExp RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp INTEGRATED = colexp(_R, "integrated");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 2;

    /** The default payout factor for newly added games. */
    public static final int DEFAULT_PAYOUT_FACTOR = 128;

    /** The quantity of flow to be awarded before our first recalc. */
    public static final int INITIAL_RECALC_FLOW = 6000;

    /** The unique identifier for this game. */ // TODO: nix initialValue after migration
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY, initialValue=2500)
    public int gameId;

    /** The name of the game (baby). */
    @Column(length=GameInfo.MAX_NAME_LENGTH)
    public String name;

    /** This game's genre. */
    @Index public byte genre;

    /** True if this is an AVRG, false if it's a parlor game. */
    public boolean isAVRG;

    /** The id of the member that created this game. */
    @Index public int creatorId;

    /** A more detailed description of the game. */
    @Column(length=GameInfo.MAX_DESCRIPTION_LENGTH)
    public String description;

    /** A hash code identifying the game's thumbnail media. */
    @Column(nullable=true)
    public byte[] thumbMediaHash;

    /** The MIME type of the {@link #thumbMediaHash} media. */
    public byte thumbMimeType;

    /** The size constraint on the {@link #thumbMediaHash} media. */
    public byte thumbConstraint;

    /** A hash code identifying the screenshot media. */
    @Column(nullable=true)
    public byte[] shotMediaHash;

    /** The MIME type of the {@link #shotMediaHash} media. */
    public byte shotMimeType;

    /** Group associated with this game, required */
    public int groupId;

    /** The tag used to identify (non-pack) items in this game's shop. */
    @Column(length=TagCodes.MAX_TAG_LENGTH, nullable=true)
    public String shopTag;

    /** The current sum of all ratings that have been applied to this game. */
    public int ratingSum;

    /** The number of user ratings that went into the average rating. */
    public int ratingCount;

    /** Whether or not we believe that this game is integrated with the Whirled API. */
    public boolean integrated;

    /** 
     * Calculates this game's average rating from the sum and count.
     */
    public float getRating ()
    {
        return (ratingCount > 0) ? (float) ratingSum / ratingCount : 0f;
    }

    /**
     * Returns this game's thumbnail media or a default if it is not specified.
     */
    public MediaDesc getThumbMedia ()
    {
        return MediaDesc.make(thumbMediaHash, thumbMimeType, thumbConstraint, DEFAULT_THUMB_MEDIA);
    }

    /**
     * Returns this game's screenshot media or a default if it is not specified.
     */
    public MediaDesc getShotMedia ()
    {
        return MediaDesc.make(shotMediaHash, shotMimeType, getThumbMedia());
    }

    /**
     * Returns the item types in our suite.
     */
    public IdentGameItem[] getSuiteTypes ()
    {
        // TODO: add GameLauncher
        return (isAVRG ? new IdentGameItem[] {
                new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), new Prop() } :
            new IdentGameItem[] {
                new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), });
    }

    /**
     * Creates a {@link GameCard} record for this game.
     */
    public GameCard toGameCard (int playersOnline)
    {
        GameCard card = new GameCard();
        card.gameId = gameId;
        card.name = name;
        card.thumbMedia = getThumbMedia();
        card.playersOnline = playersOnline;
        return card;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public GameInfo toGameInfo (int playersOnline)
    {
        GameInfo info = new GameInfo();
        info.gameId = gameId;
        info.name = name;
        info.genre = genre;
        info.isAVRG = isAVRG;
        info.creator = MemberName.makeKey(creatorId);
        info.description = description;
        info.thumbMedia = getThumbMedia();
        info.shotMedia = getShotMedia();
        info.groupId = groupId;
        info.shopTag = shopTag;
        info.rating = getRating();
        info.ratingCount = ratingCount;
        info.integrated = integrated;
        info.playersOnline = playersOnline;
        return info;
    }

    /**
     * Creates a {@link GameSummary} record for this game.
     */
    public GameSummary toGameSummary ()
    {
        return new GameSummary(gameId, name, isAVRG, getThumbMedia());
    }

    /**
     * Updates this record with changes provided by the creator.
     */
    public void update (GameInfo info)
    {
        this.name = info.name;
        this.genre = info.genre;
        this.isAVRG = info.isAVRG; // TODO: don't allow this to be changed?
        this.description = info.description;
        this.thumbMediaHash = MediaDesc.unmakeHash(info.thumbMedia);
        this.thumbMimeType = MediaDesc.unmakeMimeType(info.thumbMedia);
        this.thumbConstraint = MediaDesc.unmakeConstraint(info.thumbMedia);
        this.shotMediaHash = MediaDesc.unmakeHash(info.shotMedia);
        this.shotMimeType = MediaDesc.unmakeMimeType(info.shotMedia);
        this.groupId = info.groupId;
        this.shopTag = info.shopTag;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameInfoRecord}
     * with the supplied key values.
     */
    public static Key<GameInfoRecord> getKey (int gameId)
    {
        return new Key<GameInfoRecord>(
                GameInfoRecord.class,
                new ColumnExp[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END

    protected static final MediaDesc DEFAULT_THUMB_MEDIA = new StaticMediaDesc(
        MediaDesc.IMAGE_PNG, "game", "thumb", MediaDesc.HALF_VERTICALLY_CONSTRAINED); // TODO
}
