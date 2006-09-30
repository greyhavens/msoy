//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

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
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 2;

    public static final String NAME = "name";
    public static final String MIN_PLAYERS = "minPlayers";
    public static final String MAX_PLAYERS = "maxPlayers";
    public static final String DESIRED_PLAYERS = "desiredPlayers";
    public static final String GAME_MEDIA_HASH = "gameMediaHash";
    public static final String GAME_MIME_TYPE = "gameMimeType";

    /** The name of the game. */
    @Column(nullable=false)
    public String name;

    /** The minimum number of players. */
    @Column(nullable=false)
    public short minPlayers;

    /** The maximum number of players. */
    @Column(nullable=false)
    public short maxPlayers;

    /** The desired number of players. */
    @Column(nullable=false)
    public short desiredPlayers;

    /** The XML game configuration. */
    @Column(columnDefinition="config TEXT NOT NULL")
    public String config;

    /** A hash code identifying the game media. */
    @Column(nullable=false)
    public byte[] gameMediaHash;

    /** The MIME type of the {@link #gameMediaHash} media. */
    @Column(nullable=false)
    public byte gameMimeType;

    public GameRecord ()
    {
        super();
    }

    protected GameRecord (Game game)
    {
        super(game);

        name = game.name;
        minPlayers = game.minPlayers;
        maxPlayers = game.maxPlayers;
        desiredPlayers = game.desiredPlayers;
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
        object.name = name;
        object.minPlayers = minPlayers;
        object.maxPlayers = maxPlayers;
        object.desiredPlayers = desiredPlayers;
        object.config = config;
        object.gameMedia = gameMediaHash == null ? null :
            new MediaDesc(gameMediaHash, gameMimeType);
        return object;
    }
}
