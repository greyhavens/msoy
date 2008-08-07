//
// $Id$

package com.threerings.msoy.room.server;

import com.google.inject.Singleton;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomConfig;

/**
 * Scene and config factory for Msoy.
 */
@Singleton
public class MsoySceneFactory
    implements SceneFactory, SceneRegistry.ConfigFactory
{
    // from interface SceneFactory
    public Scene createScene (SceneModel model, PlaceConfig config)
    {
        return new MsoyScene((MsoySceneModel) model, config);
    }

    // from interface SceneRegistry.ConfigFactory
    public PlaceConfig createPlaceConfig (SceneModel smodel)
    {
        // TODO: do the right thing
        return new RoomConfig();
    }
}
