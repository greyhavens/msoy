//
// $Id$

package com.threerings.msoy.game.data;

import com.threerings.util.ActionScript;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.WhirledGameConfig;

import com.threerings.orth.data.MediaDesc;

/**
 * Configuration for a Parlor game. Parlor games take over the entire display and don't leverage
 * any of the Whirled virtual world stuff.
 */
@com.threerings.util.ActionScript(omit=true)
public class ParlorGameConfig extends WhirledGameConfig
    implements MsoyGameConfig
{
    /** Info on the game being played. */
    public GameSummary game;

    /** The game's groupId, or 0 for none. */
    public int groupId;

    /** This game's splash screen media or null. */
    public MediaDesc splashMedia;

    /**
     * Configures this config with information from the supplied game bits.
     */
    public void init (int gameId, GameSummary game, GameDefinition gameDef,
                      int groupId, MediaDesc splash)
    {
        this.game = game;
        this.groupId = groupId;
        this.splashMedia = splash;
        _gameId = gameId;
        _gameDef = gameDef;
    }

    @Override @ActionScript(omit=true)
    public String getManagerClassName ()
    {
        String manager = getGameDefinition().manager;
        return (manager != null) ? manager : "com.threerings.msoy.game.server.ParlorGameManager";
    }
}
