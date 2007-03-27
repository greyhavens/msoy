//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.IntTuple;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.whirled.server.SceneManager;

/**
 * Represent a single place in the top list of populated places.
 */
public abstract class PopularPlace extends SimpleStreamableObject
{
    public static class PopularPlaceOwner extends IntTuple
    {
        public enum OwnerType
        {
            MEMBER,
            GROUP,
            GAME;
        }

        public PopularPlaceOwner (OwnerType type, int ownerId)
        {
            super(type.ordinal(), ownerId);
        }
    }

    public static abstract class PopularScenePlace extends PopularPlace
    {
        /** The manager for this scene. */
        public SceneManager plMgr;

        @Override
        public String getName ()
        {
            return plMgr.getScene().getName();
        }
        
        @Override
        public int getId ()
        {
            return ((MsoySceneModel) plMgr.getScene().getSceneModel()).ownerId;
        }
        
        public int getSceneId ()
        {
            return plMgr.getScene().getId();
        }
    }

    public static class PopularGamePlace extends PopularPlace
    {
        public PopularGamePlace (String name, int id)
        {
            _name = name;
            _id = id;
        }

        @Override
        public String getName ()
        {
            return _name;
        }
        
        @Override
        public int getId ()
        {
            return _id;
        }

        protected String _name;
        protected int _id;
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        public PopularMemberPlace (SceneManager manager)
        {
            plMgr = manager;
        }
    }

    public static class PopularGroupPlace extends PopularScenePlace
    {
        public PopularGroupPlace (SceneManager manager)
        {
            plMgr = manager;
        }
    }

    /** The number of people who are in this place. */
    public int population;
    
    /** Returns the name of this user- or group-owned scene, or game. */
    public abstract String getName ();
    
    /** Returns the id of this user, group or game. */
    public abstract int getId ();
}
