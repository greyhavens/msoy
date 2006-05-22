//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.UpdateList;

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
        SceneModel model = new SceneModel();
        model.sceneId = sceneId;
        model.version = 0;
        model.name = "FakeScene" + sceneId;
        return model;
    }

    // documentation inherited from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
    {
        // TODO: real implementation
        return new UpdateList();
    }
}
