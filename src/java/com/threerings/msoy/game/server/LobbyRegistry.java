//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.RepositoryUnit;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.ResultListenerList;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manages the lobbies active on this server.
 */
public class LobbyRegistry
    implements LobbyProvider, LobbyManager.ShutdownObserver
{
    /**
     * Initializes this registry.
     */
    public void init (RootDObjectManager omgr, InvocationManager invmgr, GameRepository gameRepo)
    {
        _omgr = omgr;
        _gameRepo = gameRepo;
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);
    }

    public RootDObjectManager getDObjectManager ()
    {
        return _omgr;
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

        MsoyServer.invoker.postUnit(new RepositoryUnit() {
            public void invokePersist () throws PersistenceException {
                GameRecord rec = _gameRepo.loadItem(gameId);
                _game = (rec == null) ? null : (Game)rec.toItem();
            }

            public void handleSuccess () {
                if (_game == null) {
                    reportFailure("m.no_such_game");
                    return;
                }

                try {
                    LobbyManager lmgr = new LobbyManager(_omgr, _game, LobbyRegistry.this);
                    _lobbies.put(gameId, lmgr);

                    ResultListenerList list = _loading.remove(gameId);
                    if (list != null) {
                        list.requestProcessed(lmgr.getLobbyObject().getOid());
                    }

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

    // from interface LobbyManager.ShutdownObserver
    public void lobbyDidShutdown (Game game)
    {
        // destroy our record of that lobby
        _lobbies.remove(game.itemId);
        _loading.remove(game.itemId); // just in case

        // let our world server know we're audi
        MsoyGameServer.worldClient.stoppedHostingGame(game.itemId);
    }

    /** The distributed object manager that we work with. */
    protected RootDObjectManager _omgr;

    /** Provides access to game metadata. */
    protected GameRepository _gameRepo;

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected IntMap<ResultListenerList> _loading = new HashIntMap<ResultListenerList>();
}
