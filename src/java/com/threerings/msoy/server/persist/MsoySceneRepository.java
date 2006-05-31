//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneModel;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoySceneModel;

/**
 * Provides scene storage services for the msoy server.
 */
public class MsoySceneRepository
    implements SceneRepository
{
    public MsoySceneRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        // TODO
    }

    // documentation inherited from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
    {
        // TODO
    }

    // documentation inherited from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
    {
        // TODO: real implementation
        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = sceneId;
        model.version = 1;
        model.name = "FakeScene" + sceneId;

        Portal portal = new Portal();
        portal.portalId = 1;
        portal.targetPortalId = 1;

        if (sceneId == 1) {
            portal.loc = new MsoyLocation(400, 200, 0, (short)0);
            portal.targetSceneId = 2;
        } else {
            portal.loc = new MsoyLocation(20, 200, 0, (short)180);
            portal.targetSceneId = 1;
        }

        SpotSceneModel spotty = SpotSceneModel.getSceneModel(model);
        spotty.addPortal(portal);

        return model;
    }

    // documentation inherited from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
    {
        // TODO: real implementation
        return new UpdateList();
    }
}
