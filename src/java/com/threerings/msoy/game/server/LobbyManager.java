//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.Interval;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.server.TableManager;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.web.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyTable;

import static com.threerings.msoy.Log.log;

/**
 * Manages a lobby room.
 */
public class LobbyManager 
{
    /**
     * Create a new LobbyManager.
     *
     * @param game The game we're managing a lobby for.
     */
    public LobbyManager (Game game)
    {
        _game = game;
        _lobj = MsoyServer.omgr.registerObject(new LobbyObject());
        _lobj.setGame(_game);

        // if our game object is mutable, listen for updates from the ItemManager
        if (_game.parentId == 0) {
            _uplist = new ItemManager.ItemUpdateListener() {
                public void itemUpdated (ItemRecord item) {
                    if (item.itemId == getGameId()) {
                        // update the game in the lobby object, everything else is groovy
                        _lobj.setGame((Game)item.toItem());
                    }
                }
            };
            MsoyServer.itemMan.registerItemUpdateListener(GameRecord.class, _uplist);
        }

        _tableMgr = new TableManager(_lobj) {
            protected GameConfig createConfig (Table table) {
                MsoyGameConfig config = (MsoyGameConfig)super.createConfig(table);
                // fill in our game id and name
                Game game = _lobj.game;
                config.persistentGameId = game.getPrototypeId();
                config.name = game.name;
                return config;
            }
            protected void purgeTable (Table table) {
                super.purgeTable(table);
                checkShutdownInterval();
            }
        };
        _tableMgr.setTableClass(MsoyTable.class);
        _lobj.addListener(_tableWatcher);

        // since we start empty, we need to immediately assume shutdown
        checkShutdownInterval();
    }

    public void shutdown ()
    {
        _lobj.removeListener(_tableWatcher);
        MsoyServer.lobbyReg.lobbyShutdown(getGameId());

        _tableMgr.shutdown();

        // make sure we don't have any shutdowner in the queue
        cancelShutdowner();

        // finally, destroy the Lobby DObject
        MsoyServer.omgr.destroyObject(_lobj.getOid());
    }

    /**
     * Return the ID of the game for which we're the lobby.
     */
    public int getGameId ()
    {
        return _game.itemId;
    }

    /**
     * Return the object ID of the LobbyObject
     */
    public LobbyObject getLobbyObject ()
    {
        return _lobj;
    }
    
    protected void didShutdown ()
    {
        // if our game is mutable, clear our update listener
        if (_uplist != null) {
            MsoyServer.itemMan.removeItemUpdateListener(GameRecord.class, _uplist);
        }
    }

    protected void checkShutdownInterval ()
    {
        if (_lobj.tables.size() == 0) {
            // queue up a shutdown interval, unless we've already got one.
            if (_shutdownInterval == null) {
                _shutdownInterval = new Interval(MsoyServer.omgr) {
                    public void expired () {
                        log.fine("Unloading idle game lobby [gameId=" + getGameId() + "]");
                        shutdown();
                    }
                };
                _shutdownInterval.schedule(IDLE_UNLOAD_PERIOD);
            }
        } 
    }

    protected void cancelShutdowner ()
    {
        if (_shutdownInterval != null) {
            _shutdownInterval.cancel();
            _shutdownInterval = null;
        }
    }

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms

    /** The Lobby object we're using. */
    protected LobbyObject _lobj;

    /** The game for which we're lobbying. */
    protected Game _game;

    /** Manages the actual tables. */
    protected TableManager _tableMgr;

    /** Used to listen for updates to our game item if necessary. */
    protected ItemManager.ItemUpdateListener _uplist;

    /** Listens for table removal/addition and considers destroying the room. */
    protected SetAdapter _tableWatcher = new SetAdapter() {
        public void entryAdded (EntryAddedEvent event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                cancelShutdowner();
            }
        }
        public void entryRemoved (EntryRemovedEvent event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                checkShutdownInterval();
            }
        } 
    };

    /** interval to let us delay lobby shutdown for awhile, in case a new table is created 
     * immediately */
    protected Interval _shutdownInterval;
}
