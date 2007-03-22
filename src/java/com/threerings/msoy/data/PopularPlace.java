//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.server.RoomManager;

/**
 * Represent a single place in the top list of populated places.
 */
public abstract class PopularPlace extends SimpleStreamableObject
{
    public static abstract class PopularScenePlace extends PopularPlace
    {
        @Override
        public String getName ()
        {
            return ((RoomManager) plMgr).getScene().getName();
        }
        
        @Override
        public int getId ()
        {
            return ((MsoySceneModel) ((RoomManager) plMgr).getScene().getSceneModel()).ownerId;
        }
        
        public int getSceneId ()
        {
            return ((RoomManager) plMgr).getScene().getId();
        }
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        public PopularMemberPlace (RoomManager manager)
        {
            plMgr = manager;
        }
    }

    public static class PopularGroupPlace extends PopularScenePlace
    {
        public PopularGroupPlace (RoomManager manager)
        {
            plMgr = manager;
        }
    }

    /** The manager for this place. */
    public PlaceManager plMgr;
    /** The number of people who are in this place. */
    public int population;
    
    /** Returns the name of this user- or group-owned scene, or game. */
    public abstract String getName ();
    
    /** Returns the id of this user, group or game. */
    public abstract int getId ();
}
