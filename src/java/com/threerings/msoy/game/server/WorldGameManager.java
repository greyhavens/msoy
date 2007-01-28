//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.web.Item;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.server.persist.MemoryRecord;

import com.threerings.msoy.game.data.WorldGameConfig;
import com.threerings.msoy.game.data.WorldGameObject;

import static com.threerings.msoy.Log.*;

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
        
        WorldGameConfig wgconfig = (WorldGameConfig)_config;
        ((WorldGameObject)_plobj).config = wgconfig;
        
        final int prototypeId = wgconfig.persistentGameId;
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _mems = MsoyServer.memoryRepo.loadMemory(Item.GAME, prototypeId);
                    return true;
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to load memories [where=" + where() +
                            ", id=" + prototypeId + "].", pe);
                    return false;
                }
            };

            public void handleResult () {
                WorldGameObject worldGameObj = (WorldGameObject)_gameObj;
                worldGameObj.startTransaction();
                try {
                    for (MemoryRecord mrec : _mems) {
                        worldGameObj.addToMemories(mrec.toEntry());
                    }
                } finally {
                    worldGameObj.commitTransaction();
                }
            }

            protected Collection<MemoryRecord> _mems;
        });
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
    
    @Override // documentation inherited
    protected void didShutdown ()
    {
        super.didShutdown();
        
        // flush any modified memory records to the database
        final ArrayList<MemoryRecord> memrecs = new ArrayList<MemoryRecord>();
        for (MemoryEntry entry : ((WorldGameObject)_gameObj).memories) {
            if (entry.modified) {
                memrecs.add(new MemoryRecord(entry));
            }
        }
        if (memrecs.size() > 0) {
            MsoyServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        MsoyServer.memoryRepo.storeMemories(memrecs);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to update memories [where=" + where() +
                                ", memrecs=" + memrecs + "].", pe);
                    }
                    return false;
                }
            });
        }
    }

    /** The id of the world game. */
    protected int _gameId;
}
