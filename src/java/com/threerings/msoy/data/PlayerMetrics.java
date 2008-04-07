package com.threerings.msoy.data;

import java.io.IOException;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.world.data.IdleMetrics;
import com.threerings.msoy.world.data.RoomVisitMetrics;

/** Collection of player metrics objects, with serialization accessors. */
public class PlayerMetrics
    implements Streamable
{
    /** Reference to the room visit metrics. */
    public RoomVisitMetrics room = new RoomVisitMetrics();
    
    /** Keeps track of the player's active/idle times across different server sessions. */
    public IdleMetrics idle = new IdleMetrics();
    
    /** Flush all metrics before a server switch. */
    public void save (MemberObject player) 
    {
        room.save(player);
        idle.save(player);
    }
    
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.writeObject(room);
        out.writeObject(idle);
    }
    
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        try {
            room = (RoomVisitMetrics) in.readObject();
            idle = (IdleMetrics) in.readObject();

        } catch (ClassCastException cce) {
            throw new IOException("Malformed PlayerMetrics object: " + cce);
        }
    }
    
    /** Interface for all components of PlayerMetrics; enforces implementation of Streamable. */ 
    public interface Entry extends Streamable 
    { 
        /** Called to update current metrics when switching servers. */
        public void save (MemberObject player);
    };
}
