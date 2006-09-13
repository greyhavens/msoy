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

import com.threerings.msoy.data.MediaDesc;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

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

        // TEMP: db update of a massive kind
        if (!JDBCUtil.tableContainsColumn(conn, "SCENES", "BACKGROUND_HASH")) {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("drop table SCENES");
                stmt.executeUpdate("drop table PORTALS");
                stmt.executeUpdate("drop table FURNI");
                stmt.executeUpdate("drop table SCENE_UPDATES");

            } finally {
                JDBCUtil.close(stmt);
            }
        }

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

        } else if (update instanceof ModifyPortalsUpdate) {
            applyPortalsUpdate(mmodel, (ModifyPortalsUpdate) update);

        } else if (update instanceof SceneAttrsUpdate) {
            applySceneAttrsUpdate(mmodel, (SceneAttrsUpdate) update);

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
                        "DEPTH, WIDTH, HORIZON, BACKGROUND_HASH, " +
                        "BACKGROUND_TYPE, MUSIC_HASH, MUSIC_TYPE " +
                        "from SCENES where SCENE_ID=" + sceneId);
                    if (rs.next()) {
                        model.ownerId = rs.getInt(1);
                        model.version = rs.getInt(2);
                        model.name = rs.getString(3).intern();
                        model.type = rs.getByte(4);
                        spotModel.defaultEntranceId = rs.getInt(5);
                        model.depth = rs.getShort(6);
                        model.width = rs.getShort(7);
                        model.horizon = rs.getFloat(8);
                        byte[] hash = rs.getBytes(9);
                        if (hash != null) {
                            model.background = new MediaDesc();
                            model.background.hash = hash;
                            model.background.mimeType = rs.getByte(10);
                        }
                        hash = rs.getBytes(11);
                        if (hash != null) {
                            model.music = new MediaDesc();
                            model.music.hash = hash;
                            model.music.mimeType = rs.getByte(12);
                        }

                    } else {
                        return Boolean.FALSE; // no scene found
                    }

                    // Load: portals
                    rs = stmt.executeQuery("select " +
                        "PORTAL_ID, TARGET_PORTAL_ID, TARGET_SCENE_ID, " +
                        "MEDIA_HASH, MEDIA_TYPE, X, Y, Z, SCALE_X, SCALE_Y " +
                        "from PORTALS where SCENE_ID=" + sceneId);
                    ArrayList<MsoyPortal> plist = new ArrayList<MsoyPortal>();
                    while (rs.next()) {
                        MsoyPortal p = new MsoyPortal();
                        p.portalId = rs.getShort(1);
                        p.targetPortalId = rs.getShort(2);
                        p.targetSceneId = rs.getInt(3);
                        p.media = new MediaDesc();
                        p.media.hash = rs.getBytes(4);
                        p.media.mimeType = rs.getByte(5);
                        p.loc = new MsoyLocation(
                            rs.getFloat(6), rs.getFloat(7), rs.getFloat(8), 0);
                        p.scaleX = rs.getFloat(9);
                        p.scaleY = rs.getFloat(10);
                        plist.add(p);
                    }
                    spotModel.portals = new Portal[plist.size()];
                    plist.toArray(spotModel.portals);

                    // Load: furni
                    rs = stmt.executeQuery("select " +
                        "FURNI_ID, MEDIA_HASH, MEDIA_TYPE, X, Y, Z, " +
                        "SCALE_X, SCALE_Y, ACTION " +
                        "from FURNI where SCENE_ID=" + sceneId);
                    ArrayList<FurniData> flist = new ArrayList<FurniData>();
                    while (rs.next()) {
                        FurniData furni = new FurniData();
                        furni.id = rs.getInt(1);
                        furni.media = new MediaDesc();
                        furni.media.hash = rs.getBytes(2);
                        furni.media.mimeType = rs.getByte(3);
                        furni.loc = new MsoyLocation(
                            rs.getFloat(4), rs.getFloat(5), rs.getFloat(6), 0);
                        furni.scaleX = rs.getFloat(7);
                        furni.scaleY = rs.getFloat(8);
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
     * Apply an update that changes the basic scene attributes.
     */
    protected void applySceneAttrsUpdate (
        final MsoySceneModel mmodel, final SceneAttrsUpdate update)
        throws PersistenceException
    {
        executeUpdate(new Operation<Object>() {
            public Object invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = conn.prepareStatement(
                    "update SCENES " +
                    "set TYPE=?, DEPTH=?, WIDTH=?, HORIZON=?, " +
                    "BACKGROUND_HASH=?, BACKGROUND_TYPE=?, " +
                    "MUSIC_HASH=?, MUSIC_TYPE=? " +
                    "where SCENE_ID=" + mmodel.sceneId);
                try {
                    stmt.setByte(1, update.type);
                    stmt.setInt(2, update.depth);
                    stmt.setInt(3, update.width);
                    stmt.setFloat(4, update.horizon);
                    if (update.background != null) {
                        stmt.setBytes(5, update.background.hash);
                        stmt.setByte(6, update.background.mimeType);
                    } else {
                        stmt.setBytes(5, null);
                        stmt.setByte(6, (byte) 0);
                    }
                    if (update.music != null) {
                        stmt.setBytes(7, update.music.hash);
                        stmt.setByte(8, update.music.mimeType);
                    } else {
                        stmt.setBytes(7, null);
                        stmt.setByte(8, (byte) 0);
                    }

                    JDBCUtil.checkedUpdate(stmt, 1);
                } finally {
                    JDBCUtil.close(stmt);
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
            "(OWNER_ID, VERSION, NAME, TYPE, DEF_PORTAL_ID, DEPTH, WIDTH, " +
            "HORIZON, BACKGROUND_HASH, BACKGROUND_TYPE, " +
            "MUSIC_HASH, MUSIC_TYPE) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, model.ownerId);
            stmt.setInt(2, model.version);
            stmt.setString(3, model.name);
            stmt.setByte(4, model.type);
            stmt.setInt(5, spotModel.defaultEntranceId);
            stmt.setShort(6, model.depth);
            stmt.setShort(7, model.width);
            stmt.setFloat(8, model.horizon);
            if (model.background != null) {
                stmt.setBytes(9, model.background.hash);
                stmt.setByte(10, model.background.mimeType);
            } else {
                stmt.setBytes(9, null);
                stmt.setByte(10, (byte) 0);
            }
            if (model.music != null) {
                stmt.setBytes(11, model.music.hash);
                stmt.setByte(12, model.music.mimeType);
            } else {
                stmt.setBytes(11, null);
                stmt.setByte(12, (byte) 0);
            }
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
            "MEDIA_HASH, MEDIA_TYPE, X, Y, Z, SCALE_X, SCALE_Y) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, sceneId);
            for (Portal portal : portals) {
                MsoyPortal p = (MsoyPortal) portal;
                MsoyLocation loc = (MsoyLocation) p.loc;
                stmt.setInt(2, p.portalId);
                stmt.setInt(3, p.targetPortalId);
                stmt.setInt(4, p.targetSceneId);
                stmt.setBytes(5, p.media.hash);
                stmt.setByte(6, p.media.mimeType);
                stmt.setFloat(7, loc.x);
                stmt.setFloat(8, loc.y);
                stmt.setFloat(9, loc.z);
                stmt.setFloat(10, p.scaleX);
                stmt.setFloat(11, p.scaleY);
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
            "(SCENE_ID, FURNI_ID, MEDIA_HASH, MEDIA_TYPE, X, Y, Z, " +
            "SCALE_X, SCALE_Y, ACTION) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, sceneId);

            for (FurniData f : furni) {
                stmt.setInt(2, f.id);
                stmt.setBytes(3, f.media.hash);
                stmt.setByte(4, f.media.mimeType);
                stmt.setFloat(5, f.loc.x);
                stmt.setFloat(6, f.loc.y);
                stmt.setFloat(7, f.loc.z);
                stmt.setFloat(8, f.scaleX);
                stmt.setFloat(9, f.scaleY);
                stmt.setBytes(10, null); // TODO: save action
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
            "TYPE tinyint not null",
            "DEF_PORTAL_ID integer not null",
            "DEPTH integer not null",
            "WIDTH integer not null",
            "HORIZON float not null",
            "BACKGROUND_HASH tinyblob",
            "BACKGROUND_TYPE tinyint",
            "MUSIC_HASH tinyblob",
            "MUSIC_TYPE tinyint",
            "primary key (SCENE_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "PORTALS", new String[] {
            "SCENE_ID integer not null",
            "PORTAL_ID smallint not null",
            "TARGET_PORTAL_ID smallint not null",
            "TARGET_SCENE_ID integer not null",
            "MEDIA_HASH tinyblob not null",
            "MEDIA_TYPE tinyint not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "primary key (SCENE_ID, PORTAL_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "FURNI", new String[] {
            "SCENE_ID integer not null",
            "FURNI_ID integer not null",
            "MEDIA_HASH tinyblob not null",
            "MEDIA_TYPE tinyint not null",
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
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.width = 1600;
            model.background = new MediaDesc( // crayon room
                "b3084c929b49cce36a6708fb8f47a45c59e1d400.png");

            portal.loc = new MsoyLocation(0, 0, .3, 0);
            portal.targetSceneId = 2;
            portal.targetPortalId = 1;
            portal.scaleX = portal.scaleY = (float) (1 / .865f);
            portal.media = new MediaDesc( // smile door
                "7511842ed6201ccddb7b072fc4886c21d395376f.png");

            p2 = new MsoyPortal();
            p2.portalId = 2;
            p2.targetPortalId = 1;
            p2.targetSceneId = 6;
            p2.loc = new MsoyLocation(.8, 0, 1, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .55);
            p2.media = new MediaDesc( // aqua door
                "7fbc0922c7f36e1ce14648466b42c093185b6c1b.png");
            spotty.addPortal(p2);

            p2 = new MsoyPortal();
            p2.portalId = 3;
            p2.targetPortalId = 1;
            p2.targetSceneId = 4;
            p2.loc = new MsoyLocation(1, 0, .3, 0);
            p2.scaleX = p2.scaleY = (float) (1 / .865f);
            p2.media = new MediaDesc( // red door
                "2c75bf2cc3e9a35aafb2e96bd13dd05060d132a0.png");
            spotty.addPortal(p2);

            p2 = new MsoyPortal();
            p2.portalId = 4;
            p2.targetPortalId = 1;
            p2.targetSceneId = 5;
            p2.loc = new MsoyLocation(.75, 1, .5, 0);
            p2.media = new MediaDesc( // ladder
                "92d6dbe0b44a1c070b638f93d9fff8a907ba1cda.png");
            spotty.addPortal(p2);

            p2 = new MsoyPortal();
            p2.portalId = 5;
            p2.targetPortalId = 1;
            p2.targetSceneId = 7;
            p2.loc = new MsoyLocation(.95, 0, 1, 0);
            p2.scaleX = p2.scaleY = .3f;
            p2.media = new MediaDesc(
                "7fbc0922c7f36e1ce14648466b42c093185b6c1b.png");
            spotty.addPortal(p2);

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
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.background = new MediaDesc( // alley
                "13fd51be845d51b1571424cf459ce4fd78472ec2.png");
            model.music = new MediaDesc( // boll weevil
                "71a3c968012324a387179f2e17ba8f1a5d2c685d.mp3");

            portal.loc = new MsoyLocation(0, .1, .53, 180);
            portal.targetSceneId = 1;
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
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.width = 800;
            model.background = new MediaDesc( // cliff background
                "974259e79d58c34beffe67fb781832183309fe57.swf");

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.targetSceneId = 6;
            portal.targetPortalId = 2;
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
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.width = 800;
            model.background = new MediaDesc( // fancy room
                "95101b275b607c5c02a8a411a09082ef2e9b98a7.png");

            portal.loc = new MsoyLocation(0, 0, .8, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 3;
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
            //furn.action = "http://www.pinballnews.com/";
            furn.action = "http://www.t45ol.com/play/420/jungle-quest.html";
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
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.width = 1600;
            model.background = new MediaDesc( // faucet forest
                "05164b5141659e18687bea9e7dbd781833cbf28c.png");

            portal.loc = new MsoyLocation(.3125, .71, 0, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 4;
            portal.media = new MediaDesc( // pipe
                "d63cfbf63645d168094119a784e2e5f780d4218d.png");

        } else if (sceneId == 6) {
            // comic
            model.type = MsoySceneModel.IMAGE_OVERLAY;
            model.width = 1600;
            model.background = new MediaDesc( // comic room
                "3b9a430a4d2fe6473b2ab71251162a2494843772.png");

            portal.loc = new MsoyLocation(0, 0, .5, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 2;
            portal.media = new MediaDesc( // bendaydoor
                "dae195a14e7b24fb7d21c0da1404785bdad1cd92.swf");

            p2 = new MsoyPortal();
            p2.portalId = 2;
            p2.targetSceneId = 3;
            p2.targetPortalId = 1;
            p2.loc = new MsoyLocation(.84, 0, .3, 0);
            p2.media = new MediaDesc( // bendaytransport
                "636dfebde41fb73d0bf6ee3cca0387ac5532b9ce.swf");
            spotty.addPortal(p2);

            /*
            p2 = new MsoyPortal();
            p2.portalId = 3;
            p2.targetPortalId = 1;
            p2.targetSceneId = 2;
            p2.loc = new MsoyLocation(.5, 0, .5, 0);
            p2.media = new MediaDesc(6);
            spotty.addPortal(p2);
            */

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // comic foreground
                "eee1b3961c5b3fd1f1e222ac5601bd7a2063f4d2.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            model.addFurni(furn);

        } else if (sceneId == 7) {
            // game room background
            model.width = 800;

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // reversi... doesn't work
                "6326b311c1ecdc58ddb2f6bb8b3735c79d7088a2.swf");
            furn.loc = new MsoyLocation(.5, 0, 1, 0);
            furn.scaleX = 1.1f;
            furn.scaleY = .9f;
            model.addFurni(furn);

            portal.loc = new MsoyLocation(.5, 0, .5, 0);
            portal.targetSceneId = 1;
            portal.targetPortalId = 5;
            portal.media = new MediaDesc( // bendaytransport
                "636dfebde41fb73d0bf6ee3cca0387ac5532b9ce.swf");

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
            ModifyPortalsUpdate.class,
            SceneAttrsUpdate.class
            // end of update class registration (DO NOT CHANGE ORDER)
        );

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
