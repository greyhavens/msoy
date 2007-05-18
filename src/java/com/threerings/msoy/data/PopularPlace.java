//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.server.PlaceManager;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.util.ActionScript;

import com.threerings.whirled.server.SceneManager;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.server.MsoyGameManager;
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
            MsoySceneModel model = (MsoySceneModel) rMgr.getScene().getSceneModel();

            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
                return new PopularGroupPlace(model.ownerId);
            }
            if (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                return new PopularMemberPlace(model.ownerId);
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
        protected PopularScenePlace (int ownerId)
        {
            _ownerId = ownerId;
        }

        @Override
        public int getId ()
        {
            return _ownerId;
        }

        protected int _ownerId;

        public int getSceneId()
        {
            // TODO Auto-generated method stub
            return 1;
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
        public int getId ()
        {
            return _id;
        }

        protected String _name;
        protected int _id;
    }

    public static class PopularMemberPlace extends PopularScenePlace
    {
        public PopularMemberPlace (int ownerId)
        {
            super(ownerId);
        }
    }

    public static class PopularGroupPlace extends PopularScenePlace
    {
        public PopularGroupPlace (int ownerId)
        {
            super(ownerId);
        }
    }

    /** Returns the id of this user, group or game. */
    public abstract int getId ();

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
