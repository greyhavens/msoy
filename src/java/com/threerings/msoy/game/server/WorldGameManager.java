//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.WorldGameObject;

/**
 * Manages an in-world game.
 */
public class WorldGameManager extends EZGameManager
{
    @Override // documentation inherited
    public void startup (PlaceObject plobj)
    {
        super.startup(plobj);
        MsoyServer.worldGameReg.gameStartup(_gameId, plobj.getOid());
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        MsoyServer.worldGameReg.gameShutdown(_gameId);
        super.shutdown();
    }
    
    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new WorldGameObject();
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // remember our game id
        _gameId = ((WorldGameConfig)_config).persistentGameId;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();
        ((WorldGameObject)_plobj).config = (WorldGameConfig)_config;
    }
    
    /** The id of the world game. */
    protected int _gameId;
}
