//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.ResultListener;

import com.threerings.presents.client.InvocationService;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;

import com.threerings.presents.dobj.ObjectDeathListener;
import com.threerings.presents.dobj.ObjectDestroyedEvent;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.presents.util.ResultAdapter;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.ItemIdent;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.item.util.ItemEnum;

import com.threerings.msoy.game.data.LobbyConfig;

/**
 * Manages the lobbies in use.
 */
public class LobbyRegistry
    implements LobbyProvider
{
    /**
     * Create the lobby registry.
     */
    public LobbyRegistry ()
    {
    }
    
    /**
     * Initialize the lobby registry.
     */
    public void init (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new LobbyDispatcher(this), true);
    }

    // from LobbyProvider
    public void identifyLobby (
        ClientObject caller, final int gameId,
        InvocationService.ResultListener listener)
        throws InvocationException
    {
        // see what we've got..
        Integer lobbyOid = _lobbies.get(gameId);
        if (lobbyOid != null) {
            // if we know the lobby oid, return it straight away
            listener.requestProcessed(lobbyOid);
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

        MsoyServer.itemMan.getItem(new ItemIdent(ItemEnum.GAME, gameId),
            new ResultListener<Item>() {
            public void requestCompleted (Item item)
            {
                try {
                    LobbyConfig config = new LobbyConfig();
                    config.game = (Game) item;
                    MsoyServer.plreg.createPlace(config);

                } catch (Exception e) {
                    requestFailed(e);
                }
            }

            public void requestFailed (Exception cause)
            {
                ArrayList<InvocationService.ResultListener> list =
                    _loading.remove(gameId);
                for (InvocationService.ResultListener listener : list) {
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            }
        });
    }

    /**
     * Called by LobbyManager instances after they're all ready to go.
     */
    public void lobbyStartup (int gameId, Integer lobbyOid)
    {
        // record the lobby oid for the game
        _lobbies.put(gameId, lobbyOid);

        // remove the list of listeners and notify each of them
        ArrayList<InvocationService.ResultListener> list =
            _loading.remove(gameId);
        for (InvocationService.ResultListener listener : list) {
            listener.requestProcessed(lobbyOid);
        }
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

    /** Maps game id -> lobby oid. */
    protected HashIntMap<Integer> _lobbies = new HashIntMap<Integer>();

    /** Maps game id -> listeners waiting for a lobby to load. */
    protected HashIntMap<ArrayList<InvocationService.ResultListener>> _loading =
        new HashIntMap<ArrayList<InvocationService.ResultListener>>();
}
