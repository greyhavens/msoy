//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.SimpleRepository;
import com.samskivert.jdbc.TransitionRepository;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.StringUtil;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.server.persist.SceneUpdateMarshaller;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.item.web.Decor;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.world.data.DecorData;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
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

        // TEMP: can be removed after all servers past 2006-10-23
        if (!JDBCUtil.tableContainsColumn(conn, "FURNI", "LAYOUT_INFO")) {
            JDBCUtil.addColumn(conn, "FURNI", "LAYOUT_INFO",
                "tinyint not null", "Z");
        }
        // END: temp
        
        // TEMP: can be removed after all servers past 2006-12-05
        if (!JDBCUtil.tableContainsColumn(conn, "SCENES", "OWNER_TYPE")) {
            JDBCUtil.addColumn(conn, "SCENES", "OWNER_TYPE",
                "tinyint not null", "SCENE_ID"); 
        }
        // END: temp
        
        // TEMP: can be removed after all servers past 2006-12-06
        if (JDBCUtil.tableContainsColumn(conn, "SCENES", "TYPE")) {
            JDBCUtil.changeColumn(conn, "SCENES", "TYPE", "SCENE_TYPE tinyint not null");
        }
        // END: temp

        // TEMP: can be removed after all servers past 2006-12-07
        if (true) {
            Statement stmt = conn.createStatement();
            try {
                stmt.executeUpdate("update SCENES set OWNER_TYPE=1 " +
                    "where OWNER_TYPE=0");
            } finally {
                JDBCUtil.close(stmt);
            }
        }
        // END: temp

        // TEMP: migration; this so needs to be Depot-ed
        JDBCUtil.dropColumn(conn, "FURNI", "MEMORY_ID");
        // END TEMP

        // TEMP: removable after all servers are past the date specified...
        MsoyServer.transitRepo.transition(getClass(), "delUpdates_20070108",
            new TransitionRepository.Transition() {
                public void run ()
                    throws PersistenceException
                {
                    executeUpdate(new Operation<Void>() {
                        public Void invoke (Connection conn,
                            DatabaseLiaison liaison)
                            throws SQLException, PersistenceException
                        {
                            Statement stmt = conn.createStatement();
                            try {
                                stmt.executeUpdate("delete from SCENE_UPDATES");
                            } finally {
                                JDBCUtil.close(stmt);
                            }
                            return null;
                        }
                    });
                }
            });
        // END: temp

        
        // TEMP: decor additions. removable after all servers are past April 1 2007
        if (!JDBCUtil.tableContainsColumn(conn, "SCENES", "DECOR_ID")) {
            // add decor data columns
            JDBCUtil.addColumn(conn, "SCENES", "DECOR_ID", "int not null", "SCENE_TYPE");
            JDBCUtil.addColumn(conn, "SCENES", "DECOR_MEDIA_TYPE", "tinyint not null", "DECOR_ID");
            JDBCUtil.addColumn(conn, "SCENES", "DECOR_MEDIA_HASH", "tinyblob not null", "DECOR_ID");
        }
        // second round of decor additions
        if (!JDBCUtil.tableContainsColumn(conn, "SCENES", "HEIGHT")) {

            // add the height column
            JDBCUtil.addColumn(conn, "SCENES", "HEIGHT", "integer not null", "WIDTH");

            // insert default height values
            Statement stmt = conn.createStatement();
            try { 
                stmt.executeUpdate("update SCENES set HEIGHT=494");
            } finally {
                JDBCUtil.close(stmt);
            }
            
            Statement deletestmt = conn.createStatement();
            try { // separately, try to delete any new stale updates
                deletestmt.executeUpdate("delete from SCENE_UPDATES");
            } finally {
                JDBCUtil.close(deletestmt);
            }
        }
        // END: temp
    }

    /**
     * Provides any additional initialization that needs to happen after runtime
     * configuration had been loaded, and other services initialized.
     */
    public void finishInit ()
    {
        // TEMP: decor migration. removable after all servers are past April 15 2007 (?)
        // FIXME ROBERT
        Statement getData = null, getMax = null;
        
        try {
                Connection conn = _provider.getConnection(_dbident, false);

                getData = conn.createStatement();
                getMax = conn.createStatement();
            
                // find the next acceptable primary key for decor records
                ResultSet decorMax = getMax.executeQuery("select max(itemId) from DecorRecord");
                int newDecorId = decorMax.first() ? decorMax.getInt(1) : 0;

                // pull out all scenes that use a background furni, and no decor
                ResultSet results = getData.executeQuery(
                    "select * from SCENES left join FURNI on SCENES.SCENE_ID = FURNI.SCENE_ID " +
                    "where FURNI.ACTION_TYPE = -1 AND DECOR_ID = 0");

                int count = results.last() ? results.getRow() : 0;
                if (count > 0) {
                    log.info("*** DECOR MIGRATION");
                    log.info("Found: " + count + " scene(s) with background furnis");
                    
                    results.first();
                    do {

                        // pull out all relevant scene info...
                        int sceneId = results.getInt("SCENES.SCENE_ID");
                        short furniId = results.getShort("FURNI_ID");
                        int itemId = results.getInt("ITEM_ID");
                        byte itemType = results.getByte("ITEM_TYPE");
                        int ownerId = results.getInt("OWNER_ID");
                        String sceneName = results.getString("NAME");
                        int width = results.getInt("WIDTH");
                        int height = results.getInt("HEIGHT");
                        int depth = results.getInt("DEPTH");
                        float horizon = results.getFloat("HORIZON");
                        
                        // ...and furni info
                        byte mediaType = results.getByte("MEDIA_TYPE");
                        byte[] mediaHash = results.getBytes("MEDIA_HASH");
                        MediaDesc media = createMediaDesc(mediaHash, mediaType);
                        log.info("Scene: " + sceneId + " - " + sceneName + ", " +
                                 width + "x" + height + "x" + depth + ":" + horizon);
                        log.info("       background media: " + media);
                        
                        // create a new decor item (without going through the item repository;
                        // we don't have access to that here)
                        PreparedStatement ins = conn.prepareStatement(
                            "insert into DecorRecord (itemId, height, width, depth, " +
                            " horizon, creatorId, ownerId, used, " +
                            " location, name, thumbMediaHash, thumbMimeType, " +
                            " thumbConstraint, furniMediaHash, furniMimeType, furniConstraint, " +
                            " type) " +
                            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                        ins.setInt(1, ++newDecorId);
                        ins.setShort(2, (short) height);
                        ins.setShort(3, (short) width);
                        ins.setShort(4, (short) depth);
                        ins.setFloat(5, horizon);
                        ins.setInt(6, ownerId);
                        ins.setInt(7, ownerId);
                        ins.setByte(8, Item.USED_AS_DECOR);
                        ins.setInt(9, sceneId);
                        ins.setString(10, (sceneName != "" ? sceneName : "new") + " decor");
                        ins.setBytes(11, mediaHash);
                        ins.setByte(12, mediaType);
                        ins.setByte(13, MediaDesc.HORIZONTALLY_CONSTRAINED); 
                        ins.setBytes(14, mediaHash);
                        ins.setByte(15, mediaType);
                        ins.setByte(16, MediaDesc.HORIZONTALLY_CONSTRAINED);
                        ins.setByte(17, Decor.IMAGE_OVERLAY);

                        // update the scene to use this decor
                        PreparedStatement scene = conn.prepareStatement(
                            "update SCENES set DECOR_ID=?, DECOR_MEDIA_HASH=?, " +
                            "DECOR_MEDIA_TYPE=? where SCENE_ID=" + sceneId);
                        scene.setInt(1, newDecorId);
                        scene.setBytes(2, mediaHash);
                        scene.setByte(3, mediaType);

                        // remove the background furni from the scene
                        PreparedStatement deleteFurni = conn.prepareStatement(
                            "delete from FURNI where SCENE_ID = ? and FURNI_ID = ?");
                        deleteFurni.setInt(1, sceneId);
                        deleteFurni.setShort(2, furniId);

                        // and finally, if this furni was an Item, mark it as no longer used.
                        PreparedStatement unused = null;
                        if (itemId != 0 && itemType > 0 && itemType < Item.VIDEO) {
                            final String[] tables =
                                new String[] { null, "Photo", "Document", "Furniture",
                                               "Game", "Avatar", "Pet", "Audio" };
                            String table = tables[itemType];
                            if (table != null) {
                                table += "Record";  // fix up the table name
                                unused = conn.prepareStatement(
                                    "update " + table +
                                    " set USED = ?, LOCATION = ? where itemId = ?");
                                unused.setByte(1, (byte) 0);
                                unused.setInt(2, 0);
                                unused.setInt(3, itemId);
                            }                                
                        }
                        
                        // now run those queries!
                        try {
                            JDBCUtil.checkedUpdate(ins, 1);
                            JDBCUtil.checkedUpdate(scene, 1);
                            JDBCUtil.checkedUpdate(deleteFurni, 1);
                            int unusedcount = (unused != null ? unused.executeUpdate() : 0);
                            log.info("moved to decor #" + newDecorId);
                            log.info("marked " + unusedcount + " item(s) as unused.");
                        } finally {
                            JDBCUtil.close(ins);
                            JDBCUtil.close(scene);
                            JDBCUtil.close(deleteFurni);
                            if (unused != null) {
                                JDBCUtil.close(unused);
                            }
                        }
                    } while (results.next());
                    
                    log.info("Decor migration done.\n");
                }
                
                JDBCUtil.close(getMax);
                JDBCUtil.close(getData);
                
                // just for good measure, delete any new stale scene updates
                Statement deletestmt = conn.createStatement();
                try { 
                    deletestmt.executeUpdate("delete from SCENE_UPDATES");
                } finally {
                    JDBCUtil.close(deletestmt);
                }
        } catch (Exception ex) {
            log.warning("Failed finishInit(): " + ex.toString());
        }
        
        // END TEMP
    }
        
    /**
     * Retrieve a list of all the scenes that the user directly owns.
     */
    public ArrayList<SceneBookmarkEntry> getOwnedScenes (
        final byte ownerType, final int memberId)
        throws PersistenceException
    {
        return execute(new Operation<ArrayList<SceneBookmarkEntry>>() {
            public ArrayList<SceneBookmarkEntry> invoke (
                Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ArrayList<SceneBookmarkEntry> list =
                    new ArrayList<SceneBookmarkEntry>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery("select " +
                        "SCENE_ID, NAME from SCENES " +
                        "where OWNER_TYPE = " + ownerType + " " +
                        "and OWNER_ID = " + memberId);
                    while (rs.next()) {
                        list.add(new SceneBookmarkEntry(
                            rs.getInt(1), rs.getString(2), 0L));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }

                return list;
            }
        });
    }

    /**
     * Given a list of scene ids, return a map containing the current names,
     * indexed by scene id.
     */
    public HashIntMap<String> identifyScenes (int[] scenes)
        throws PersistenceException
    {
        final String sceneSet = StringUtil.toString(scenes);

        return execute(new Operation<HashIntMap<String>>() {
            public HashIntMap<String> invoke (
                Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                HashIntMap<String> names = new HashIntMap<String>();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery("select " +
                        "SCENE_ID, NAME from SCENES " +
                        "where SCENE_ID in " + sceneSet);
                    while (rs.next()) {
                        names.put(rs.getInt(1), rs.getString(2));
                    }
                } finally {
                    JDBCUtil.close(stmt);
                }

                return names;
            }
        });
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
        // TEMP compatibility mode: since we have some scenes with decor and some without,
        // just create a default decor for every scene, and clobber the defaults with
        // what we get from the database.
        model.decorData = MsoySceneModel.createDefaultDecorData();
        model.sceneId = sceneId;

        Boolean success = execute(new Operation<Boolean>() {
            public Boolean invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                Statement stmt = conn.createStatement();
                try {
                    // Load: basic scene data
                    ResultSet rs = stmt.executeQuery(
                        "select OWNER_TYPE, OWNER_ID, VERSION, NAME, " +
                        "ENTRANCE_X, ENTRANCE_Y, ENTRANCE_Z, SCENE_TYPE, " +
                        "DEPTH, WIDTH, HEIGHT, HORIZON,  " +
                        "DECOR_ID, DECOR_MEDIA_HASH, DECOR_MEDIA_TYPE " +
                        "from SCENES where SCENE_ID=" + sceneId);
                    if (rs.next()) {
                        model.ownerType = rs.getByte(1);
                        model.ownerId = rs.getInt(2);
                        model.version = rs.getInt(3);
                        model.name = rs.getString(4).intern();
                        model.entrance = new MsoyLocation(
                            rs.getFloat(5), rs.getFloat(6), rs.getFloat(7),
                            180);

                        DecorData d = model.decorData;
                        d.type = rs.getByte(8);
                        d.depth = rs.getShort(9);
                        d.width = rs.getShort(10);
                        d.height = rs.getShort(11); 
                        d.horizon = rs.getFloat(12);
                        d.itemId = rs.getInt(13);
                        if (d.itemId != 0) { // only clobber media if the decor item exists
                            d.media = createMediaDesc(rs.getBytes(14), rs.getByte(15));
                        }
                    } else {
                        return Boolean.FALSE; // no scene found
                    }

                    // Load: furni
                    rs = stmt.executeQuery("select " +
                        "FURNI_ID, ITEM_TYPE, ITEM_ID, " +
                        "MEDIA_HASH, MEDIA_TYPE, X, Y, Z, LAYOUT_INFO, " +
                        "SCALE_X, SCALE_Y, ACTION_TYPE, ACTION_DATA " +
                        "from FURNI where SCENE_ID=" + sceneId);
                    ArrayList<FurniData> flist = new ArrayList<FurniData>();
                    while (rs.next()) {
                        FurniData furni = new FurniData();
                        furni.id = rs.getShort(1);
                        furni.itemType = rs.getByte(2);
                        furni.itemId = rs.getInt(3);
                        furni.media = createMediaDesc(rs.getBytes(4), rs.getByte(5));
                        furni.loc = new MsoyLocation(
                            rs.getFloat(6), rs.getFloat(7), rs.getFloat(8), 0);
                        furni.layoutInfo = rs.getByte(9);
                        furni.scaleX = rs.getFloat(10);
                        furni.scaleY = rs.getFloat(11);
                        furni.actionType = rs.getByte(12);
                        furni.actionData = rs.getString(13);
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
                    "update SCENES set NAME=?, SCENE_TYPE=?, DECOR_ID=?, DECOR_MEDIA_HASH=?, " +
                    "DECOR_MEDIA_TYPE=?, DEPTH=?, WIDTH=?, HEIGHT=?," +
                    "HORIZON=?, ENTRANCE_X=?, ENTRANCE_Y=?, ENTRANCE_Z=? " +
                    "where SCENE_ID=" + mmodel.sceneId);
                try {
                    stmt.setString(1, update.name);
                    stmt.setByte(2, update.decorData.type);
                    stmt.setInt(3, update.decorData.itemId);
                    stmt.setBytes(4, flattenMediaDesc(update.decorData.media));
                    stmt.setByte(5, update.decorData.media.mimeType);
                    stmt.setInt(6, update.decorData.depth);
                    stmt.setInt(7, update.decorData.width);
                    stmt.setInt(8, update.decorData.height);
                    stmt.setFloat(9, update.decorData.horizon);
                    stmt.setFloat(10, update.entrance.x);
                    stmt.setFloat(11, update.entrance.y);
                    stmt.setFloat(12, update.entrance.z);

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
    public int createBlankRoom (byte ownerType, int ownerId, String roomName)
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
        f.actionData = "1:A common area";
        model.addFurni(f);

        return executeUpdate(new Operation<Integer>() {
            public Integer invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                return insertScene(conn, liaison, model);
            }
        });
    }

    /**
     * Insert a new scene, with furni and all, into the database
     * and return the newly assigned sceneId.
     */
    protected int insertScene (
        Connection conn, DatabaseLiaison liaison, MsoySceneModel model)
        throws SQLException, PersistenceException
    {
        int sceneId = insertSceneModel(conn, liaison, model);
        // add the furni for the room
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
        PreparedStatement stmt = conn.prepareStatement(
            "insert into SCENES " +
            "(OWNER_TYPE, OWNER_ID, VERSION, NAME, " +
            "SCENE_TYPE, DECOR_ID, DECOR_MEDIA_HASH, DECOR_MEDIA_TYPE, " +
            "DEPTH, WIDTH, HEIGHT, HORIZON, " +
            "ENTRANCE_X, ENTRANCE_Y, ENTRANCE_Z) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setByte(1, model.ownerType);
            stmt.setInt(2, model.ownerId);
            stmt.setInt(3, model.version);
            stmt.setString(4, model.name);
            stmt.setByte(5, model.decorData.type);
            stmt.setInt(6, model.decorData.itemId);
            stmt.setBytes(7, flattenMediaDesc(model.decorData.media));
            stmt.setByte(8, model.decorData.media.mimeType);
            stmt.setShort(9, model.decorData.depth);
            stmt.setShort(10, model.decorData.width);
            stmt.setShort(11, model.decorData.height);
            stmt.setFloat(12, model.decorData.horizon);
            stmt.setFloat(13, model.entrance.x);
            stmt.setFloat(14, model.entrance.y);
            stmt.setFloat(15, model.entrance.z);
            JDBCUtil.checkedUpdate(stmt, 1);
            return liaison.lastInsertedId(conn);
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
            "(SCENE_ID, FURNI_ID, ITEM_TYPE, ITEM_ID, " +
            " MEDIA_HASH, MEDIA_TYPE, X, Y, Z, LAYOUT_INFO, " +
            " SCALE_X, SCALE_Y, ACTION_TYPE, ACTION_DATA) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        try {
            stmt.setInt(1, sceneId);

            for (FurniData f : furni) {
                stmt.setShort(2, f.id);
                stmt.setByte(3, f.itemType);
                stmt.setInt(4, f.itemId);
                stmt.setBytes(5, flattenMediaDesc(f.media));
                stmt.setByte(6, f.media.mimeType);
                stmt.setFloat(7, f.loc.x);
                stmt.setFloat(8, f.loc.y);
                stmt.setFloat(9, f.loc.z);
                stmt.setByte(10, f.layoutInfo);
                stmt.setFloat(11, f.scaleX);
                stmt.setFloat(12, f.scaleY);
                stmt.setByte(13, f.actionType);
                stmt.setString(14, f.actionData);
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
                stmt.setShort(2, f.id);
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
     * Creates a {@link MediaDesc} of the appropriate type based on the supplied hash and mime
     * type. The hash should previously have been created by calling {@link #flattenMediaDesc} on a
     * media descriptor.
     */
    protected MediaDesc createMediaDesc (byte[] mediaHash, byte mimeType)
    {
        if (mediaHash.length == 4) {
            byte itemType = (byte)ByteBuffer.wrap(mediaHash).asIntBuffer().get();
            return Item.getDefaultFurniMediaFor(itemType);
        } else {
            return new MediaDesc(mediaHash, mimeType);
        }
    }

    /**
     * Flattens the supplied {@link MediaDesc} into bytes that can later be
     * decoded by {@link #createMediaDesc} into the appropriate type of
     * descriptor.
     */
    protected byte[] flattenMediaDesc (MediaDesc desc)
    {
        if (desc instanceof StaticMediaDesc) {
            StaticMediaDesc sdesc = (StaticMediaDesc)desc;

            // sanity check; if we later need to flatten other static types than furni, we can have
            // the type constant map to an integer and stuff that into the byte array as well
            if (!sdesc.getMediaType().equals(Item.FURNI_MEDIA)) {
                throw new IllegalArgumentException(
                    "Cannot flatten non-furni static media " + desc + ".");
            }

            ByteBuffer data = ByteBuffer.allocate(4);
            data.asIntBuffer().put(sdesc.getItemType());
            return data.array();

        } else {
            return desc.hash;
        }
    }

    /**
     * Create the database tables and populate them with some starter scenes.
     */
    protected void createAndPopulate (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "SCENES", new String[] {
            "SCENE_ID integer not null auto_increment",
            "OWNER_TYPE tinyint not null",
            "OWNER_ID integer not null",
            "VERSION integer not null",
            "NAME varchar(255) not null",
            "SCENE_TYPE tinyint not null",
            "DECOR_ID int not null",
            "DECOR_MEDIA_HASH tinyblob not null",
            "DECOR_MEDIA_TYPE tinyint not null",
            "DEPTH integer not null",
            "WIDTH integer not null",
            "HEIGHT integer not null",
            "HORIZON float not null",
            "ENTRANCE_X float not null",
            "ENTRANCE_Y float not null",
            "ENTRANCE_Z float not null",
            "primary key (SCENE_ID)",
            "index (OWNER_ID)" }, "");

        JDBCUtil.createTableIfMissing(conn, "FURNI", new String[] {
            "SCENE_ID integer not null",
            "FURNI_ID smallint not null",
            "ITEM_TYPE tinyint not null",
            "ITEM_ID integer not null",
            "MEDIA_HASH tinyblob not null",
            "MEDIA_TYPE tinyint not null",
            "X float not null",
            "Y float not null",
            "Z float not null",
            "LAYOUT_INFO tinyint not null",
            "SCALE_X float not null",
            "SCALE_Y float not null",
            "ACTION_TYPE tinyint not null",
            "ACTION_DATA varchar(255)",
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
        // TODO: this probably doesn't work well without decor. all this needs
        // cleaning up once we've migrated background furniture over.
        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = sceneId;
        model.version = 1;
        model.name = "SampleScene" + sceneId;

        FurniData portal = new FurniData();
        portal.id = 51;
        portal.actionType = FurniData.ACTION_PORTAL;
        FurniData p2;
        FurniData furn;

        if (sceneId == 1) {
            // crayon room
            model.decorData.type = Decor.IMAGE_OVERLAY;
            model.decorData.width = 1600;

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

            furn = new FurniData();
            furn.id = 6;
            furn.media = new MediaDesc( // crayon room
                "b3084c929b49cce36a6708fb8f47a45c59e1d400.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 2) {
            // alley
            model.decorData.type = Decor.IMAGE_OVERLAY; 

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

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // alley
                "13fd51be845d51b1571424cf459ce4fd78472ec2.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaDesc( // boll weevil
                "71a3c968012324a387179f2e17ba8f1a5d2c685d.mp3");
            furn.loc = new MsoyLocation(0, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 3) {
            // cliff
            model.decorData.type = Decor.IMAGE_OVERLAY; 
            model.decorData.width = 800;

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

            furn = new FurniData();
            furn.id = 3;
            furn.media = new MediaDesc( // cliff background
                "974259e79d58c34beffe67fb781832183309fe57.swf");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 4) {
            // fans
            model.decorData.type = Decor.IMAGE_OVERLAY;
            model.decorData.width = 800;

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

            furn = new FurniData();
            furn.id = 6;
            furn.media = new MediaDesc( // fancy room
                "95101b275b607c5c02a8a411a09082ef2e9b98a7.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 5) {
            // faucet
            model.decorData.type = Decor.IMAGE_OVERLAY;
            model.decorData.width = 1600;

            portal.loc = new MsoyLocation(.3125, .71, 0, 0);
            portal.actionData = "1:54";
            portal.media = new MediaDesc( // pipe
                "d63cfbf63645d168094119a784e2e5f780d4218d.png");

            furn = new FurniData();
            furn.id = 1;
            furn.media = new MediaDesc( // faucet forest
                "05164b5141659e18687bea9e7dbd781833cbf28c.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 6) {
            // comic
            model.decorData.type = Decor.IMAGE_OVERLAY;
            model.decorData.width = 1600;

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

            furn = new FurniData();
            furn.id = 2;
            furn.media = new MediaDesc( // comic room
                "3b9a430a4d2fe6473b2ab71251162a2494843772.png");
            furn.loc = new MsoyLocation(.5, 0, 0, 0);
            furn.actionType = FurniData.BACKGROUND;
            model.addFurni(furn);

        } else if (sceneId == 7) {
            // game room background
            model.decorData.width = 800;

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
    protected SceneUpdateMarshaller _updateMarshaller =
        new SceneUpdateMarshaller(
            // register the update classes
            // (DO NOT CHANGE ORDER! see note in SceneUpdateMarshaller const.)
            ModifyFurniUpdate.class,
            null,                           // previously: ModifyPortalsUpdate
            SceneAttrsUpdate.class
            // end of update class registration (DO NOT CHANGE ORDER)
        );

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
