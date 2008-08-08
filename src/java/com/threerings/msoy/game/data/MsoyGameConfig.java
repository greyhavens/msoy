//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;
import com.threerings.toybox.data.ToyBoxGameConfig;
import com.whirled.game.data.GameDefinition;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;

/**
 * A game config for a metasoy game.
 */
public class MsoyGameConfig extends ToyBoxGameConfig
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
        return (manager != null) ? manager : "com.threerings.msoy.game.server.MsoyGameManager";
    }
}
