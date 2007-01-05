//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.IntIntMap;

import com.threerings.presents.data.ClientObject;

import com.threerings.presents.client.InvocationService;

import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;

import com.threerings.msoy.data.MemberObject;

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
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        // see what we've got..
        MemberObject member = (MemberObject)caller;
        int gameOid = _games.get(gameId);
        if (gameOid > 0) {
            // if we know the game oid, set it straight away
            member.setInWorldGame(gameOid);
            return;
        }
        
        
    }
    
    /** Maps game id -> world game oid. */
    protected IntIntMap _games = new IntIntMap();
}
