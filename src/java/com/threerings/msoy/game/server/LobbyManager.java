//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManager;

import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.server.TableManager;
import com.threerings.parlor.server.TableManagerProvider;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.LobbyConfig;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;

import static com.threerings.msoy.Log.log;

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

        MsoyServer.lobbyReg.lobbyStartup(getGameId(), plobj.getOid());
        _tableMgr = new TableManager(plobj) {
            protected GameConfig createConfig (Table table) {
                MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
                // fill in our game id and name
                Game game = ((LobbyObject)_plobj).game;
                config.persistentGameId = game.getPrototypeId();
                config.name = game.name;
                return config;
            }
        };
        _tableMgr.setTableClass(MsoyTable.class);
        plobj.addListener(_tableWatcher);
    }

    @Override
    public void shutdown ()
    {
        _plobj.removeListener(_tableWatcher);
        MsoyServer.lobbyReg.lobbyShutdown(getGameId());

        super.shutdown();
    }

    /**
     * Return the ID of the game for which we're the lobby.
     */
    public int getGameId ()
    {
        return _gameId;
    }
    
    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new LobbyObject();
    }

    @Override
    protected void didStartup ()
    {
        super.didStartup();

        // publish our game in the lobby object and remember its id
        Game game = ((LobbyConfig)_config).game;
        ((LobbyObject)_plobj).setGame(game);
        _gameId = game.itemId;

        // if our game is mutable, listen for updates to the GameRecord
        if (game.parentId == 0) {
            _uplist = new ItemManager.ItemUpdateListener() {
                public void itemUpdated (ItemRecord item) {
                    if (item.itemId == getGameId()) {
                        // update the game in the lobby object, everything else is groovy
                        ((LobbyObject)_plobj).setGame((Game)item.toItem());
                    }
                }
            };
            MsoyServer.itemMan.registerItemUpdateListener(GameRecord.class, _uplist);
        }
    }

    @Override
    protected void didShutdown ()
    {
        super.didShutdown();

        // if our game is mutable, clear our update listener
        if (_uplist != null) {
            MsoyServer.itemMan.removeItemUpdateListener(GameRecord.class, _uplist);
        }
    }

    @Override
    protected void checkShutdownInterval ()
    {
        if (_plobj.occupants.size() == 0 && ((LobbyObject) _plobj).tables.size() == 0) {
            super.checkShutdownInterval();
        }
    }

    /** The game for which we're lobbying. */
    protected int _gameId;

    /** Manages the actual tables. */
    protected TableManager _tableMgr;

    /** Used to listen for updates to our game item if necessary. */
    protected ItemManager.ItemUpdateListener _uplist;

    /** Listens for table removal and considers destroying the room. */
    protected SetAdapter _tableWatcher = new SetAdapter() {
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                checkShutdownInterval();
            }
        }
        // new tables can't be created without someone entering, so if we do end up scheduling a
        // shutdown interval after the last table disappears then it will be cancelled when someone
        // enters.
    };
}
