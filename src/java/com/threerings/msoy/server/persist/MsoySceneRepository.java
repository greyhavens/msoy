//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.ArrayList;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.SimpleRepository;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneModel;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.server.persist.SceneUpdateMarshaller;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.ModifyFurniUpdate;

import static com.threerings.msoy.Log.log;

/**
 * Provides scene storage services for the msoy server.
 */
public class MsoySceneRepository extends SimpleRepository
    implements SceneRepository
{
    /**
     * The database identifier used when establishing a connection.
     */
    public static final String SCENE_DB_IDENT = "scenedb";

    /**
     * Construct.
     */
    public MsoySceneRepository (ConnectionProvider provider)
        throws PersistenceException
    {
        super(provider, SCENE_DB_IDENT);

        maintenance("analyze", "SCENES");
        maintenance("analyze", "PORTALS");
        maintenance("analyze", "FURNI");
    }

    @Override
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        // let's leave this in here even if we just call super, because
        // migration code will come and go
        super.migrateSchema(conn, liaison);

        if (!JDBCUtil.tableExists(conn, "SCENES")) {
            createAndPopulate(conn, liaison);
        }

        // and create the scene-update table
        JDBCUtil.createTableIfMissing(conn, "SCENE_UPDATES", new String[] {
            "SCENE_ID integer not null",
            "SCENE_VERSION integer not null",
            "UPDATE_TYPE integer not null",
            "DATA blob not null",
            "primary key (SCENE_ID, SCENE_VERSION)" }, "");
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

        } else {
            log.warning("Requested to apply unknown update to scene repo " +
                "[update=" + update + "].");
        }

        // finally, update the scene version (which will already be the
        // new version because the update has been applied)
        updateVersion(model.sceneId, model.version);
        log.info("Updated verison of " + model.sceneId + " to " +
            model.version + ".");

        // record the update itself
        insertSceneUpdate(update);
    }

    /**
     * Updates the version of the specified scene in the database.
     */
    public void updateVersion (final int sceneId, final int version)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "update SCENES set VERSION = ? " +
                    "where SCENE_ID = ?";
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(1, version);
                    stmt.setInt(2, sceneId);
                    JDBCUtil.checkedUpdate(stmt, 1);

                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    // documentation inherited from interface SceneRepository
    public UpdateList loadUpdates (final int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        return execute(new Operation<UpdateList>() {
            public UpdateList invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                UpdateList list = new UpdateList();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery("select " +
                        "SCENE_VERSION, UPDATE_TYPE, DATA from SCENE_UPDATES " +
                        "where SCENE_ID = " + sceneId);
                    while (rs.next()) {
                        list.addUpdate(_updateMarshaller.decodeUpdate(
                            sceneId, rs.getInt(1), rs.getInt(2),
                            rs.getBytes(3)));
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
                return list;
            }
        });
    }

    // documentation inherited from interface SceneRepository
    public SceneModel loadSceneModel (final int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        final MsoySceneModel model = new MsoySceneModel();
        final SpotSceneModel spotModel = new SpotSceneModel();
        model.addAuxModel(spotModel);
        model.sceneId = sceneId;

        Boolean success = execute(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // Load: basic scene data
                    ResultSet rs = stmt.executeQuery("select " +
                        "VERSION, NAME, TYPE, DEF_PORTAL_ID, WIDTH, " +
                        "BACKGROUND, MUSIC " +
                        "from SCENES where SCENE_ID=" + sceneId);
                    if (rs.next()) {
                        model.version = rs.getInt(1);
                        model.name = rs.getString(2);
                        model.type = rs.getString(3);
                        spotModel.defaultEntranceId = rs.getInt(4);
                        model.width = rs.getShort(5);
                        int bkgId = rs.getInt(6);
                        if (!rs.wasNull()) {
                            model.background = new MediaData(bkgId);
                        }
                        int musicId = rs.getInt(7);
                        if (!rs.wasNull()) {
                            model.music = new MediaData(musicId);
                        }

                    } else {
                        return Boolean.FALSE; // no scene found
                    }

                    // Load: portals
                    rs = stmt.executeQuery("select " +
                        "PORTAL_ID, TARGET_PORTAL_ID, TARGET_SCENE_ID, " +
                        "MEDIA, X, Y, Z, SCALE_X, SCALE_Y " +
                        "from PORTALS where SCENE_ID=" + sceneId);
                    ArrayList<MsoyPortal> plist = new ArrayList<MsoyPortal>();
                    while (rs.next()) {
                        MsoyPortal p = new MsoyPortal();
                        p.portalId = rs.getShort(1);
                        p.targetPortalId = rs.getShort(2);
                        p.targetSceneId = rs.getInt(3);
                        p.media = new MediaData(rs.getInt(4));
                        p.loc = new MsoyLocation(
                            rs.getFloat(5), rs.getFloat(6), rs.getFloat(7), 0);
                        p.scaleX = rs.getFloat(8);
                        p.scaleY = rs.getFloat(9);
                        plist.add(p);
                    }
                    spotModel.portals = new Portal[plist.size()];
                    plist.toArray(spotModel.portals);

                    // Load: furni
                    rs = stmt.executeQuery("select " +
                        "FURNI_ID, MEDIA, X, Y, Z, SCALE_X, SCALE_Y, ACTION " +
                        "from FURNI where SCENE_ID=" + sceneId);
                    ArrayList<FurniData> flist = new ArrayList<FurniData>();
                    while (rs.next()) {
                        FurniData furni = new FurniData();
                        furni.id = rs.getInt(1);
                        furni.media = new MediaData(rs.getInt(2));
                        furni.loc = new MsoyLocation(
                            rs.getFloat(3), rs.getFloat(4), rs.getFloat(5), 0);
                        furni.scaleX = rs.getFloat(6);
                        furni.scaleY = rs.getFloat(7);
                        furni.action = null; // TODO: decode blob
                        flist.add(furni);
                    }
                    model.furnis = new FurniData[flist.size()];
                    flist.toArray(model.furnis);
                    return Boolean.TRUE; // success

                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        if (!success.booleanValue()) {
            throw new NoSuchSceneException(sceneId);
        }

        return model;
    }

    /**
     * Apply a furniture changing update.
     */
    protected void applyFurniUpdate (
            MsoySceneModel mmodel, ModifyFurniUpdate update)
    {
        // TODO
    }

    /**
     * Insert a new scene and return the newly assigned scene id.
     */
    protected int insertScene (
            Connection conn, DatabaseLiaison liaison, MsoySceneModel model)
        throws SQLException, PersistenceException
    {
        SpotSceneModel spotModel = SpotSceneModel.getSceneModel(model);

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into SCENES " +
                "(VERSION, NAME, TYPE, DEF_PORTAL_ID, WIDTH, " +
                "BACKGROUND, MUSIC) values (?, ?, ?, ?, ?, ?, ?)");

            stmt.setInt(1, model.version);
            stmt.setString(2, model.name);
            stmt.setString(3, model.type);
            stmt.setInt(4, spotModel.defaultEntranceId);
            stmt.setShort(5, model.width);
            if (model.background != null) {
                stmt.setInt(6, model.background.id);
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            if (model.music != null) {
                stmt.setInt(7, model.music.id);
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            JDBCUtil.checkedUpdate(stmt, 1);
            return liaison.lastInsertedId(conn);

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Insert the specified portal into the database.
     */
    protected void insertPortal (
            Connection conn, DatabaseLiaison liaison,
            int sceneId, MsoyPortal p)
        throws SQLException, PersistenceException
    {
        MsoyLocation loc = (MsoyLocation) p.loc;
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into PORTALS " +
                "(SCENE_ID, PORTAL_ID, TARGET_PORTAL_ID, TARGET_SCENE_ID, " +
                "MEDIA, X, Y, Z, SCALE_X, SCALE_Y) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, sceneId);
            stmt.setInt(2, p.portalId);
            stmt.setInt(3, p.targetPortalId);
            stmt.setInt(4, p.targetSceneId);
            stmt.setInt(5, p.media.id);
            stmt.setFloat(6, loc.x);
            stmt.setFloat(7, loc.y);
            stmt.setFloat(8, loc.z);
            stmt.setFloat(9, p.scaleX);
            stmt.setFloat(10, p.scaleY);
            JDBCUtil.checkedUpdate(stmt, 1);

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Insert the specified piece of furni into the database.
     */
    protected void insertFurni (
            Connection conn, DatabaseLiaison liaison,
            int sceneId, FurniData furni)
        throws SQLException, PersistenceException
    {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into FURNI " +
                "(SCENE_ID, FURNI_ID, MEDIA, X, Y, Z, " +
                "SCALE_X, SCALE_Y, ACTION) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setInt(1, sceneId);
            stmt.setInt(2, furni.id);
            stmt.setInt(3, furni.media.id);
            stmt.setFloat(4, furni.loc.x);
            stmt.setFloat(5, furni.loc.y);
            stmt.setFloat(6, furni.loc.z);
            stmt.setFloat(7, furni.scaleX);
            stmt.setFloat(8, furni.scaleY);
            stmt.setBytes(9, null); // TODO: save action
            JDBCUtil.checkedUpdate(stmt, 1);

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Records a scene update to the update table.
     */
    protected void insertSceneUpdate (final SceneUpdate update)
        throws PersistenceException
    {
        // first determine the assigned update type
        final int updateType = _updateMarshaller.getUpdateType(update);
        if (updateType == -1) {
            String errmsg = "Can't insert update of unknown type " +
                "[update=" + update + ", updateClass=" + update.getClass() +
                "]";
            throw new PersistenceException(errmsg);
        }

        // then serialize the update
        final byte[] updateData = _updateMarshaller.persistUpdate(update);

        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                String query = "insert into SCENE_UPDATES (SCENE_ID, " +
                    "SCENE_VERSION, UPDATE_TYPE, DATA) values " +
                    "(?, ?, ?, ?)";
                PreparedStatement stmt = null;
                try {
                    // first insert the new update
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(1, update.getSceneId());
                    stmt.setInt(2, update.getSceneVersion());
                    stmt.setInt(3, updateType);
                    stmt.setBytes(4, updateData);
                    JDBCUtil.checkedUpdate(stmt, 1);
                    JDBCUtil.close(stmt);

                    // then delete any older updates
                    stmt = conn.prepareStatement("delete from UPDATES where " +
                        "SCENE_ID = ? and SCENE_VERSION <= ?");
                    stmt.setInt(1, update.getSceneId());
                    stmt.setInt(2, update.getSceneVersion() -
                        MAX_UPDATES_PER_SCENE);
                    stmt.executeUpdate();

                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Create the database tables and populate them with some starter scenes.
     */
    protected void createAndPopulate (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "SCENES", new String[] {
            "SCENE_ID integer not null auto_increment",
            "VERSION integer not null",
            "NAME varchar(255) not null",
            "TYPE varchar(255)",
            "DEF_PORTAL_ID integer not null",
            "WIDTH integer not null",
            "BACKGROUND integer",
            "MUSIC integer",
            "primary key (SCENE_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "PORTALS", new String[] {
            "SCENE_ID integer not null",
            "PORTAL_ID smallint not null",
            "TARGET_PORTAL_ID smallint not null",
            "TARGET_SCENE_ID integer not null",
            "MEDIA integer not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "primary key (SCENE_ID, PORTAL_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "FURNI", new String[] {
            "SCENE_ID integer not null",
            "FURNI_ID integer not null",
            "MEDIA integer not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "ACTION blob",
            "primary key (SCENE_ID, FURNI_ID)" }, "");

        // populate some starting scenes
        for (int sceneId = 1; sceneId < 8; sceneId++) {
            MsoySceneModel model = createSampleScene(sceneId);
            SpotSceneModel spotModel = SpotSceneModel.getSceneModel(model);
            int insertedId = insertScene(conn, liaison, model);
            if (insertedId != sceneId) {
                throw new RuntimeException("It's not quite right!");
            }

            for (int ii = 0; ii < spotModel.portals.length; ii++) {
                MsoyPortal p = (MsoyPortal) spotModel.portals[ii];
                insertPortal(conn, liaison, sceneId, p);
            }

            for (int ii = 0; ii < model.furnis.length; ii++) {
                insertFurni(conn, liaison, sceneId, model.furnis[ii]);
            }
        }
    }

    /**
     * Create a sample scene.
     */
    protected MsoySceneModel createSampleScene (int sceneId)
    {
        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = sceneId;
        model.version = 1;
        model.name = "SampleScene" + sceneId;
        SpotSceneModel spotty = SpotSceneModel.getSceneModel(model);

        MsoyPortal portal = new MsoyPortal();
        portal.portalId = 1;
        portal.targetPortalId = 1;
        MsoyPortal p2;
        FurniData furn;

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

            p2 = new MsoyPortal();
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

            p2 = new MsoyPortal();
            p2.portalId = 5;
            p2.targetPortalId = 1;
            p2.targetSceneId = 7;
            p2.loc = new MsoyLocation(.95, 0, 1, 0);
            p2.scaleX = p2.scaleY = .3f;
            p2.media = new MediaData(33); // aqua door (made small)
            spotty.addPortal(p2);

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

            p2 = new MsoyPortal();
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

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(17); // comic foreground
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

        } else if (sceneId == 7) {
            // game room background
            model.width = 800;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaData(15); // 3d logic
            furn.loc = new MsoyLocation(.5, 0, 1, 0);
            furn.scaleX = 1.1f;
            furn.scaleY = .9f;
            model.addFurni(furn);

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 5;
            portal.media = new MediaData(19); // bendaytransport

        } else {
            System.err.println("Unknown scene: " + sceneId);
        }

        spotty.addPortal(portal);
        spotty.defaultEntranceId = 1;

        return model;
    }

    /** The marshaller that assists us in managing scene updates. */
    protected SceneUpdateMarshaller _updateMarshaller =
        new SceneUpdateMarshaller(new Class[] {
            // register the update classes
            // (DO NOT CHANGE ORDER! see note in SceneUpdateMarshaller const.)
            ModifyFurniUpdate.class,
            // end of update class registration (DO NOT CHANGE ORDER)
        });

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
