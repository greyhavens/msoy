//
// $Id$

package com.threerings.msoy.server;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.data.RoomConfig;

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
        // TODO: this is just a sample implementation
        return new SceneImpl(model, config);
    }

    // documentation inherited from interface SceneRegistry.ConfigFactory
    public PlaceConfig createPlaceConfig (SceneModel model)
    {
        // TODO: do the right thing
        return new RoomConfig();
    }
}
