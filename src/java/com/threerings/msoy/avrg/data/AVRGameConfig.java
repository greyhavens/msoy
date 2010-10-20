//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.util.ActionScript;

import com.threerings.crowd.data.PlaceConfig;

import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.orth.scene.data.EntityMedia;

/**
 * Configuration for an AVR game. This is basically BaseGameConfig + ParlorGameConfig,
 * but without the GameConfig dependency.
 */
public class AVRGameConfig extends PlaceConfig
    implements MsoyGameConfig
{
    /** The creator provided name of this game. */
    public String name;

    /** The description of this game. */
    public String description;

    /** The game's thumbnail media. */
    public EntityMedia thumbMedia;

    /** This game's splash screen media or null. */
    public EntityMedia splashMedia;

    /**
     * Configures this config with information from the supplied game metadata.
     */
    public void init (int gameId, GameSummary game, GameDefinition gameDef, EntityMedia splashMedia)
    {
        this.name = game.name;
        this.thumbMedia = game.thumbMedia;
        this.splashMedia = splashMedia;
        _gameId = gameId;
        _gameDef = gameDef;
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        String manager = getGameDefinition().manager;
        return (manager != null) ? manager : "com.threerings.msoy.avrg.server.AVRGameManager";
    }

    // from MsoyGameConfig
    public int getGameId ()
    {
        return _gameId;
    }

    // from MsoyGameConfig
    public GameDefinition getGameDefinition ()
    {
        return _gameDef;
    }

    /** Our game's unique id. */
    protected int _gameId;

    /** Our game definition. */
    protected GameDefinition _gameDef;
}
