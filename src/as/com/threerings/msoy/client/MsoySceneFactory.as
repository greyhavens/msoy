package com.threerings.msoy.client {

import com.threerings.whirled.util.SceneFactory;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneModel;

/**
 * The client-side scene factory in use by the msoy client.
 */
public class MsoySceneFactory
    implements SceneFactory
{
    // documentation inherited from interface SceneFactory
    public function createScene (model :SceneModel, config :PlaceConfig) :Scene
    {
        // TODO: something real
        return new SceneImpl(model, config);
    }
}
}
