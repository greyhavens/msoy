//
// $Id$

package com.threerings.msoy.data;

import com.samskivert.util.StringUtil;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.ActionScript;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.server.SceneManager;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.server.MsoyGameManager;
import com.threerings.msoy.swiftly.server.ProjectRoomManager;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

import static com.threerings.msoy.Log.log;

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

        } else if (plMgr instanceof MsoyGameManager) {
            MsoyGameConfig config = (MsoyGameConfig) ((GameManager) plMgr).getGameConfig();
            return new PopularGamePlace(config.name, config.getGameId());

        } else if (plMgr instanceof ProjectRoomManager) {
            return null; // don't worry about people using Swiftly

        } else {
            log.warning("Unknown place manager type: " + StringUtil.shortClassName(plMgr));
            return null;
        }
    }

    public static abstract class PopularScenePlace extends PopularPlace
    {
        protected PopularScenePlace (MsoyScene scene)
        {
            this(((MsoySceneModel)scene.getSceneModel()).ownerId, scene.getName(), scene.getId());
        }

        protected PopularScenePlace (int placeId, String name, int sceneId)
        {
            _placeId = placeId;
            _name = name;
            _sceneId = sceneId;
        }

        @Override
        public int getId ()
        {
            return _placeId;
        }

        @Override
        public String getName()
        {
            return _name;
        }

        public int getSceneId ()
        {
            return _sceneId;
        }

        protected int _placeId;
        protected String _name;
        protected int _sceneId;
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

    public static class PopularGroupPlace extends PopularScenePlace
    {
        public PopularGroupPlace (MsoyScene scene)
        {
            super(scene);
        }
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        public PopularMemberPlace (MsoyScene scene)
        {
            super(scene);
        }

        public PopularMemberPlace (int memberId, int homeSceneId)
        {
            super(memberId, null, homeSceneId);
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
