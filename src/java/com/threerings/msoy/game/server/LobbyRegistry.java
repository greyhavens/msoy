//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.logging.Level;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;

import com.threerings.presents.dobj.DObject;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.SubscriberListener;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.data.HostedGame;

import static com.threerings.msoy.Log.log;

/**
 * Manages the lobbies in use.
 */
public class LobbyRegistry
    implements LobbyProvider
{
    /**
     * Initialize the lobby registry.
     */
    public void init (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new LobbyDispatcher(this), MsoyCodes.GAME_GROUP);
    }

    // from LobbyProvider
    public void identifyLobby (ClientObject caller, final int gameId,
                               final InvocationService.ResultListener listener)
        throws InvocationException
    {
        // if its hosted on this server, give them that...
        LobbyManager mgr = _lobbies.get(gameId);
        if (mgr != null) {
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        // if we've already got the proxy oid, give them that...
        int proxyOid = _proxies.get(gameId);
        if (proxyOid != -1) {
            listener.requestProcessed(proxyOid);
            return;
        }

        // if we're already resolving this lobby, add this listener to the list of those 
        // interested in the outcome
        ArrayList<InvocationService.ResultListener> list = _loading.get(gameId);
        if (list != null) {
            list.add(listener);
            return;
        }

        // start a new list for this gameId
        list = new ArrayList<InvocationService.ResultListener>();
        list.add(listener);
        _loading.put(gameId, list);
        
        // if its hosted on another server, we need to proxy the lobby here.
        final Tuple<String, Integer> gameInfo = MsoyServer.peerMan.getGameHost(gameId);
        if (gameInfo != null) {
            MsoyServer.peerMan.proxyRemoteObject(gameInfo.left, gameInfo.right,
                    new ResultListener<Integer>() {
                public void requestCompleted (Integer proxyOid) {
                    _proxies.put(gameId, proxyOid);
                    notifyLoadingComplete(gameId, proxyOid);

                    // if all of our local lobby subscribers unsubscribe, we need to stop 
                    // proxying...
                    LobbyObject lobj = ((LobbyObject) MsoyServer.omgr.getObject(proxyOid));
                    lobj.subscriberListener = new SubscriberListener() {
                        public void subscriberCountChanged (DObject target) {
                            LobbyObject lobj = (LobbyObject) target;
                            if (lobj.getSubscriberCount() == 1) { // PeerManager is always there
                                _proxies.remove(gameId);
                                notifyLoadingFailed(gameId); // just in case
                                MsoyServer.peerMan.unproxyRemoteObject(
                                    gameInfo.left, gameInfo.right);
                            }
                        }
                    };
                }
                public void requestFailed (Exception cause) {
                    log.log(Level.WARNING, "Game lobby proxy subscription failed [gameId=" +
                        gameId + "]", cause);
                    notifyLoadingFailed(gameId);
                }
            });
            return;
        }

        // otherwise this lonely little game isn't being played by anyone right now... we get to
        // host it.
        MsoyServer.peerMan.acquireLock(
                MsoyPeerManager.getGameLock(gameId), new ResultListener<String>() {
            public void requestCompleted (String nodeName) {
                if (MsoyServer.peerMan.getNodeObject().nodeName.equals(nodeName)) {
                    log.info("Got lock, resolving game " + gameId + ".");
                    resolveLobby(gameId);
                } else if (nodeName != null) {
                    log.info("someone else got the lock: " + nodeName);
                    // TODO - listen on hostedGames to get the lobby oid when the lock holder
                    // finishes resolving the lobby.
                    notifyLoadingFailed(gameId);
                } else {
                    log.warning("Game lock acquired by null? [id=" + gameId + "].");
                    notifyLoadingFailed(gameId);
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Failed to acquire game resolution lock " + 
                    "[id=" + gameId + "].", cause);
                notifyLoadingFailed(gameId);
            }
        });
    

    }

    /**
     * Called by LobbyManager instances when they start to shut down and
     * destroy their dobject.
     */
    public void lobbyShutdown (int gameId)
    {
        // destroy our record of that lobby
        _lobbies.remove(gameId);
        notifyLoadingFailed(gameId); // just in case
        MsoyServer.peerMan.lobbyDidShutdown(gameId);
    }

    /**
     * Returns an enumeration of all of the registered lobby managers.
     * This should only be accessed on the dobjmgr thread and shouldn't be
     * kept around across event dispatches.
     */
    public Iterator<LobbyManager> enumerateLobbyManagers ()
    {
        return _lobbies.values().iterator();
    }

    protected void resolveLobby (final int gameId)
    {
        MsoyServer.itemMan.getItem(new ItemIdent(Item.GAME, gameId), 
                new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                try {
                    LobbyManager lmgr = new LobbyManager(
                        (Game)item, new MsoyGameParser().parseGame((Game)item));
                    // record the lobby oid for the game
                    _lobbies.put(gameId, lmgr);
                    notifyLoadingComplete(gameId, lmgr.getLobbyObject().getOid());

                    // add this game lobby to our hostedGames list
                    MsoyServer.peerMan.lobbyDidStartup(
                        gameId, item.name, lmgr.getLobbyObject().getOid());

                } catch (Exception e) {
                    requestFailed(e);
                }
            }
            public void requestFailed (Exception cause)
            {
                MsoyServer.peerMan.releaseLock(
                    MsoyPeerManager.getGameLock(gameId), new ResultListener.NOOP<String>());
                notifyLoadingFailed(gameId);
            }
        });
    }

    protected void notifyLoadingComplete (int gameId, int lobbyOid) 
    {
        ArrayList<InvocationService.ResultListener> list = _loading.remove(gameId);
        if (list != null) {
            for (InvocationService.ResultListener rList : list) {
                rList.requestProcessed(lobbyOid);
            }
        }
    }

    protected void notifyLoadingFailed (int gameId)
    {
        ArrayList<InvocationService.ResultListener> list = _loading.remove(gameId);
        if (list != null) {
            for (InvocationService.ResultListener rList : list) {
                rList.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        }
    }

    /** Maps game id -> lobby. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected HashIntMap<ArrayList<InvocationService.ResultListener>> _loading =
        new HashIntMap<ArrayList<InvocationService.ResultListener>>();

    /** Maps game id -> proxy oids for remotely hosted game lobbies */
    protected IntIntMap _proxies = new IntIntMap();
}
