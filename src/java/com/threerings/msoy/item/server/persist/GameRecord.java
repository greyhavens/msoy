//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

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
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameType} field. */
    public static final String GAME_TYPE = "gameType";

    /** The qualified column identifier for the {@link #gameType} field. */
    public static final ColumnExp GAME_TYPE_C =
        new ColumnExp(GameRecord.class, GAME_TYPE);

    /** The column identifier for the {@link #minPlayers} field. */
    public static final String MIN_PLAYERS = "minPlayers";

    /** The qualified column identifier for the {@link #minPlayers} field. */
    public static final ColumnExp MIN_PLAYERS_C =
        new ColumnExp(GameRecord.class, MIN_PLAYERS);

    /** The column identifier for the {@link #maxPlayers} field. */
    public static final String MAX_PLAYERS = "maxPlayers";

    /** The qualified column identifier for the {@link #maxPlayers} field. */
    public static final ColumnExp MAX_PLAYERS_C =
        new ColumnExp(GameRecord.class, MAX_PLAYERS);

    /** The column identifier for the {@link #unwatchable} field. */
    public static final String UNWATCHABLE = "unwatchable";

    /** The qualified column identifier for the {@link #unwatchable} field. */
    public static final ColumnExp UNWATCHABLE_C =
        new ColumnExp(GameRecord.class, UNWATCHABLE);

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
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 6 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** The game type. */
    public byte gameType;

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

        gameType = game.gameType;
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
        object.gameType = gameType;
        object.minPlayers = minPlayers;
        object.maxPlayers = maxPlayers;
        object.unwatchable = unwatchable;
        object.config = config;
        object.gameMedia = gameMediaHash == null ? null :
            new MediaDesc(gameMediaHash, gameMimeType);
        return object;
    }
}
