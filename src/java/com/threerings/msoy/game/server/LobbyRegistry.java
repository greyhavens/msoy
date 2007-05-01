//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Iterator;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.game.xml.MsoyGameParser;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

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
                               InvocationService.ResultListener listener)
        throws InvocationException
    {
        LobbyManager mgr = _lobbies.get(gameId);
        // see what we've got..
        if (mgr != null) {
            // if we know the lobby, return its oid straight away
            listener.requestProcessed(mgr.getLobbyObject().getOid());
            return;
        }

        ArrayList<InvocationService.ResultListener> list = _loading.get(gameId);
        if (list != null) {
            // if we're already resolving this lobby, add this listener
            // to the list of those interested in the outcome
            list.add(listener);
            return;
        }

        list = new ArrayList<InvocationService.ResultListener>();
        list.add(listener);
        _loading.put(gameId, list);

        MsoyServer.itemMan.getItem(new ItemIdent(Item.GAME, gameId), new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                try {
                    LobbyManager lmgr = new LobbyManager(
                        (Game)item, new MsoyGameParser().parseGame((Game)item));
                    // record the lobby oid for the game
                    _lobbies.put(gameId, lmgr);

                    // remove the list of listeners and notify each of them
                    for (InvocationService.ResultListener rList : _loading.remove(gameId)) {
                        rList.requestProcessed(lmgr.getLobbyObject().getOid());
                    }

                } catch (Exception e) {
                    requestFailed(e);
                }
            }

            public void requestFailed (Exception cause)
            {
                for (InvocationService.ResultListener rList : _loading.remove(gameId)) {
                    rList.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
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
        _loading.remove(gameId); // just in case
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

    /** Maps game id -> lobby oid. */
    protected IntMap<LobbyManager> _lobbies = new HashIntMap<LobbyManager>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected HashIntMap<ArrayList<InvocationService.ResultListener>> _loading =
        new HashIntMap<ArrayList<InvocationService.ResultListener>>();
}
