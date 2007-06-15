//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.sql.Connection;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.And;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.StringUtil;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.server.persist.SceneUpdateMarshaller;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

import static com.threerings.msoy.Log.log;

/**
 * Provides scene storage services for the msoy server.
 */
public class MsoySceneRepository extends DepotRepository
    implements SceneRepository
{
    public MsoySceneRepository (ConnectionProvider provider)
    {
        super(provider);

        // TEMP: hand migrate all the old-school business to the new hotness
        try {
            Connection conn = provider.getConnection("scenedb", false);
            try {
                migrateTable(conn, "SCENES", "SceneRecord", new String[] {
                    "SCENE_ID", "sceneId integer not null auto_increment",
                    "OWNER_TYPE", "ownerType tinyint not null",
                    "OWNER_ID", "ownerId integer not null",
                    "VERSION", "version integer not null",
                    "NAME", "name varchar(255) not null",
                    "DECOR_ID", "decorId int not null",
                    "AUDIO_ID", "audioId int not null",
                    "AUDIO_MEDIA_HASH", "audioMediaHash tinyblob not null",
                    "AUDIO_MEDIA_TYPE", "audioMediaType tinyint not null",
                    "AUDIO_VOLUME", "audioVolume float not null",
                    "ENTRANCE_X", "entranceX float not null",
                    "ENTRANCE_Y", "entranceY float not null",
                    "ENTRANCE_Z", "entranceZ float not null",
                });

                migrateTable(conn, "FURNI", "SceneFurniRecord", new String[] {
                    "SCENE_ID", "sceneId integer not null",
                    "FURNI_ID", "furniId smallint not null",
                    "ITEM_TYPE", "itemType tinyint not null",
                    "ITEM_ID", "itemId integer not null",
                    "MEDIA_HASH", "mediaHash tinyblob not null",
                    "MEDIA_TYPE", "mediaType tinyint not null",
                    "X", "x float not null",
                    "Y", "y float not null",
                    "Z", "z float not null",
                    "LAYOUT_INFO", "layoutInfo tinyint not null",
                    "SCALE_X", "scaleX float not null",
                    "SCALE_Y", "scaleY float not null",
                    "ACTION_TYPE", "actionType tinyint not null",
                    "ACTION_DATA", "actionData varchar(255)",
                });

                migrateTable(conn, "SCENE_UPDATES", "SceneUpdateRecord", new String[] {
                    "SCENE_ID", "sceneId integer not null",
                    "SCENE_VERSION", "sceneVersion integer not null",
                    "UPDATE_TYPE", "updateType integer not null",
                    "DATA", "data blob not null",
                });

            } finally {
                provider.releaseConnection("scenedb", false, conn);
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Pants, scene database migration failed.", e);
        }
        // END TEMP

        // TODO: check and populate
//         // populate some starting scenes
//         for (int sceneId = 1; sceneId < 8; sceneId++) {
//             if (sceneId !=
//                     insertScene(conn, liaison, createSampleScene(sceneId))) {
//                 throw new RuntimeException("It's not quite right!");
//             }
//         }
    }

    // TEMP
    protected void migrateTable (Connection conn, String table, String ntable, String[] migrations)
        throws Exception
    {
        if (!JDBCUtil.tableExists(conn, table)) {
            return;
        }
        log.info("Migrating " + table + " to " + ntable + ". Cross your fingers!");
        for (int ii = 0; ii < migrations.length; ii += 2) {
            if (JDBCUtil.tableContainsColumn(conn, table, migrations[ii])) {
                JDBCUtil.changeColumn(conn, table, migrations[ii], migrations[ii+1]);
            }
        }
        Statement stmt = conn.createStatement();
        try {
            stmt.executeUpdate("rename table " + table + " to " + ntable);
            stmt.executeUpdate("insert into DepotSchemaVersion values('" + ntable + "', 1)");
        } finally {
            JDBCUtil.close(stmt);
        }
    }
    // END TEMP

    /**
     * Provides any additional initialization that needs to happen after runtime configuration had
     * been loaded, and other services initialized.
     */
    public void finishInit (DecorRepository decorRepo)
    {
        // keep a pointer to the decor repository
        _decorRepo = decorRepo;
    }

    /**
     * Retrieve a list of all the scenes that the user directly owns.
     */
    public ArrayList<SceneBookmarkEntry> getOwnedScenes (byte ownerType, int memberId)
        throws PersistenceException
    {
        ArrayList<SceneBookmarkEntry> marks = new ArrayList<SceneBookmarkEntry>();
        Where where = new Where(new And(new Equals(SceneRecord.OWNER_TYPE, ownerType),
                                        new Equals(SceneRecord.OWNER_ID, memberId)));
        // TODO: use a @Computed record?
        for (SceneRecord scene : findAll(SceneRecord.class, where)) {
            marks.add(new SceneBookmarkEntry(scene.sceneId, scene.name, 0L));
        }
        return marks;
    }

    /**
     * Given a list of scene ids, return a map containing the current names, indexed by scene id.
     */
    public HashIntMap<String> identifyScenes (int[] scenes)
        throws PersistenceException
    {
        HashIntMap<String> names = new HashIntMap<String>();
        // TODO: use a @Computed record?
        Where where = new Where(new In(SceneRecord.SCENE_ID, IntListUtil.box(scenes)));
        for (SceneRecord scene : findAll(SceneRecord.class, where)) {
            names.put(scene.sceneId, scene.name);
        }
        return names;
    }

    // documentation inherited from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
        throws PersistenceException
    {
        // ensure that the update has been applied
        if (model.version != update.getSceneVersion() + 1) {
            log.warning("Refusing to apply update " + update +
                        ", wrong version " + model.version + ".");
            return;
        }

        MsoySceneModel mmodel = (MsoySceneModel) model;
        if (update instanceof ModifyFurniUpdate) {
            applyFurniUpdate(mmodel, (ModifyFurniUpdate) update);

        } else if (update instanceof SceneAttrsUpdate) {
            applySceneAttrsUpdate(mmodel, (SceneAttrsUpdate) update);

        } else {
            log.warning("Requested to apply unknown update to scene repo [update=" + update + "].");
        }

        // finally, update the scene version (which will already be the new version because the
        // update has been applied)
        updateVersion(model.sceneId, model.version);
        log.info("Updated version of " + model.sceneId + " to " + model.version + ".");

        // record the update itself
        insertSceneUpdate(update);
    }

    /**
     * Updates the version of the specified scene in the database.
     */
    public void updateVersion (int sceneId, int version)
        throws PersistenceException
    {
        updatePartial(SceneRecord.class, sceneId, SceneRecord.VERSION, version);
    }

    // documentation inherited from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        UpdateList list = new UpdateList();
        for (SceneUpdateRecord record : findAll(
                 SceneUpdateRecord.class, new Where(SceneUpdateRecord.SCENE_ID, sceneId),
                 OrderBy.ascending(SceneUpdateRecord.SCENE_VERSION))) {
            list.addUpdate(_updateMarshaller.decodeUpdate(
                               sceneId, record.sceneVersion, record.updateType, record.data));
        }
        return list;
    }

    // documentation inherited from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        if (scene == null) {
            throw new NoSuchSceneException(sceneId);
        }
        MsoySceneModel model = scene.toSceneModel();

        // load up all of our furni data
        ArrayList<FurniData> flist = new ArrayList<FurniData>();
        for (SceneFurniRecord furni : findAll(SceneFurniRecord.class,
                                              new Where(SceneFurniRecord.SCENE_ID, sceneId))) {
            flist.add(furni.toFurniData());
        }
        model.furnis = flist.toArray(new FurniData[flist.size()]);

        // load up our room decor
        if (model.decor.itemId != 0) {
            DecorRecord record = _decorRepo.loadItem(model.decor.itemId);
            if (record != null) {
                model.decor = (Decor) record.toItem();
            }
        }
        if (model.decor == null) {
            // the scene specified no or an invalid decor, just load up the default
            model.decor = MsoySceneModel.defaultMsoySceneModelDecor();
        }

        return model;
    }

    /**
     * Apply a furniture changing update.
     */
    protected void applyFurniUpdate (MsoySceneModel mmodel, ModifyFurniUpdate update)
        throws PersistenceException
    {
        if (update.furniRemoved != null) {
            for (FurniData data : update.furniRemoved) {
                delete(SceneFurniRecord.class,
                       SceneFurniRecord.getKey(mmodel.sceneId, data.id));
            }
        }
        if (update.furniAdded != null) {
            for (FurniData data : update.furniAdded) {
                insert(new SceneFurniRecord(mmodel.sceneId, data));
            }
        }
    }

    /**
     * Apply an update that changes the basic scene attributes.
     */
    protected void applySceneAttrsUpdate (MsoySceneModel mmodel, SceneAttrsUpdate update)
        throws PersistenceException
    {
        updatePartial(
            SceneRecord.class, mmodel.sceneId,
            SceneRecord.NAME, update.name,
            SceneRecord.DECOR_ID, update.decor.itemId,
            SceneRecord.AUDIO_ID, update.audioData.itemId,
            SceneRecord.AUDIO_MEDIA_HASH, SceneUtil.flattenMediaDesc(update.audioData.media),
            SceneRecord.AUDIO_MEDIA_TYPE, update.audioData.media.mimeType,
            SceneRecord.AUDIO_VOLUME, update.audioData.volume,
            SceneRecord.ENTRANCE_X, update.entrance.x,
            SceneRecord.ENTRANCE_Y, update.entrance.y,
            SceneRecord.ENTRANCE_Z, update.entrance.z);
    }

    /**
     * Creates a new blank room pointing to the common area.
     *
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (byte ownerType, int ownerId, String roomName)
        throws PersistenceException
    {
        return createBlankRoom(ownerType, ownerId, roomName, null);
    }

    /**
     * Create a new blank room for the specified member.
     *
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (byte ownerType, int ownerId, String roomName, String portalAction)
        throws PersistenceException
    {
        // TODO: we'll clone a starter room
        final MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.ownerType = ownerType;
        model.ownerId = ownerId;
        model.version = 1;
        model.name = roomName;

        FurniData f = new FurniData();
        f.id = 1;
        f.media = new MediaDesc("e8b660ec5aa0aa30dab46b267daf3b80996269e7.swf");
        f.loc = new MsoyLocation(1, 0, 0.5, 0);
        f.scaleX = 1.4f;
        f.actionType = FurniData.ACTION_PORTAL;
        f.actionData = portalAction == null ? "1:A Common Room" : portalAction;
        model.addFurni(f);

        return insertScene(model);
    }

    /**
     * Insert a new scene, with furni and all, into the database and return the newly assigned
     * sceneId.
     */
    protected int insertScene (MsoySceneModel model)
        throws PersistenceException
    {
        int sceneId = insert(new SceneRecord(model));
        for (FurniData data : model.furnis) {
            insert(new SceneFurniRecord(sceneId, data));
        }
        return sceneId;
    }

    /**
     * Records a scene update to the update table.
     */
    protected void insertSceneUpdate (final SceneUpdate update)
        throws PersistenceException
    {
        // first insert the current update
        SceneUpdateRecord record = new SceneUpdateRecord();
        record.sceneId = update.getSceneId();
        record.sceneVersion = update.getSceneVersion();
        record.updateType = _updateMarshaller.getUpdateType(update);
        if (record.updateType == -1) {
            String errmsg = "Can't insert update of unknown type [update=" + update +
                ", updateClass=" + update.getClass() + "]";
            throw new PersistenceException(errmsg);
        }
        record.data = _updateMarshaller.persistUpdate(update);
        insert(record);

        // then prune the older updates
        int minVersion = update.getSceneVersion() - MAX_UPDATES_PER_SCENE;
        deleteAll(SceneUpdateRecord.class,
                  new Where(new LessThan(SceneUpdateRecord.SCENE_VERSION, minVersion)),
                  null); // TODO: cache invalidator
    }

    /**
     * Create a sample scene.
     */
    protected MsoySceneModel createSampleScene (int sceneId)
    {
        // TODO: now that decor comes from a separate table, we need to rethink how we're seeding a
        // new server's room set. the decor table should probably be filled in separately.  let's
        // comment out the old settings for reference, and just leave each room with the default
        // empty decor.

        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = sceneId;
        model.version = 1;
        model.name = "SampleScene" + sceneId;

        FurniData portal = new FurniData();
        portal.id = 51;
        portal.actionType = FurniData.ACTION_PORTAL;
        FurniData p2;
        FurniData furn;

        //DecorData decorData = model.decorData;
        //decorData.type = Decor.IMAGE_OVERLAY;

        if (sceneId == 1) {
            // crayon room
            //decorData.width = 1600;
            //decorData.media = new MediaDesc( // crayon room
            //    "b3084c929b49cce36a6708fb8f47a45c59e1d400.png");

            portal.loc = new MsoyLocation(0, 0, .3, 0);
            portal.actionData = "2:51";
            portal.scaleX = portal.scaleY = (float) (1 / .865f);
            portal.media = new MediaDesc( // smile door
                "7511842ed6201ccddb7b072fc4886c21d395376f.png");

            p2 = new FurniData();
            p2.id = 52;
            p2.actionType = FurniData.ACTION_PORTAL;
            p2.actionData = "6:51";
            p2.loc = new MsoyLocation(.8, 0, 1, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .55);
            p2.media = new MediaDesc( // aqua door
                "7fbc0922c7f36e1ce14648466b42c093185b6c1b.png");
            model.addFurni(p2);

            p2 = new FurniData();
            p2.id = 53;
            p2.actionType = FurniData.ACTION_PORTAL;
            p2.actionData = "4:51";
            p2.loc = new MsoyLocation(1, 0, .3, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .865f);
            p2.media = new MediaDesc( // red door
                "2c75bf2cc3e9a35aafb2e96bd13dd05060d132a0.png");
            model.addFurni(p2);

            p2 = new FurniData();
            p2.id = 54;
            p2.actionType = FurniData.ACTION_PORTAL;
            p2.actionData = "5:51";
            p2.loc = new MsoyLocation(.75, 1, .5, 0);
            p2.media = new MediaDesc( // ladder
                "92d6dbe0b44a1c070b638f93d9fff8a907ba1cda.png");
            model.addFurni(p2);

            p2 = new FurniData();
            p2.id = 55;
            p2.actionType = FurniData.ACTION_PORTAL;
            p2.actionData = "7:51";
            p2.loc = new MsoyLocation(.95, 0, 1, 0);
            p2.scaleX = p2.scaleY = .3f;
            p2.media = new MediaDesc(
                "7fbc0922c7f36e1ce14648466b42c093185b6c1b.png");
            model.addFurni(p2);

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // candles
                "a8997a46f7bc6a69fce9a183d065044e2f6c890f.png");
            furn.loc = new MsoyLocation(.45, 1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaDesc( // cactus
                "3e8fcc0796d47c5901fe73866c2fdbc1d294cdc1.png");
            furn.loc = new MsoyLocation(.6, -.1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaDesc( // fishbowl
                "abd5e0df720cad59ae1603912f1169eaebe27bcd.png");
            furn.loc = new MsoyLocation(.8, -.1, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 4;
            furn.media = new MediaDesc( // frame
                "347bb61120ead9b24a4f5c82ecf38b3736b96d9f.png");
            furn.loc = new MsoyLocation(.42, .5, .999, 0);
            furn.scaleX = furn.scaleY = 1.9f;
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 5;
            furn.media = new MediaDesc( // mario galaxy
                "97080f068568a9c403e5d7fb70ec625754ed807e.flv");
            furn.loc = new MsoyLocation(.42, .22, 1, 0);
            furn.scaleX = 1.3f;
            furn.scaleY = 1.3f;
            model.addFurni(furn);

        } else if (sceneId == 2) {
            // alley
            //decorData.media = new MediaDesc( // alley
            //    "13fd51be845d51b1571424cf459ce4fd78472ec2.png");

            portal.loc = new MsoyLocation(0, .1, .53, 180);
            portal.actionData = "1:-1";
            portal.media = new MediaDesc( // alley door
                "fcd7eedea57bc869e3ab576661ac8516dcb561a8.swf");

            furn = new FurniData();
            furn.id = 0;
            furn.media = new MediaDesc( // director's chair
                "89bf404f515961fd3d578d369e3f93fd3faef1b8.swf");
            furn.loc = new MsoyLocation(.46, 0, .15, 0);
            model.addFurni(furn);

        } else if (sceneId == 3) {
            // cliff
            //decorData.width = 800;
            //decorData.media = new MediaDesc( // cliff background
            //    "974259e79d58c34beffe67fb781832183309fe57.swf");

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.actionData = "6:52";
            portal.media = new MediaDesc( // bendaytransport
                "636dfebde41fb73d0bf6ee3cca0387ac5532b9ce.swf");

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // cliff foreground
                "fc41437dd475fdb1b8a8dd40f4d487c715028059.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaDesc( // cedric
                "af1aae0c4750b247a8f13a9f76bf5361d9c214c8.swf");
            furn.loc = new MsoyLocation(.15, 0, .35, 0);
            model.addFurni(furn);

        } else if (sceneId == 4) {
            // fans
            //decorData.width = 800;
            //decorData.media = new MediaDesc( // fancy room
            //    "95101b275b607c5c02a8a411a09082ef2e9b98a7.png");

            portal.loc = new MsoyLocation(0, 0, .8, 0);
            portal.actionData = "1:53";
            portal.scaleX = -1;
            portal.media = new MediaDesc( // rainbow door
                "e8b660ec5aa0aa30dab46b267daf3b80996269e7.swf");

            furn = new FurniData();
            furn.id = 1;
            //furn.scaleX = -2f;
            //furn.scaleY = 1f;
            furn.media = new MediaDesc( // fans
                "b6f9dd3e043cd399bca8c386a143d6e3db4a354c.swf");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaDesc( // pinball
                "52c2f1dd0b1bb8aea5e7ce2d98b196c1b88a25bc.swf");
            furn.scaleX = -1;
            furn.loc = new MsoyLocation(.8, 0, .2, 0);
            furn.actionType = FurniData.ACTION_URL;
            //furn.actionData = "http://www.pinballnews.com/";
            furn.actionData = "http://www.t45ol.com/play/420/jungle-quest.html";
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaDesc( // curtain
                "2939c71733856d1affcdd23fae3b24298a8a95f3.swf");
            furn.loc = new MsoyLocation(.2, 0, 1, 0);
            model.addFurni(furn);

            /*
            furn = new FurniData();
            furn.id = 4;
            furn.media = new MediaDesc(5); // Joshua Tree
            furn.loc = new MsoyLocation(.5, 0, 1, 0);
            furn.scaleX = 2;
            furn.scaleY = 2;
            furn.action = "http://bogocorp.com/";
            model.addFurni(furn);
            */

            /*
            furn = new FurniData();
            furn.id = 5;
            furn.media = new MediaDesc(15); // 3d logic
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);
            */

        } else if (sceneId == 5) {
            // faucet
            //decorData.width = 1600;
            //decorData.media = new MediaDesc( // faucet forest
            //    "05164b5141659e18687bea9e7dbd781833cbf28c.png");

            portal.loc = new MsoyLocation(.3125, .71, 0, 0);
            portal.actionData = "1:54";
            portal.media = new MediaDesc( // pipe
                "d63cfbf63645d168094119a784e2e5f780d4218d.png");

        } else if (sceneId == 6) {
            // comic
            //decorData.width = 1600;
            //decorData.media = new MediaDesc( // comic room
            //    "3b9a430a4d2fe6473b2ab71251162a2494843772.png");

            portal.loc = new MsoyLocation(0, 0, .5, 0);
            portal.actionData = "1:52";
            portal.media = new MediaDesc( // bendaydoor
                "dae195a14e7b24fb7d21c0da1404785bdad1cd92.swf");

            p2 = new FurniData();
            p2.id = 52;
            p2.actionType = FurniData.ACTION_PORTAL;
            p2.actionData = "3:51";
            p2.loc = new MsoyLocation(.84, 0, .3, 0);
            p2.media = new MediaDesc( // bendaytransport
                "636dfebde41fb73d0bf6ee3cca0387ac5532b9ce.swf");
            model.addFurni(p2);

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // comic foreground
                "eee1b3961c5b3fd1f1e222ac5601bd7a2063f4d2.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

        } else if (sceneId == 7) {
            // game room background
            //decorData.width = 800;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // reversi... doesn't work
                "6326b311c1ecdc58ddb2f6bb8b3735c79d7088a2.swf");
            furn.loc = new MsoyLocation(.5, 0, 1, 0);
            furn.scaleX = 1.1f;
            furn.scaleY = .9f;
            model.addFurni(furn);

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.actionData = "1:55";
            portal.media = new MediaDesc( // bendaytransport
                "636dfebde41fb73d0bf6ee3cca0387ac5532b9ce.swf");

        } else {
            System.err.println("Unknown scene: " + sceneId);
        }

        model.addFurni(portal);

        return model;
    }

    /** The marshaller that assists us in managing scene updates. */
    protected SceneUpdateMarshaller _updateMarshaller = new SceneUpdateMarshaller(
        // register the update classes
        // (DO NOT CHANGE ORDER! see note in SceneUpdateMarshaller const.)
        ModifyFurniUpdate.class,
        null,                           // previously: ModifyPortalsUpdate
        SceneAttrsUpdate.class
        // end of update class registration (DO NOT CHANGE ORDER)
        );

    /** Internal reference to the decor repository, used to load up decor for each scene. */
    protected DecorRepository _decorRepo;

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
