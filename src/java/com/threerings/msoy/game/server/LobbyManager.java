//
// $Id$

package com.threerings.msoy.game.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.Interval;
import com.samskivert.util.Invoker;
import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.crowd.server.PlaceManagerDelegate;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.parlor.data.Parameter;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.server.ParlorSender;

import com.whirled.game.data.GameDefinition;
import com.whirled.game.data.TableMatchConfig;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.xml.MsoyGameParser;

import static com.threerings.msoy.Log.log;

/**
 * Manages a lobby room.
 */
@EventThread
public class LobbyManager
    implements LobbyObject.SubscriberListener
{
    /** Allows interested parties to know when a lobby shuts down. */
    public interface ShutdownObserver
    {
        void lobbyDidShutdown (int gameId);
    }

    /**
     * Initializes this lobby manager and prepares it for operation.
     */
    public void init (InvocationManager invmgr, ShutdownObserver shutObs)
    {
        _shutObs = shutObs;

        _lobj = _omgr.registerObject(new LobbyObject());
        _lobj.subscriberListener = this;
        _lobj.addListener(_tableWatcher);

        _tableMgr = new MsoyTableManager(_omgr, invmgr, _plreg, _worldClient, this);

        // since we start empty, we need to immediately assume shutdown
        recheckShutdownInterval();
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

    /**
     * Called when a lobby is first created and possibly again later to refresh its game metadata.
     */
    public void setGameContent (GameContent content)
    {
        // parse the game definition
        GameDefinition gameDef;
        try {
            gameDef = new MsoyGameParser().parseGame(content.game);
        } catch (Exception e) {
            log.warning("Error parsing game definition [id=" + content.game.gameId +
                ", err=" + e + "].");
            // however, we do not want to put the kibosh on the update. If someone
            // booches their game, we want the lobby to *fail fast*, not fail the next time
            // the server reboots...
            gameDef = null;
        }

        // accept the new game
        _content = content;

        // update the lobby object
        _lobj.startTransaction();
        try {
            _lobj.setGame(_content.game);
            _lobj.setGameDef(gameDef);
            _lobj.setGroupId(ServerConfig.getGameGroupId(_content.game.groupId));
        } finally {
            _lobj.commitTransaction();
        }
    }

    /**
     * Initialize the specified config.
     * Exposed to allow MsoyTableManager to call it.
     */
    public void initConfig (MsoyGameConfig config)
    {
        config.init(_lobj.game, _lobj.gameDef, ServerConfig.getGameGroupId(_lobj.game.groupId));
    }

    /**
     * Attempts to create and place the player into a single player game.
     *
     * @return if a game was created for the player, false if we were unable to create a game.
     */
    public boolean playNowSingle (PlayerObject player)
    {
        MsoyMatchConfig match = (MsoyMatchConfig)_lobj.gameDef.match;
        if (match == null || (match.minSeats != 1 && !match.isPartyGame)) {
            log.warning("Requested single player for non-single player game", "who", player.who(),
                        "gameId", getGameId(), "match", match);
            return false;
        }

        // start up a single player game
        MsoyGameConfig config = new MsoyGameConfig();
        initConfig(config);
        if (_lobj.gameDef.params != null) {
            for (Parameter param : _lobj.gameDef.params) {
                config.params.put(param.ident, param.getDefaultValue());
            }
        }
        TableConfig tconfig = new TableConfig();
        tconfig.desiredPlayerCount = tconfig.minimumPlayerCount = 1;
        tconfig.privateTable = true; // they asked to play by themselves
        Table table = null;
        try {
            table = _tableMgr.createTable(player, tconfig, config);
        } catch (InvocationException ie) {
            log.warning("Failed to create play now table [who=" + player.who() +
                        ", error=" + ie.getMessage() + "].");
            return false;
        }

        // if this is a party or seated continuous game, we need to tell the player to head
        // into the game because the game manager ain't oging to do it for us
        if (_lobj.gameDef.match.getMatchType() != MsoyGameConfig.SEATED_GAME) {
            ParlorSender.gameIsReady(player, table.gameOid);
        }
        return true;
    }

    /**
     * Attempts to send the specified player directly into a game.
     *
     * @return if the player was sent into a game, false if they should display the lobby.
     */
    public boolean playNowMulti (PlayerObject player, boolean friendsOnly)
    {
        // if this is a party game (or seated continuous); send them into an existing game
        if (_lobj.gameDef.match.getMatchType() != MsoyGameConfig.SEATED_GAME) {
            // TODO: order the tables most occupants to least?
            for (Table table : _lobj.tables) {
                if (table.gameOid > 0 && shouldJoinGame(player, table, friendsOnly)) {
                    ParlorSender.gameIsReady(player, table.gameOid);
                    return true;
                }
            }
        }

        // TODO: if we can add them to a table and that table will become immediately ready to
        // play, we could do that here and save the caller the trouble of subscribing to the lobby

        // otherwise we'll just send the player to the lobby and the client will look for a table
        // to join since it would have to download all that business anyway
        return false;
    }

    protected boolean shouldJoinGame (PlayerObject player, Table table, boolean friendsOnly)
    {
        // if this table has been marked as private, we don't want to butt in
        if (table.tconfig.privateTable) {
            return false;
        }

        // if the game is over its maximum capacity, don't join it
        int maxSeats = ((TableMatchConfig)_lobj.gameDef.match).maxSeats;
        if (table.watchers.length >= maxSeats) {
            return false;
        }

        // if we're not looking for friends, then this game is fine!
        if (!friendsOnly) {
            return true;
        }

        // see if any of our friends are in this game
        for (Name occ : table.players) {
            int memberId = ((MemberName)occ).getMemberId();
            if (player != null && player.friends.containsKey(memberId)) {
                return true;
            }
        }
        return false;
    }

    public void shutdown ()
    {
        _lobj.subscriberListener = null;
        _lobj.removeListener(_tableWatcher);

        _shutObs.lobbyDidShutdown(getGameId());

        _tableMgr.shutdown();

        // make sure we don't have any shutdowner in the queue
        cancelShutdowner();

        // finally, destroy the Lobby DObject
        _omgr.destroyObject(_lobj.getOid());
    }

    // from LobbyObject.SubscriberListener
    public void subscriberCountChanged (LobbyObject lobj)
    {
        recheckShutdownInterval();
    }

    /**
     * Called by {@link MsoyTableManager} when it wants to create a game.
     */
    protected GameManager createGameManager (GameConfig config)
        throws InstantiationException, InvocationException
    {
        List<PlaceManagerDelegate> delegates = Lists.newArrayList();
        delegates.add(new EventLoggingDelegate(_content));
        delegates.add(new TrackExperienceDelegate(_content));
        delegates.add(new AwardDelegate(_content));
        delegates.add(new TrophyDelegate(_content));
        delegates.add(new AgentTraceDelegate(getGameId()));
        return (GameManager)_plreg.createPlace(config, delegates);
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
                    @Override public void expired () {
                        log.debug("Unloading idle game lobby [gameId=" + getGameId() + "]");
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
    protected SetAdapter<Table> _tableWatcher = new SetAdapter<Table>() {
        @Override public void entryAdded (EntryAddedEvent<Table> event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                cancelShutdowner();
            }
        }
        @Override public void entryRemoved (EntryRemovedEvent<Table> event) {
            if (event.getName().equals(LobbyObject.TABLES)) {
                recheckShutdownInterval();
            }
        }
    };

    /** The Lobby object we're using. */
    protected LobbyObject _lobj;

    /** This fellow wants to hear when we shutdown. */
    protected ShutdownObserver _shutObs;

    /** The metadata for the game for which we're lobbying. */
    protected GameContent _content;

    /** Manages the actual tables. */
    protected MsoyTableManager _tableMgr;

    /** An interval to let us delay lobby shutdown for awhile. */
    protected Interval _shutdownInterval;

    // our dependencies
    @Inject protected RootDObjectManager _omgr;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected PlaceRegistry _plreg;
    @Inject protected WorldServerClient _worldClient;
    @Inject protected GameRepository _gameRepo;

    /** idle time before shutting down the manager. */
    protected static final long IDLE_UNLOAD_PERIOD = 60 * 1000L; // in ms
}
