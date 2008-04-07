package com.threerings.msoy.world.data;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlayerMetrics;

public class RoomVisitMetrics implements PlayerMetrics.Entry
{
    /** Seconds spent in the player's own room. */
    public int timeInMyRoom;
    
    /** Seconds spent in a room belonging to my friend. */
    public int timeInFriendRooms;
    
    /** Seconds spent in an unknown person's room. */
    public int timeInStrangerRooms;
    
    /** Seconds spent in a whirled. */
    public int timeInWhirleds;
    
    /** Starts a counter for the given scene. */
    public void init (MsoySceneModel scene) 
    {
        _currentScene = scene;
        _currentSceneEntryTime = System.currentTimeMillis();
    }
    
    /** Finishes counting the current scene and updates the metrics. */
    public void save (MemberObject player) 
    {
        if (_currentScene == null) {
            return; // not initialized
        }
        
        // get time in seconds (rounded down)
        int seconds = (int) ((System.currentTimeMillis() - _currentSceneEntryTime) / 1000);

        if (_currentScene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
            
            if (_currentScene.ownerId == player.getMemberId()) {
                // my room
                this.timeInMyRoom += seconds;
                
            } else if (player.friends.containsKey(_currentScene.ownerId)) {
                // a friend's room
                this.timeInFriendRooms += seconds;
                
            } else {
                // stranger's room
                this.timeInStrangerRooms += seconds;
            }
            
        } else {
            
            this.timeInWhirleds += seconds;
        }
        
        // we're done with this scene
        _currentScene = null;
    }
    
    /** Non-streamable reference to the current scene. */
    protected transient MsoySceneModel _currentScene;
    
    /** Non-streamable timestamp when we entered the current scene. */
    protected transient long _currentSceneEntryTime;
}
