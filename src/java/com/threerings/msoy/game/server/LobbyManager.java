//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.parlor.server.TableManager;
import com.threerings.parlor.server.TableManagerProvider;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.LobbyConfig;
import com.threerings.msoy.game.data.LobbyObject;

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
        _tableMgr = new TableManager(this);

        plobj.addListener(_tableWatcher);
    }

    @Override
    public void shutdown ()
    {
        _plobj.removeListener(_tableWatcher);
        MsoyServer.lobbyReg.lobbyShutdown(_gameId);

        super.shutdown();
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new LobbyObject();
    }

    @Override
    protected void didInit ()
    {
        super.didInit();

        // remember our game id
        _gameId = ((LobbyConfig) _config).game.itemId;
    }

    @Override
    protected void checkShutdownInterval ()
    {
        if (_plobj.occupants.size() == 0 &&
                ((LobbyObject) _plobj).tables.size() == 0) {
            super.checkShutdownInterval();
        }
    }

    /** The game id for which we're lobbying. */
    protected int _gameId;

    /** Manages the actual tables. */
    protected TableManager _tableMgr;

    /** Listens for table removal and considers destroying the room. */
    protected SetAdapter _tableWatcher = new SetAdapter() {
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                checkShutdownInterval();
            }
        }

        // new tables can't be created without someone entering,
        // so if we do end up scheduling a shutdown interval after
        // the last table disappears then it will be cancelled when
        // someone enters.
    };
}
