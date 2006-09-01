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
import com.threerings.whirled.spot.data.ModifyPortalsUpdate;
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

        if (!JDBCUtil.tableContainsColumn(conn, "SCENES", "OWNER_ID")) {
            JDBCUtil.addColumn(conn, "SCENES", "OWNER_ID",
                "integer not null", "SCENE_ID");
        }

        // TEMP: db upgrade
        if (Types.INTEGER == JDBCUtil.getColumnType(conn, "PORTALS", "MEDIA")) {
            // just change them all
            JDBCUtil.changeColumn(conn, "PORTALS", "MEDIA",
                "MEDIA varchar(255) not null");
            JDBCUtil.changeColumn(conn, "FURNI", "MEDIA",
                "MEDIA varchar(255) not null");
            JDBCUtil.changeColumn(conn, "SCENES", "BACKGROUND",
                "BACKGROUND varchar(255)");
            JDBCUtil.changeColumn(conn, "SCENES", "MUSIC",
                "MUSIC varchar(255)");

            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("delete from SCENE_UPDATES");
            } finally {
                JDBCUtil.close(stmt);
            }
        }
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

        } else if (update instanceof ModifyPortalsUpdate) {
            applyPortalsUpdate(mmodel, (ModifyPortalsUpdate) update);

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
                PreparedStatement stmt = conn.prepareStatement(
                    "update SCENES set VERSION = ? where SCENE_ID = ?");
                try {
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
                        "OWNER_ID, VERSION, NAME, TYPE, DEF_PORTAL_ID, " +
                        "WIDTH, BACKGROUND, MUSIC " +
                        "from SCENES where SCENE_ID=" + sceneId);
                    if (rs.next()) {
                        model.ownerId = rs.getInt(1);
                        model.version = rs.getInt(2);
                        model.name = rs.getString(3).intern();
                        model.type = rs.getString(4);
                        if (model.type != null) {
                            model.type = model.type.intern();
                        }
                        spotModel.defaultEntranceId = rs.getInt(5);
                        model.width = rs.getShort(6);
                        model.background =
                            MediaData.fromDBString(rs.getString(7));
                        model.music =
                            MediaData.fromDBString(rs.getString(8));

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
                        p.media = MediaData.fromDBString(rs.getString(4));
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
                        furni.media = MediaData.fromDBString(rs.getString(2));
                        furni.loc = new MsoyLocation(
                            rs.getFloat(3), rs.getFloat(4), rs.getFloat(5), 0);
                        furni.scaleX = rs.getFloat(6);
                        furni.scaleY = rs.getFloat(7);
                        furni.action = "http://bogocorp.com"; // null; // TODO: decode blob
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
        final MsoySceneModel mmodel, final ModifyFurniUpdate update)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                if (update.furniRemoved != null) {
                    deleteFurni(conn, liaison, mmodel.sceneId,
                        update.furniRemoved);
                }
                if (update.furniAdded != null) {
                    insertFurni(conn, liaison, mmodel.sceneId,
                        update.furniAdded);
                }
                return null;
            }
        });
    }

    /**
     * Apply a portal changing update.
     */
    protected void applyPortalsUpdate (
        final MsoySceneModel mmodel, final ModifyPortalsUpdate update)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                if (update.portalsRemoved != null) {
                    deletePortals(conn, liaison, mmodel.sceneId,
                        update.portalsRemoved);
                }
                if (update.portalsAdded != null) {
                    insertPortals(conn, liaison, mmodel.sceneId,
                        update.portalsAdded);
                }
                return null;
            }
        });
    }

    /**
     * Create a new blank room for the specified member.
     *
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (int ownerMemberId)
        throws PersistenceException
    {
        // TODO: perhaps clone a prototype room?
        final MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.ownerId = ownerMemberId;
        model.version = 1;
        model.name = "Room"; // TODO

        // TODO
        //SpotSceneModel spotty = SpotSceneModel.getSceneModel(model);
        //spotty.addPortal(portal);
        //spotty.defaultEntranceId = 1;

        return executeUpdate(new Operation<Integer>() {
            public Integer invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                return insertScene(conn, liaison, model);
            }
        });
    }

    /**
     * Insert a new scene, with portals and furni and all, into the database
     * and return the newly assigned sceneId.
     */
    protected int insertScene (
        Connection conn, DatabaseLiaison liaison, MsoySceneModel model)
        throws SQLException, PersistenceException
    {
        SpotSceneModel spotModel = SpotSceneModel.getSceneModel(model);
        int sceneId = insertSceneModel(conn, liaison, model);
        // add the portals and furni for the room
        insertPortals(conn, liaison, sceneId, spotModel.portals);
        insertFurni(conn, liaison, sceneId, model.furnis);
        return sceneId;
    }

    /**
     * Insert a new scene model and return the newly assigned scene id.
     */
    protected int insertSceneModel (
        Connection conn, DatabaseLiaison liaison, MsoySceneModel model)
        throws SQLException, PersistenceException
    {
        SpotSceneModel spotModel = SpotSceneModel.getSceneModel(model);

        PreparedStatement stmt = conn.prepareStatement("insert into SCENES " +
            "(OWNER_ID, VERSION, NAME, TYPE, DEF_PORTAL_ID, WIDTH, " +
            "BACKGROUND, MUSIC) values (?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, model.ownerId);
            stmt.setInt(2, model.version);
            stmt.setString(3, model.name);
            stmt.setString(4, model.type);
            stmt.setInt(5, spotModel.defaultEntranceId);
            stmt.setShort(6, model.width);
            stmt.setString(7, MediaData.asDBString(model.background));
            stmt.setString(8, MediaData.asDBString(model.music));
            JDBCUtil.checkedUpdate(stmt, 1);
            return liaison.lastInsertedId(conn);

        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Insert the specified portals into the database.
     */
    protected void insertPortals (
        Connection conn, DatabaseLiaison liaison, int sceneId,
        Portal[] portals)
        throws SQLException, PersistenceException
    {
        PreparedStatement stmt = conn.prepareStatement("insert into PORTALS " +
            "(SCENE_ID, PORTAL_ID, TARGET_PORTAL_ID, TARGET_SCENE_ID, " +
            "MEDIA, X, Y, Z, SCALE_X, SCALE_Y) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, sceneId);
            for (Portal portal : portals) {
                MsoyPortal p = (MsoyPortal) portal;
                MsoyLocation loc = (MsoyLocation) p.loc;
                stmt.setInt(2, p.portalId);
                stmt.setInt(3, p.targetPortalId);
                stmt.setInt(4, p.targetSceneId);
                stmt.setString(5, MediaData.asDBString(p.media));
                stmt.setFloat(6, loc.x);
                stmt.setFloat(7, loc.y);
                stmt.setFloat(8, loc.z);
                stmt.setFloat(9, p.scaleX);
                stmt.setFloat(10, p.scaleY);
                JDBCUtil.checkedUpdate(stmt, 1);
            }
        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Delete the specified portals from the database.
     */
    protected void deletePortals (
        Connection conn, DatabaseLiaison liaison, int sceneId,
        Portal[] portals)
        throws SQLException, PersistenceException
    {
        PreparedStatement stmt = conn.prepareStatement(
            "delete from PORTALS where SCENE_ID = ? and PORTAL_ID = ?");
        try {
            stmt.setInt(1, sceneId);

            for (Portal p : portals) {
                stmt.setInt(2, p.portalId);
                JDBCUtil.checkedUpdate(stmt, 1);
            }
        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Insert the specified pieces of furni into the database.
     */
    protected void insertFurni (
        Connection conn, DatabaseLiaison liaison, int sceneId,
        FurniData[] furni)
        throws SQLException, PersistenceException
    {
        PreparedStatement stmt = conn.prepareStatement("insert into FURNI " +
            "(SCENE_ID, FURNI_ID, MEDIA, X, Y, Z, SCALE_X, SCALE_Y, ACTION) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, sceneId);

            for (FurniData f : furni) {
                stmt.setInt(2, f.id);
                stmt.setString(3, MediaData.asDBString(f.media));
                stmt.setFloat(4, f.loc.x);
                stmt.setFloat(5, f.loc.y);
                stmt.setFloat(6, f.loc.z);
                stmt.setFloat(7, f.scaleX);
                stmt.setFloat(8, f.scaleY);
                stmt.setBytes(9, null); // TODO: save action
                JDBCUtil.checkedUpdate(stmt, 1);
            }
        } finally {
            JDBCUtil.close(stmt);
        }
    }

    /**
     * Delete the specified pieces of furni from the database.
     */
    protected void deleteFurni (
        Connection conn, DatabaseLiaison liaison, int sceneId,
        FurniData[] furni)
        throws SQLException, PersistenceException
    {
        PreparedStatement stmt = conn.prepareStatement(
            "delete from FURNI where SCENE_ID = ? and FURNI_ID = ?");
        try {
            stmt.setInt(1, sceneId);

            for (FurniData f : furni) {
                stmt.setInt(2, f.id);
                JDBCUtil.checkedUpdate(stmt, 1);
            }
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
                PreparedStatement stmt = conn.prepareStatement(
                    "insert into SCENE_UPDATES (SCENE_ID, " +
                    "SCENE_VERSION, UPDATE_TYPE, DATA) values " +
                    "(?, ?, ?, ?)");
                try {
                    // first insert the new update
                    stmt.setInt(1, update.getSceneId());
                    stmt.setInt(2, update.getSceneVersion());
                    stmt.setInt(3, updateType);
                    stmt.setBytes(4, updateData);
                    JDBCUtil.checkedUpdate(stmt, 1);
                    JDBCUtil.close(stmt);

                    // then delete any older updates
                    stmt = conn.prepareStatement("delete from SCENE_UPDATES " +
                        "where SCENE_ID = ? and SCENE_VERSION <= ?");
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
            "OWNER_ID integer not null",
            "VERSION integer not null",
            "NAME varchar(255) not null",
            "TYPE varchar(255)",
            "DEF_PORTAL_ID integer not null",
            "WIDTH integer not null",
            "BACKGROUND varchar(255)",
            "MUSIC varchar(255)",
            "primary key (SCENE_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "PORTALS", new String[] {
            "SCENE_ID integer not null",
            "PORTAL_ID smallint not null",
            "TARGET_PORTAL_ID smallint not null",
            "TARGET_SCENE_ID integer not null",
            "MEDIA varchar(255) not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "primary key (SCENE_ID, PORTAL_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "FURNI", new String[] {
            "SCENE_ID integer not null",
            "FURNI_ID integer not null",
            "MEDIA varchar(255) not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "ACTION blob",
            "primary key (SCENE_ID, FURNI_ID)" }, "");

        // populate some starting scenes
        for (int sceneId = 1; sceneId < 8; sceneId++) {
            if (sceneId !=
                    insertScene(conn, liaison, createSampleScene(sceneId))) {
                throw new RuntimeException("It's not quite right!");
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
        portal.targetPortalId = -1;
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

            furn = new FurniData();
            furn.id = 6;
            furn.media = new MediaData(53); // square100
            furn.loc = new MsoyLocation(.2, 0, .2, 0);
            furn.scaleX = .5f;
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
        new SceneUpdateMarshaller(
            // register the update classes
            // (DO NOT CHANGE ORDER! see note in SceneUpdateMarshaller const.)
            ModifyFurniUpdate.class,
            ModifyPortalsUpdate.class
            // end of update class registration (DO NOT CHANGE ORDER)
        );

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
