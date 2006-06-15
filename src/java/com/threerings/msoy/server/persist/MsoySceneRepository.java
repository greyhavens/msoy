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

import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
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
        model.version = 14;
        model.name = "FakeScene" + sceneId;

        MsoyPortal portal = new MsoyPortal();
        portal.portalId = 1;
        portal.targetPortalId = 1;

        if (sceneId == 1) {
            //model.type = "image";
            model.width = 1600;
            model.background = new MediaData(8); // fancy room

            portal.loc = new MsoyLocation(1, 0, .8, 0);
            portal.targetSceneId = 2;
            portal.media = new MediaData(6); // rainbow door

            FurniData furn;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(7); // fans
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaData(9); // pinball
            furn.loc = new MsoyLocation(.2, 0, .2, 0);
            furn.action = "http://www.pinballnews.com/";
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaData(12); // curtain
            furn.loc = new MsoyLocation(.2, 0, 1, 0);
            model.addFurni(furn);

        } else {
            model.type = "image";
            model.background = new MediaData(11); // alley

            portal.loc = new MsoyLocation(0, .1, .53, 180);
            portal.targetSceneId = 1;
            portal.media = new MediaData(3); // alley door

            FurniData furn;
            furn = new FurniData();
            furn.id = 0;
            furn.media = new MediaData(10); // director's chair
            furn.loc = new MsoyLocation(.46, 0, .15, 0);
            model.addFurni(furn);
        }

        SpotSceneModel spotty = SpotSceneModel.getSceneModel(model);
        spotty.addPortal(portal);
        spotty.defaultEntranceId = 1;

        return model;
    }

    // documentation inherited from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
    {
        // TODO: real implementation
        return new UpdateList();
    }
}
