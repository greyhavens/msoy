//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.WorldGameObject;

/**
 * Manages an in-world game.
 */
public class WorldGameManager extends EZGameManager
{
    @Override // documentation inherited
    public void startup (PlaceObject plobj)
    {
        super.startup(plobj);
        MsoyServer.worldGameReg.gameStartup(this);
    }

    @Override // documentation inherited
    public void shutdown ()
    {
        MsoyServer.worldGameReg.gameShutdown(this);
        super.shutdown();
    }

    /**
     * Returns the persistent game id.
     */
    public int getGameId ()
    {
        return _gameId;
    }
    
    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new WorldGameObject();
    }

    @Override // documentation inherited
    protected void didInit ()
    {
        super.didInit();

        // remember our game id
        _gameId = ((WorldGameConfig)_config).persistentGameId;
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();
        ((WorldGameObject)_plobj).config = (WorldGameConfig)_config;
    }
    
    @Override // documentation inherited
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        if (getPlayerCount() < getPlayerSlots()) {
            // automatically add as a player
            MemberObject member = (MemberObject)MsoyServer.omgr.getObject(bodyOid);
            addPlayer(member.memberName);
        }
    }
    
    @Override // documentation inherited
    protected void bodyLeft (int bodyOid)
    {
        MemberObject member = (MemberObject)MsoyServer.omgr.getObject(bodyOid);
        if (getPlayerIndex(member.memberName) != -1) {
            // clear the slot to let another take it
            removePlayer(member.memberName);
        }
        super.bodyLeft(bodyOid);
    }
    
    /** The id of the world game. */
    protected int _gameId;
}
