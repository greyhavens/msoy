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
        model.version = 1;
        model.name = "FakeScene" + sceneId;
        SpotSceneModel spotty = SpotSceneModel.getSceneModel(model);

        MsoyPortal portal = new MsoyPortal();
        portal.portalId = 1;
        portal.targetPortalId = 1;

        if (sceneId == 1) {
            // crayon room
            model.type = "image";
            model.width = 1600;
            model.background = new MediaData(28); // crayon room

            portal.loc = new MsoyLocation(0, 0, .3, 0);
            portal.targetSceneId = 2;
            portal.targetPortalId = 1;
            portal.scaleX = portal.scaleY = (float) (1 / .865f);
            portal.media = new MediaData(34); // smile door

            MsoyPortal p2 = new MsoyPortal();
            p2.portalId = 2;
            p2.targetPortalId = 1;
            p2.targetSceneId = 6;
            p2.loc = new MsoyLocation(.8, 0, 1, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .55);
            p2.media = new MediaData(33); // aqua door
            spotty.addPortal(p2);

            p2 = new MsoyPortal();
            p2.portalId = 3;
            p2.targetPortalId = 1;
            p2.targetSceneId = 4;
            p2.loc = new MsoyLocation(1, 0, .3, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .865f);
            p2.media = new MediaData(32); // red door
            spotty.addPortal(p2);

            p2 = new MsoyPortal();
            p2.portalId = 4;
            p2.targetPortalId = 1;
            p2.targetSceneId = 5;
            p2.loc = new MsoyLocation(.75, 1, .5, 0);
            p2.media = new MediaData(31); // ladder
            spotty.addPortal(p2);

            FurniData furn;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(35); // candles
            furn.loc = new MsoyLocation(.45, 1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaData(29); // cactus
            furn.loc = new MsoyLocation(.6, -.1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaData(30); // fishbowl
            furn.loc = new MsoyLocation(.8, -.1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 4;
            furn.media = new MediaData(37); // frame
            furn.loc = new MsoyLocation(.42, .5, .999, 0);
            furn.scaleX = furn.scaleY = 1.9f;
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 5;
            furn.media = new MediaData(5); // joshua tree
            furn.loc = new MsoyLocation(.42, .22, 1, 0);
            furn.scaleX = 1.3f;
            furn.scaleY = 1.3f;
            model.addFurni(furn);

        } else if (sceneId == 2) {
            // alley
            model.type = "image";
            model.background = new MediaData(11); // alley
            model.music = new MediaData(13); // boll weevil

            portal.loc = new MsoyLocation(0, .1, .53, 180);
            portal.targetSceneId = 1;
            portal.media = new MediaData(3); // alley door

            FurniData furn;
            furn = new FurniData();
            furn.id = 0;
            furn.media = new MediaData(10); // director's chair
            furn.loc = new MsoyLocation(.46, 0, .15, 0);
            model.addFurni(furn);

        } else if (sceneId == 3) {
            // cliff
            model.type = "image";
            model.width = 800;
            model.background = new MediaData(23); // cliff background

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.targetSceneId = 6;
            portal.targetPortalId = 2;
            portal.media = new MediaData(19); // bendaydoor

            FurniData furn;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(24); // cliff foreground
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaData(36); // cedric
            furn.loc = new MsoyLocation(.15, 0, .35, 0);
            model.addFurni(furn);

        } else if (sceneId == 4) {
            // fans
            model.type = "image";
            model.width = 800;
            model.background = new MediaData(8); // fancy room

            portal.loc = new MsoyLocation(0, 0, .8, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 3;
            portal.scaleX = -1;
            portal.media = new MediaData(6); // rainbow door

            FurniData furn;

            furn = new FurniData();
            furn.id = 1;
            //furn.scaleX = -2f;
            //furn.scaleY = 1f;
            furn.media = new MediaData(7); // fans
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaData(9); // pinball
            furn.scaleX = -1;
            furn.loc = new MsoyLocation(.8, 0, .2, 0);
            //furn.action = "http://www.pinballnews.com/";
            furn.action = "http://www.t45ol.com/play/420/jungle-quest.html";
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaData(12); // curtain
            furn.loc = new MsoyLocation(.2, 0, 1, 0);
            model.addFurni(furn);

            /*
            furn = new FurniData();
            furn.id = 4;
            furn.media = new MediaData(5); // Joshua Tree
            furn.loc = new MsoyLocation(.5, 0, 1, 0);
            furn.scaleX = 2;
            furn.scaleY = 2;
            furn.action = "http://bogocorp.com/";
            model.addFurni(furn);
            */

            /*
            furn = new FurniData();
            furn.id = 5;
            furn.media = new MediaData(15); // 3d logic
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);
            */

        } else if (sceneId == 5) {
            // faucet
            model.type = "image";
            model.width = 1600;
            model.background = new MediaData(26); // faucet forest

            portal.loc = new MsoyLocation(.3125, .71, 0, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 4;
            portal.media = new MediaData(27); // pipe

        } else if (sceneId == 6) {
            // comic
            model.type = "image";
            model.width = 1600;
            model.background = new MediaData(16); // comic room

            portal.loc = new MsoyLocation(0, 0, .5, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 2;
            portal.media = new MediaData(18); // bendaydoor

            MsoyPortal p2 = new MsoyPortal();
            p2.portalId = 2;
            p2.targetSceneId = 3;
            p2.targetPortalId = 1;
            p2.loc = new MsoyLocation(.84, 0, .3, 0);
            p2.media = new MediaData(19); // bendaytransport
            spotty.addPortal(p2);

            /*
            p2 = new MsoyPortal();
            p2.portalId = 3;
            p2.targetPortalId = 1;
            p2.targetSceneId = 2;
            p2.loc = new MsoyLocation(.5, 0, .5, 0);
            p2.media = new MediaData(6);
            spotty.addPortal(p2);
            */

            FurniData furn;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(17); // comic foreground
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

        } else {
            System.err.println("Unknown scene: " + sceneId);
        }

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
