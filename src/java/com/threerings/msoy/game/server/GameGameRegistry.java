//
// $Id$

package com.threerings.msoy.game.server;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.ResultListenerList;

import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.parlor.rating.util.Percentiler;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manages the lobbies active on this server.
 */
public class GameGameRegistry
    implements LobbyProvider, MsoyGameServer.Shutdowner, LobbyManager.ShutdownObserver
{
    /**
     * Initializes this registry.
     */
    public void init (RootDObjectManager omgr, InvocationManager invmgr, GameRepository gameRepo,
                      RatingRepository ratingRepo)
    {
        _omgr = omgr;
        _gameRepo = gameRepo;
        _ratingRepo = ratingRepo;
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);

        // register to hear when the server is shutdown
        MsoyGameServer.registerShutdowner(this);
    }

    /**
     * Returns the game repository used to maintain our persistent data.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Returns the percentiler for the specified game and score distribution. The percentiler may
     * be modified and when the lobby for the game in question is finally unloaded, the percentiler
     * will be written back out to the database.
     */
    public Percentiler getScoreDistribution (int gameId, boolean multiplayer)
    {
        return _distribs.get(multiplayer ? Math.abs(gameId) : -Math.abs(gameId));
    }

    // from LobbyProvider
    public void identifyLobby (ClientObject caller, final int gameId,
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        // if we're already resolving this lobby, add this listener to the list of those interested
        // in the outcome
        ResultListenerList list = _loading.get(gameId);
        if (list != null) {
            list.add(listener);
            return;
        }

        // if the lobby is already resolved, we're good
        LobbyManager mgr = _lobbies.get(gameId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        // otherwise we need to do the resolving
        _loading.put(gameId, list = new ResultListenerList());
        list.add(listener);

        MsoyGameServer.invoker.postUnit(new RepositoryUnit("loadLobby") {
            public void invokePersist () throws PersistenceException {
                GameRecord rec = _gameRepo.loadGameRecord(gameId);
                if (rec != null) {
                    _game = (Game)rec.toItem();
                    // load up the score distribution information for this game as well
                    _single = _ratingRepo.loadPercentile(-Math.abs(gameId));
                    _multi = _ratingRepo.loadPercentile(Math.abs(gameId));
                }
            }

            public void handleSuccess () {
                if (_game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                try {
                    LobbyManager lmgr = new LobbyManager(_omgr, _game, GameGameRegistry.this);
                    _lobbies.put(gameId, lmgr);

                    ResultListenerList list = _loading.remove(gameId);
                    if (list != null) {
                        list.requestProcessed(lmgr.getLobbyObject().getOid());
                    }

                    // map this game's score distributions
                    _distribs.put(-Math.abs(gameId), _single == null ? new Percentiler() : _single);
                    _distribs.put(Math.abs(gameId), _multi == null ? new Percentiler() : _multi);

                } catch (Exception e) {
                    handleFailure(e);
                }
            }

            public void handleFailure (Exception pe) {
                log.log(Level.WARNING, "Failed to resolve game [id=" + gameId + "].", pe);
                reportFailure(InvocationCodes.E_INTERNAL_ERROR);
            }

            protected void reportFailure (String reason) {
                ResultListenerList list = _loading.remove(gameId);
                if (list != null) {
                    list.requestFailed(reason);
                }

                // clear out the hosting record that our world server assigned to us when it sent
                // this client our way to resolve this game
                MsoyGameServer.worldClient.stoppedHostingGame(gameId);
            }

            protected Game _game;
            protected Percentiler _single, _multi;
        });
    }

    /**
     * Returns an enumeration of all of the registered lobby managers.  This should only be
     * accessed on the dobjmgr thread and shouldn't be kept around across event dispatches.
     */
    public Iterator<LobbyManager> enumerateLobbyManagers ()
    {
        return _lobbies.values().iterator();
    }

    // from interface MsoyServer.Shutdowner
    public void shutdown ()
    {
        // shutdown our active lobbies
        for (LobbyManager lmgr : _lobbies.values().toArray(new LobbyManager[_lobbies.size()])) {
            lobbyDidShutdown(lmgr.getGame());
        }
    }

    // from interface LobbyManager.ShutdownObserver
    public void lobbyDidShutdown (final Game game)
    {
        // destroy our record of that lobby
        _lobbies.remove(game.gameId);
        _loading.remove(game.gameId); // just in case

        // let our world server know we're audi
        MsoyGameServer.worldClient.stoppedHostingGame(game.gameId);

        // flush any modified percentile distributions
        flushPercentiler(-Math.abs(game.gameId)); // single-player
        flushPercentiler(Math.abs(game.gameId)); // multiplayer
    }

    protected void flushPercentiler (final int gameId)
    {
        final Percentiler tiler = _distribs.remove(gameId);
        if (tiler == null || !tiler.isModified()) {
            return;
        }

        MsoyGameServer.invoker.postUnit(new Invoker.Unit("flushPercentiler") {
            public boolean invoke () {
                try {
                    _ratingRepo.updatePercentile(gameId, tiler);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to update score distribution " +
                            "[game=" + gameId + ", tiler=" + tiler + "].", pe);
                }
                return false;
            }
        });
    }

    /** The distributed object manager that we work with. */
    protected RootDObjectManager _omgr;

    /** Provides access to game metadata. */
    protected GameRepository _gameRepo;

    /** Provides access to rating information. */
    protected RatingRepository _ratingRepo;

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> a mapping of various percentile distributions. */
    protected IntMap<Percentiler> _distribs = new HashIntMap<Percentiler>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loading = new HashIntMap<ResultListenerList>();
}
