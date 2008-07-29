//
// $Id: AVRGameConfig.java 9198 2008-05-16 19:21:43Z jamie $

package com.threerings.msoy.game.data;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.util.ActionScript;
import com.threerings.toybox.data.ToyBoxGameConfig;
import com.whirled.game.data.GameDefinition;
import com.threerings.msoy.item.data.all.Game;

/**
 * Configuration for an AVR game.
 */
public class AVRGameConfig extends PlaceConfig
{
    /** The creator provided name of this game. */
    public String name;

    /**
     * Configures this config with information from the supplied {@link Game} item.
     */
    public void init (Game game, GameDefinition gameDef)
    {
        this.name = game.name;
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

    public int getGameId ()
    {
        return _gameId;
    }

    /** Our game's unique id. */
    protected int _gameId;

    /** Our game definition. */
    protected GameDefinition _gameDef;
}
