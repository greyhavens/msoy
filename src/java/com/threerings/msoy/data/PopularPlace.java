//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.ActionScript;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.server.SceneManager;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.server.MsoyGameManager;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.parlor.game.server.GameManager;

/**
 * Represent a single place in the top list of populated places.
 */
@ActionScript(omit=true)
public abstract class PopularPlace extends SimpleStreamableObject
{
    public static PopularPlace getPopularPlace (PlaceManager plMgr)
    {
        if (plMgr instanceof SceneManager) {
            SceneManager rMgr = ((SceneManager) plMgr);
            MsoyScene scene = (MsoyScene) rMgr.getScene();
            MsoySceneModel model = (MsoySceneModel) scene.getSceneModel();

            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                return new PopularGroupPlace(scene);
            }
            if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                return new PopularMemberPlace(scene);
            }
            throw new IllegalArgumentException("unknown owner type: " + model.ownerType);
        }
        if (plMgr instanceof MsoyGameManager) {
            MsoyGameConfig config = (MsoyGameConfig) ((GameManager) plMgr).getGameConfig();
            return new PopularGamePlace(config.name, config.getGameId());
        }
        throw new IllegalArgumentException("unknown place manager type: " + plMgr.getClass());
    }

    public static abstract class PopularScenePlace extends PopularPlace
    {
        protected PopularScenePlace (MsoyScene scene)
        {
            _scene = scene;
        }

        @Override
        public int getId ()
        {
            return ((MsoySceneModel )_scene.getSceneModel()).ownerId;
        }

        @Override
        public String getName()
        {
            return _scene.getName();
        }
        
        public int getSceneId ()
        {
            return _scene.getId();
        }
        
        protected Scene _scene;
    }

    public static class PopularGamePlace extends PopularPlace
    {
        public PopularGamePlace (String name, int id)
        {
            _name = name;
            _id = id;
        }

        @Override
        public int getId ()
        {
            return _id;
        }

        @Override
        public String getName()
        {
            return _name;
        }

        protected String _name;
        protected int _id;
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        public PopularMemberPlace (MsoyScene scene)
        {
            super(scene);
        }
    }

    public static class PopularGroupPlace extends PopularScenePlace
    {
        public PopularGroupPlace (MsoyScene scene)
        {
            super(scene);
        }
    }

    /** Returns the id of this user, group or game. */
    public abstract int getId ();

    /** Returns a descriptive name of this user, group or game. */
    public abstract String getName ();

    @Override
    public int hashCode()
    {
        return getId();
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj != null && getClass() == obj.getClass() &&
            getId() == ((PopularPlace) obj).getId();
    }
}
