//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.client.persist.SceneRepository;

/**
 * Handles custom scene traversal and extra bits for Whirled.
 */
public class MsoySceneDirector extends SceneDirector
{
    public function MsoySceneDirector (
        ctx :WorldContext, locDir :LocationDirector, repo :SceneRepository)
    {
        super(ctx, locDir, repo, new MsoySceneFactory());
    }

    // from SceneDirector
    override public function moveTo (sceneId :int) :Boolean
    {
        if (sceneId == _sceneId) {
            // ignore this as we're just hearing back from our browser URL update mechanism
            return false;
        }
        return super.moveTo(sceneId);
    }

    // from SceneDirector
    override public function moveSucceeded (placeId :int, config :PlaceConfig) :void
    {
        super.moveSucceeded(placeId, config);
        // tell our controller to update the URL of the browser to reflect our new location
        (_ctx as WorldContext).getMsoyController().wentToScene(_sceneId);
    }
}
}
