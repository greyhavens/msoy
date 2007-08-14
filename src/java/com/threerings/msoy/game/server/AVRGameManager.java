//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.server.persist.MemoryRecord;

import com.threerings.msoy.game.data.AVRGameObject;

import static com.threerings.msoy.Log.*;

/**
 * Manages an in-world ez-game, or an AVRGame-
 * an Alternate Virtual Reality Game.
 */
public class AVRGameManager extends MsoyGameManager
{
    public AVRGameManager ()
    {
        addDelegate(_worldDelegate = new WorldGameManagerDelegate(this));
    }

    @Override // documentation inherited
    protected PlaceObject createPlaceObject ()
    {
        return new AVRGameObject();
    }

    @Override // documentation inherited
    protected void didStartup ()
    {
        super.didStartup();
        
        // TODO: this needs some re-thinking, there could be more than one instance of this game
        // running simultaneously and they will overwrite each other's memory.
        final int prototypeId = _gameconfig.getGameId();
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
                AVRGameObject avrGameObj = (AVRGameObject)_gameObj;
                avrGameObj.startTransaction();
                try {
                    for (MemoryRecord mrec : _mems) {
                        avrGameObj.addToMemories(mrec.toEntry());
                    }
                } finally {
                    avrGameObj.commitTransaction();
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
        for (MemoryEntry entry : ((AVRGameObject)_gameObj).memories) {
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

    /** Our world delegate. */
    protected WorldGameManagerDelegate _worldDelegate;
}
