//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationManager;

import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneCodes;
import com.threerings.whirled.server.SceneRegistry;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.SceneFactory;

import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;

/**
 * Handles some custom Whirled scene traversal business.
 */
public class MsoySceneRegistry extends SceneRegistry
    implements MsoySceneProvider
{
    public MsoySceneRegistry (InvocationManager invmgr, SceneRepository screp)
    {
        super(invmgr, screp, new MsoySceneFactory(), new MsoySceneFactory());

        // register our extra scene service
        invmgr.registerDispatcher(new MsoySceneDispatcher(this), SceneCodes.WHIRLED_GROUP);
    }

    // from interface MsoySceneProvider
    public void moveTo (ClientObject caller, int sceneId, int version, MsoyLocation destLoc,
                        SceneService.SceneMoveListener listener)
    {
        // TODO!
    }
}
