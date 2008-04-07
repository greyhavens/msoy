package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;

public class IdleMetrics implements PlayerMetrics.Entry
{
    /** Seconds spent active. */
    public int timeActive;
    
    /** Seconds spent idle. */
    public int timeIdle;
    
    /** Initializes metrics for a given state (active or idle). */
    public void init (boolean isActive) 
    {
        _currentlyActive = isActive;
        _timestamp = System.currentTimeMillis();
    }
    
    /** Computes the delta, and adds to the appropriate field. */
    public void save (MemberObject player) 
    {
        if (_timestamp == 0L) {
            return; // not initialized
        }
        
        int seconds = (int) (System.currentTimeMillis() - _timestamp) / 1000;
        if (_currentlyActive) {
            timeActive += seconds;
        } else {
            timeIdle += seconds;
        }
        
        _timestamp = 0L;
    }
    
    /** Non-streamable current state. */
    protected transient boolean _currentlyActive;

    /** Non-streamable timestamp when we entered the current state. */
    protected transient long _timestamp;
}
