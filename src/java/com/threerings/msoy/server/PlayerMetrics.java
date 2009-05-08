//
// $Id$

package com.threerings.msoy.server;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.MemberObject;

/** Collection of player metrics objects, with serialization accessors. */
public class PlayerMetrics
    implements Streamable
{
    /** Interface for all components of PlayerMetrics; enforces implementation of Streamable. */
    public interface Entry extends Streamable
    {
        /**
         * Called to update current metrics when leaving the room or switching servers.
         */
        void save (MemberObject player);
    };

    /** Tracks idle time. */
    public static class Idle implements Entry
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

        // from interface Entry
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

    /** Tracks time spent in rooms. */
    public static class RoomVisit implements Entry
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
        public void init (boolean memberScene, int ownerId)
        {
            _currentSceneIsMember = memberScene;
            _currentSceneOwnerId = ownerId;
            _currentSceneEntryTime = System.currentTimeMillis();
        }

        // from interface Entry
        public void save (MemberObject player)
        {
            if (_currentSceneEntryTime == 0L) {
                return; // not initialized
            }

            // get time in seconds (rounded down)
            int seconds = (int) ((System.currentTimeMillis() - _currentSceneEntryTime) / 1000);
            if (_currentSceneIsMember) {
                if (_currentSceneOwnerId == player.getMemberId()) {
                    // my room
                    this.timeInMyRoom += seconds;

                } else if (player.getLocal(MemberLocal.class).friendIds.contains(
                        _currentSceneOwnerId)) {
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
            _lastMeasuredSeconds = seconds;
            _currentSceneEntryTime = 0L;
        }

        /**
         * Returns the last known occupancy length. Useful when dealing with server switches,
         * when PlayerMetrics get forcibly serialized and sent over before the player object
         * leaves its scene.
         */
        public int getLastOccupancyLength ()
        {
            return _lastMeasuredSeconds;
        }

        /** Non-streamable data about the current scene. */
        protected transient boolean _currentSceneIsMember;

        /** Non-streamable data about the current scene. */
        protected transient int _currentSceneOwnerId;

        /** Non-streamable timestamp when we entered the current scene. */
        protected transient long _currentSceneEntryTime;

        /** Non-streamable number that stores the last known scene occupancy length. */
        protected transient int _lastMeasuredSeconds;
    }

    /** Reference to the room visit metrics. */
    public RoomVisit room = new RoomVisit();

    /** Keeps track of the player's active/idle times across different server sessions. */
    public Idle idle = new Idle();

    /** Flush all metrics before a server switch. Ignore double-saves. */
    public void save (MemberObject player)
    {
        room.save(player);
        idle.save(player);
    }
}
