//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.ResultListenerList;

import com.threerings.util.Name;

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

import com.threerings.msoy.world.data.MemoryEntry;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.WorldGameObject;

import static com.threerings.msoy.Log.*;

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
            // if we know the game oid, join immediately
            joinWorldGame(member, gameOid);
            return;
        }
        
        // create the result listener to join the game
        ResultListener<Integer> rlistener = new ResultListener<Integer>() {
            public void requestCompleted (Integer result) {
                if (!member.isActive()) {
                    return; // he bailed
                }
                try {
                    joinWorldGame(member, result);
                } catch (InvocationException e) {
                    requestFailed(e);
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
                    config.configData = config.game.gameMedia.getMediaPath();
                    config.persistentGameId = config.game.getPrototypeId();
                    config.players = new Name[config.game.maxPlayers];
                    
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
    
    // from WorldGameProvider
    public void leaveWorldGame (ClientObject caller, InvocationService.InvocationListener arg1)
        throws InvocationException
    {
        leaveWorldGame((MemberObject)caller);
    }
    
    /**
     * Removes the user from the world game he currently occupies (if any).
     */
    public void leaveWorldGame (MemberObject member)
        throws InvocationException
    {
        // nothing to do if they aren't in a game
        if (member.inWorldGame <= 0) {
            return;
        }
        
        // get the game object
        WorldGameObject wgobj = getWorldGameObject(member, member.inWorldGame);
        member.setInWorldGame(0);
        
        // remove them from the occupant list
        int memberOid = member.getOid();
        wgobj.startTransaction();
        try {
            wgobj.removeFromOccupantInfo(memberOid);
            wgobj.removeFromOccupants(memberOid);
        } finally {
            wgobj.commitTransaction();
        }
    }

    // from WorldGameProvider
    public void updateMemory (ClientObject caller, MemoryEntry entry)
    {
        // get their game object
        MemberObject member = (MemberObject)caller;
        WorldGameObject gameObj = (WorldGameObject)MsoyServer.omgr.getObject(member.inWorldGame);
        if (gameObj == null) {
            log.warning("Received memory update request from user not in world game [who=" +
                member.who() + "].");
            return;
        }
        
        // make sure the entry refers to the game
        entry.item = new ItemIdent(Item.GAME, gameObj.config.game.getPrototypeId());
        
        // TODO: verify that the memory does not exceed legal size
        
        // mark it as modified and update the game object; we'll save it when we unload the game
        entry.modified = true;
        if (gameObj.memories.contains(entry)) {
            gameObj.updateMemories(entry);
        } else {
            gameObj.addToMemories(entry);
        }
    }
    
    /**
     * Called by WorldGameManager instances after they're all ready to go.
     */
    public void gameStartup (WorldGameManager manager)
    {
        // record the oid and manager for the game
        int gameId = manager.getGameId(), gameOid = manager.getPlaceObject().getOid();
        _games.put(gameId, gameOid);
        _managers.put(gameOid, manager);
        
        // remove the list of listeners and notify each of them
        _loading.remove(gameId).requestCompleted(gameOid);
    }

    /**
     * Called by WorldGameManager instances when they start to shut down and
     * destroy their dobject.
     */
    public void gameShutdown (WorldGameManager manager)
    {
        // destroy our record of that game
        int gameId = manager.getGameId(), gameOid = manager.getPlaceObject().getOid();
        _games.remove(gameId);
        _managers.remove(gameOid);
        _loading.remove(gameId); // just in case
    }

    /**
     * Adds the user to a world game.
     */
    protected void joinWorldGame (MemberObject member, int gameOid)
        throws InvocationException
    {
        // make sure they're not already in the game
        if (member.inWorldGame == gameOid) {
            return;
        }
        
        // make sure the game object exists
        WorldGameObject wgobj = getWorldGameObject(member, gameOid);
        
        // leave the current game, if any
        leaveWorldGame(member);
        
        // add to the occupant list
        int memberOid = member.getOid();
        WorldGameManager wgmgr = _managers.get(gameOid);
        wgobj.startTransaction();
        try {
            wgmgr.buildOccupantInfo(member);
            wgobj.addToOccupants(memberOid);
        } finally {
            wgobj.commitTransaction();
        }
        
        // set their game field
        member.setInWorldGame(gameOid);
    }
    
    /**
     * Retrieves the world game object, throwing an exception if it does not exist.
     */
    protected WorldGameObject getWorldGameObject (MemberObject member, int gameOid)
        throws InvocationException
    {
        WorldGameObject wgobj = (WorldGameObject)MsoyServer.omgr.getObject(gameOid);
        if (wgobj == null) {
            log.warning("Missing world game object [who=" + member.who() +
                ", oid=" + gameOid + "].");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }
        return wgobj;
    }
    
    /** Maps game id -> world game oid. */
    protected IntIntMap _games = new IntIntMap();
    
    /** Maps game oids -> game managers. */
    protected HashIntMap<WorldGameManager> _managers = new HashIntMap<WorldGameManager>();
    
    /** Maps game id -> listeners waiting for a world game to load. */
    protected HashIntMap<ResultListenerList<Integer>> _loading =
        new HashIntMap<ResultListenerList<Integer>>();
}
