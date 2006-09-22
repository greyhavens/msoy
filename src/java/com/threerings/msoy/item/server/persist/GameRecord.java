//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;

/**
 * Extends Item with game info.
 */
@Entity
@Table
@TableGenerator(
    name="itemId",
    allocationSize=1,
    pkColumnValue="GAME")
public class GameRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

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

    public GameRecord (Game game)
    {
        super(game);

        this.name = game.name;
        this.minPlayers = game.minPlayers;
        this.maxPlayers = game.maxPlayers;
        this.desiredPlayers = game.desiredPlayers;
        this.gameMediaHash = game.gameMediaHash == null ?
            null : game.gameMediaHash.clone();
        this.gameMimeType = game.gameMimeType;
    }

    @Override // from Item
    public ItemEnum getType ()
    {
        return ItemEnum.GAME;
    }
    
    @Override
    public Object clone ()
    {
        GameRecord clone = (GameRecord) super.clone();
        clone.gameMediaHash = gameMediaHash.clone();
        return clone;
    }

    @Override
    protected Item createItem ()
    {
        Game object = new Game();
        object.gameMediaHash = this.gameMediaHash == null ?
            null : this.gameMediaHash;
        object.gameMimeType = this.gameMimeType;
        object.name = this.name;
        object.minPlayers = this.minPlayers;
        object.maxPlayers = this.maxPlayers;
        object.desiredPlayers = this.desiredPlayers;
        return object;
    }
}
