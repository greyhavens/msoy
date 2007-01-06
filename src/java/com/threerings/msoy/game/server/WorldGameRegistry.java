//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;

import com.threerings.msoy.game.data.WorldGameConfig;

/**
 * Manages the lobbies in use.
 */
public class WorldGameRegistry
    implements WorldGameProvider
{
    /**
     * Create the in-world game registry.
     */
    public WorldGameRegistry ()
    {
    }
    
    /**
     * Initialize the lobby registry.
     */
    public void init (InvocationManager invmgr)
    {
        invmgr.registerDispatcher(new WorldGameDispatcher(this), true);
    }

    // from WorldGameProvider
    public void joinWorldGame (
        ClientObject caller, final int gameId, 
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // see what we've got..
        final MemberObject member = (MemberObject)caller;
        int gameOid = _games.get(gameId);
        if (gameOid > 0) {
            // if we know the game oid, set it straight away
            member.setInWorldGame(gameOid);
            return;
        }
        
        // create the result listener to set the game oid
        ResultListener<Integer> rlistener = new ResultListener<Integer>() {
            public void requestCompleted (Integer result) {
                if (member.isActive()) {
                    member.setInWorldGame(result);
                }
            }
            public void requestFailed (Exception cause) {
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        };
        
        ResultListenerList<Integer> list = _loading.get(gameId);
        if (list != null) {
            // if we're already resolving this game, add this listener
            // to the list of those interested in the outcome
            list.add(rlistener);
            return;
        }

        list = new ResultListenerList<Integer>();
        list.add(rlistener);
        _loading.put(gameId, list);

        // retrieve the game item
        MsoyServer.itemMan.getItem(new ItemIdent(Item.GAME, gameId),
            new ResultListener<Item>() {
            public void requestCompleted (Item item) {
                try {
                    WorldGameConfig config = new WorldGameConfig();
                    config.game = (Game)item;
                    config.configData = config.game.config;
                    config.persistentGameId = gameId;
                    
                    MsoyServer.plreg.createPlace(config);
                    
                } catch (Exception e) {
                    requestFailed(e);
                }
            }
            public void requestFailed (Exception cause) {
                _loading.remove(gameId).requestFailed(cause);
            }
        });
    }
    
    /**
     * Called by WorldGameManager instances after they're all ready to go.
     */
    public void gameStartup (int gameId, int gameOid)
    {
        // record the oid for the game
        _games.put(gameId, gameOid);

        // remove the list of listeners and notify each of them
        _loading.remove(gameId).requestCompleted(gameOid);
    }

    /**
     * Called by WorldGameManager instances when they start to shut down and
     * destroy their dobject.
     */
    public void gameShutdown (int gameId)
    {
        // destroy our record of that game
        _games.remove(gameId);
        _loading.remove(gameId); // just in case
    }

    /** Maps game id -> world game oid. */
    protected IntIntMap _games = new IntIntMap();
    
    /** Maps game id -> listeners waiting for a world game to load. */
    protected HashIntMap<ResultListenerList<Integer>> _loading =
        new HashIntMap<ResultListenerList<Integer>>();
}
