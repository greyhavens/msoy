//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Extends Item with game info.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="GAME")
public class GameRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 5;

    public static final String NAME = "name";
    public static final String MIN_PLAYERS = "minPlayers";
    public static final String MAX_PLAYERS = "maxPlayers";
    public static final String PARTY_GAME_TYPE = "partyGameType";
    public static final String UNWATCHABLE = "unwatchable";
    public static final String GAME_MEDIA_HASH = "gameMediaHash";
    public static final String GAME_MIME_TYPE = "gameMimeType";

    /** The party game type. */
    public byte partyGameType;

    /** The minimum number of players. */
    public short minPlayers;

    /** The maximum number of players. */
    public short maxPlayers;

    /** If a non-party game, it may be unwatchable. */
    public boolean unwatchable;

    /** The XML game configuration. */
    @Column(columnDefinition="config TEXT NOT NULL")
    public String config;

    /** A hash code identifying the game media. */
    public byte[] gameMediaHash;

    /** The MIME type of the {@link #gameMediaHash} media. */
    public byte gameMimeType;

    public GameRecord ()
    {
        super();
    }

    protected GameRecord (Game game)
    {
        super(game);

        partyGameType = game.partyGameType;
        minPlayers = game.minPlayers;
        maxPlayers = game.maxPlayers;
        unwatchable = game.unwatchable;
        config = game.config;
        if (game.gameMedia != null) {
            gameMediaHash = game.gameMedia.hash;
            gameMimeType = game.gameMedia.mimeType;
        }
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.GAME;
    }

    @Override // from Item
    protected Item createItem ()
    {
        Game object = new Game();
        object.partyGameType = partyGameType;
        object.minPlayers = minPlayers;
        object.maxPlayers = maxPlayers;
        object.unwatchable = unwatchable;
        object.config = config;
        object.gameMedia = gameMediaHash == null ? null :
            new MediaDesc(gameMediaHash, gameMimeType);
        return object;
    }
}
