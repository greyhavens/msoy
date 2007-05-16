//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.Interval;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.ezgame.data.GameDefinition;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.SubscriberListener;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.log;

/**
 * Manages a lobby room.
 */
public class LobbyManager 
    implements SubscriberListener
{
    /**
     * Create a new LobbyManager.
     *
     * @param game The game we're managing a lobby for.
     */
    public LobbyManager (Game game, GameDefinition gameDef)
    {
        _game = game;
        _lobj = MsoyServer.omgr.registerObject(new LobbyObject());
        _lobj.subscriberListener = this;
        _lobj.setGame(_game);
        _lobj.setGameDef(gameDef);

        // if our game object is mutable, listen for updates from the ItemManager
        if (_game.parentId == 0) {
            _uplist = new ItemManager.ItemUpdateListener() {
                public void itemUpdated (ItemRecord item) {
                    if (item.itemId == getGameId()) {
                        updateGame((Game) item.toItem());
                    }
                }
            };
            MsoyServer.itemMan.registerItemUpdateListener(GameRecord.class, _uplist);
        }

        _tableMgr = new MsoyTableManager(_lobj);
        _lobj.addListener(_tableWatcher);

        // since we start empty, we need to immediately assume shutdown
        recheckShutdownInterval();
    }

    public void shutdown ()
    {
        _lobj.subscriberListener = null;
        _lobj.removeListener(_tableWatcher);

        // if our game is mutable, clear our update listener
        if (_uplist != null) {
            MsoyServer.itemMan.removeItemUpdateListener(GameRecord.class, _uplist);
        }

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

    // from SubscriberListener
    public void subscriberCountChanged (DObject target)
    {
        recheckShutdownInterval();
    }

    /**
     * Update the game item associated with this lobby.
     */
    protected void updateGame (Game game)
    {
        // update the game in the lobby object, everything else is groovy
        GameDefinition gameDef;
        try {
            gameDef = new MsoyGameParser().parseGame(game);

        } catch (Exception e) {
            log.warning("Error parsing new game definition [gameId=" + game.itemId +
                ", err=" + e + "].");
            return;
        }

        // Accept the new game, and update the lobby object
        _game = game;
        _lobj.startTransaction();
        try {
            _lobj.setGame(game);
            _lobj.setGameDef(gameDef);
        } finally {
            _lobj.commitTransaction();
        }
        _tableMgr.gameUpdated();
    }
    
    /**
     * Check the current status of the lobby and maybe schedule or maybe cancel the shutdown
     * interval, as appropriate.
     */
    protected void recheckShutdownInterval ()
    {
        //System.err.println("Checking lobby: subscribers=" + _lobj.getSubscriberCount() +
        //        ", tables=" + _lobj.tables.size());
        if (_lobj.getSubscriberCount() == 0 && _lobj.tables.size() == 0) {
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

        } else { 
            cancelShutdowner();
        }
    }

    /**
     * Unconditionally cancel the shutdown interval.
     */
    protected void cancelShutdowner ()
    {
        if (_shutdownInterval != null) {
            _shutdownInterval.cancel();
            _shutdownInterval = null;
        }
    }

    /** The Lobby object we're using. */
    protected LobbyObject _lobj;

    /** The game for which we're lobbying. */
    protected Game _game;

    /** Manages the actual tables. */
    protected MsoyTableManager _tableMgr;

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
                recheckShutdownInterval();
            }
        } 
    };

    /** interval to let us delay lobby shutdown for awhile, in case a new table is created 
     * immediately */
    protected Interval _shutdownInterval;

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms
}
