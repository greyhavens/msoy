//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.world.data.RoomConfig;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Scene and config factory for Msoy.
 */
public class MsoySceneFactory
    implements SceneFactory, SceneRegistry.ConfigFactory
{
    public MsoySceneFactory ()
    {
    }

    // documentation inherited from interface SceneFactory
    public Scene createScene (SceneModel model, PlaceConfig config)
    {
        return new MsoyScene((MsoySceneModel) model, config);
    }

    // documentation inherited from interface SceneRegistry.ConfigFactory
    public PlaceConfig createPlaceConfig (SceneModel smodel)
    {
        MsoySceneModel model = (MsoySceneModel) smodel;

        // TODO: do the right thing
        return new RoomConfig();
    }
}
