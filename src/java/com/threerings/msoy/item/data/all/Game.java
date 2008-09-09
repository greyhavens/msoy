//
// $Id$

package com.threerings.msoy.item.data.all;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Extends Item with game info.
 */
public class Game extends Item
{
    /** A {@link #genre} constant. */
    public static final byte GENRE_OTHER = 0;

    /** A {@link #genre} constant. */
    public static final byte GENRE_WORD = 1;

    /** A {@link #genre} constant. */
    public static final byte GENRE_CARD_BOARD = 2;

    /** A {@link #genre} constant. */
    public static final byte GENRE_PUZZLE = 3;

    /** A {@link #genre} constant. */
    public static final byte GENRE_STRATEGY = 4;

    /** A {@link #genre} constant. */
    public static final byte GENRE_ACTION_ARCADE = 5;

    /** A {@link #genre} constant. */
    public static final byte GENRE_ADVENTURE_RPG = 6;

    /** A {@link #genre} constant. */
    public static final byte GENRE_SPORTS_RACING = 7;

    /** A {@link #genre} constant. */
    public static final byte GENRE_MMO_WHIRLED = 8;

    /** All game genres, in display order. */
    public static final byte[] GENRES = {
        GENRE_WORD, GENRE_STRATEGY, GENRE_ACTION_ARCADE, GENRE_CARD_BOARD, GENRE_PUZZLE,
        GENRE_ADVENTURE_RPG, GENRE_SPORTS_RACING, GENRE_MMO_WHIRLED, GENRE_OTHER
    };

    /** Identifies our lobby background table media. */
    public static final String TABLE_MEDIA = "table";

    /** Identifies our server code media. */
    public static final String SERVER_CODE_MEDIA = "scode";

    /** Defines the number of different game types. See GameConfig. */
    public static final int GAME_TYPES = 3;

    /** Value of groupId when there is no associated group */
    public static final int NO_GROUP = 0;

    /** This game's genre. */
    public byte genre;

    /** The XML game configuration. */
    public String config;

    /** The primary game media. */
    public MediaDesc gameMedia;

    /** A unique identifier assigned to this game and preserved across new versions of the game
     * item so that ratings and lobbies and content packs all reference the same "game". */
    public int gameId;

    /** The game screenshot media. */
    public MediaDesc shotMedia;

    /**
     *  The server code media. Games may provide server code (in the form of a compiled action
     *  script library) to be run in a bureau whenever the game launches.
     *  @see com.threerings.bureau.BureauRegistry
     *  @see com.threerings.msoy.game.server.MsoyGameServer
     */
    public MediaDesc serverMedia;

    /** Optional group associated with this game; 0 means no group */
    public int groupId;

    /**
     * Returns true if the specified game is a developer's in-progress original game rather than
     * one listed in the catalog.
     */
    public static boolean isDevelopmentVersion (int gameId)
    {
        return (gameId < 0);
    }

    /**
     * Returns the id of the listed item for the given game id. Note this does not check to see
     * if the listing exists.
     */
    public static int getListedId (int gameId)
    {
        return gameId < 0 ? -gameId : gameId;
    }

    /**
     * Returns the id of the developement version of the given game id.
     */
    public static int getDevelopmentId (int gameId)
    {
        return -getListedId(gameId);
    }

    /**
     * Returns true if this is a developer's in-progress original game rather than one listed in
     * the catalog.
     */
    public boolean isDevelopmentVersion ()
    {
        return isDevelopmentVersion(gameId);
    }

    /**
     * Returns this game's screenshot media. Falls back to its thumbnail media in the absence of a
     * screenshot.
     */
    public MediaDesc getShotMedia ()
    {
        return (shotMedia == null) ? getThumbnailMedia() : shotMedia;
    }

    @Override // from Item
    public byte getType ()
    {
        return GAME;
    }

    @Override // from Item
    public SubItem[] getSubTypes ()
    {
        return (isInWorld() ?
                new SubItem[] {
                    new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), new Prop() } :
                new SubItem[] {
                    new LevelPack(), new ItemPack(), new TrophySource(), new Prize(), });
    }

    @Override // from Item
    public MediaDesc getPreviewMedia ()
    {
        if (furniMedia != null) {
            return furniMedia;
        }
        return getThumbnailMedia();
    }

    @Override // from Item
    public MediaDesc getPrimaryMedia ()
    {
        return gameMedia;
    }

    @Override // from Item
    public void setPrimaryMedia (MediaDesc desc)
    {
        gameMedia = desc;
    }

    /**
     * Checks whether this game is an in-world, as opposed to lobbied, game.
     */
    public boolean isInWorld ()
    {
        return (config != null) && (config.indexOf("<avrg/>") >= 0);
    }

    @Override
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(name, MAX_NAME_LENGTH) && (gameMedia != null);
    }
}
