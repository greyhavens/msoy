//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.Interval;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.server.PlaceManagerDelegate;
import com.threerings.parlor.game.server.GameManager;

import com.threerings.ezgame.data.GameDefinition;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.log;

/**
 * Manages a lobby room.
 */
public class LobbyManager
    implements LobbyObject.SubscriberListener
{
    public interface ShutdownObserver
    {
        public void lobbyDidShutdown (Game game);
    }

    /**
     * Create a new LobbyManager.
     *
     * @param game The game we're managing a lobby for.
     */
    public LobbyManager (RootDObjectManager omgr, ShutdownObserver shutObs)
    {
        _omgr = omgr;
        _shutObs = shutObs;

        _lobj = _omgr.registerObject(new LobbyObject());
        _lobj.subscriberListener = this;
        _lobj.addListener(_tableWatcher);

        _tableMgr = new MsoyTableManager(this);

        // since we start empty, we need to immediately assume shutdown
        recheckShutdownInterval();
    }

    /**
     * Called when a lobby is first created and possibly again later to refresh its game metadata.
     */
    public void setGameContent (GameContent content)
        throws Exception
    {
        _content = content;

        _lobj.setGame(_content.game);
        _lobj.setGameDef(new MsoyGameParser().parseGame(content.game));

//         // if our game object is mutable, listen for updates from the ItemManager
//         if (_content.game.sourceId == 0) {
//             _uplist = new ItemManager.ItemUpdateListener() {
//                 public void itemUpdated (ItemRecord item) {
//                     Game game = (Game)item.toItem();
//                     if (game.gameId == getGameId()) {
//                         updateGame(game);
//                     }
//                 }
//             };
//             MsoyServer.itemMan.registerItemUpdateListener(GameRecord.class, _uplist);
//         }
    }

    public void shutdown ()
    {
        _lobj.subscriberListener = null;
        _lobj.removeListener(_tableWatcher);

//         // if our game is mutable, clear our update listener
//         if (_uplist != null) {
//             MsoyServer.itemMan.removeItemUpdateListener(GameRecord.class, _uplist);
//         }

        _shutObs.lobbyDidShutdown(_content.game);

        _tableMgr.shutdown();

        // make sure we don't have any shutdowner in the queue
        cancelShutdowner();

        // finally, destroy the Lobby DObject
        _omgr.destroyObject(_lobj.getOid());
    }

    /**
     * Returns the metadata record for the game hosted by this lobby.
     */
    public Game getGame ()
    {
        return _content.game;
    }

    /**
     * Return the ID of the game for which we're the lobby.
     */
    public int getGameId ()
    {
        return _content.game.gameId;
    }

    /**
     * Return the object ID of the LobbyObject
     */
    public LobbyObject getLobbyObject ()
    {
        return _lobj;
    }

    // from LobbyObject.SubscriberListener
    public void subscriberCountChanged (LobbyObject lobj)
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
            log.warning("Error parsing game definition [id=" + game.gameId + ", err=" + e + "].");
            return;
        }

        // Accept the new game, and update the lobby object
        _content.game = game;
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
     * Called by the {@link MsoyTableManager} when a game is created.
     */
    protected void gameCreated (GameManager gmgr)
    {
        gmgr.applyToDelegates(new GameManager.DelegateOp() {
            public void apply (PlaceManagerDelegate delegate) {
                if (delegate instanceof WhirledGameDelegate) {
                    ((WhirledGameDelegate)delegate).setGameContent(_content);
                }
            }
        });
    }

    /**
     * Check the current status of the lobby and maybe schedule or maybe cancel the shutdown
     * interval, as appropriate.
     */
    protected void recheckShutdownInterval ()
    {
        if (_lobj.getSubscriberCount() == 0 && _lobj.tables.size() == 0) {
            // queue up a shutdown interval, unless we've already got one.
            if (_shutdownInterval == null) {
                _shutdownInterval = new Interval(_omgr) {
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

    /** Our distributed object manager. */
    protected RootDObjectManager _omgr;

    /** The Lobby object we're using. */
    protected LobbyObject _lobj;

    /** This fellow wants to hear when we shutdown. */
    protected ShutdownObserver _shutObs;

    /** The metadata for the game for which we're lobbying. */
    protected GameContent _content;

    /** Manages the actual tables. */
    protected MsoyTableManager _tableMgr;

    /** Used to listen for updates to our game item if necessary. */
    protected ItemManager.ItemUpdateListener _uplist;

    /** An interval to let us delay lobby shutdown for awhile. */
    protected Interval _shutdownInterval;

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms
}
