//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.util.ActionScript;
import com.whirled.game.data.GameDefinition;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;

/**
 * Configuration for an AVR game. This is basically BaseGameConfig + MsoyGameConfig,
 * but without the GameConfig dependency.
 */
public class AVRGameConfig extends PlaceConfig
{
    /** The creator provided name of this game. */
    public String name;

    /** The game's thumbnail media. */
    public MediaDesc thumbnail;

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public void init (Game game, GameDefinition gameDef)
    {
        this.name = game.name;
        this.thumbnail = game.getThumbnailMedia();
        _gameId = game.gameId;
        _gameDef = gameDef;
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        String manager = getGameDefinition().manager;
        return (manager != null) ? manager : "com.threerings.msoy.avrg.server.AVRGameManager";
    }

    /**
     * Returns the non-changing metadata that defines this game.
     */
    public GameDefinition getGameDefinition ()
    {
        return _gameDef;
    }

    /**
     * Returns the gameId of this game.
     */
    public int getGameId ()
    {
        return _gameId;
    }

    /** Our game's unique id. */
    protected int _gameId;

    /** Our game definition. */
    protected GameDefinition _gameDef;
}
