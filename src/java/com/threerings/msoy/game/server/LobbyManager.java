//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.parlor.server.TableManager;
import com.threerings.parlor.server.TableManagerProvider;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.LobbyConfig;

/**
 * Manages a lobby room.
 */
public class LobbyManager extends PlaceManager
    implements TableManagerProvider
{
    // from TableManagerProvider
    public TableManager getTableManager ()
    {
        return _tableMgr;
    }

    @Override
    public void startup (PlaceObject plobj)
    {
        super.startup(plobj);

        MsoyServer.lobbyReg.lobbyStartup(_gameId, plobj.getOid());
    }

    @Override
    public void shutdown ()
    {
        super.shutdown();

        MsoyServer.lobbyReg.lobbyShutdown(_gameId);
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // remember our game id
        _gameId = ((LobbyConfig) _config).game.itemId;
    }

    /** The game id for which we're lobbying. */
    protected int _gameId;

    /** Manages the actual tables. */
    protected TableManager _tableMgr;
}
