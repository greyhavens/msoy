//
// $Id$

package com.threerings.msoy.world.server.persist;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.server.persist.SceneUpdateMarshaller;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.data.all.SceneBookmarkEntry;
import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

import static com.threerings.msoy.Log.log;

/**
 * Provides scene storage services for the msoy server.
 */
public class MsoySceneRepository extends DepotRepository
    implements SceneRepository
{
    public MsoySceneRepository (PersistenceContext ctx)
        throws PersistenceException
    {
        super(ctx);

        // create our stock scenes if they are not yet created
        for (SceneRecord.Stock stock : SceneRecord.Stock.values()) {
            checkCreateStockScene(stock);
        }

        _accumulator = new UpdateAccumulator(this);
    }

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
     * Returns the total number of scenes in the repository.
     */
    public int getSceneCount ()
        throws PersistenceException
    {
        return load(CountRecord.class, new FromOverride(SceneRecord.class)).count;
    }

    /**
     * Returns the number of rooms owned by the specified member.
     */
    public int getRoomCount (int memberId)
        throws PersistenceException
    {
        Where where = new Where(
            new Logic.And(new Equals(SceneRecord.OWNER_TYPE_C, MsoySceneModel.OWNER_TYPE_MEMBER),
                          new Equals(SceneRecord.OWNER_ID_C, memberId)));
        return load(CountRecord.class, new FromOverride(SceneRecord.class), where).count;
    }

    /**
     * Retrieve a list of all the scenes that the user directly owns.
     */
    public List<SceneBookmarkEntry> getOwnedScenes (byte ownerType, int memberId)
        throws PersistenceException
    {
        List<SceneBookmarkEntry> marks = Lists.newArrayList();
        Where where = new Where(new Logic.And(new Equals(SceneRecord.OWNER_TYPE_C, ownerType),
                                              new Equals(SceneRecord.OWNER_ID_C, memberId)));
        // TODO: use a @Computed record?
        for (SceneRecord scene : findAll(SceneRecord.class, where)) {
            marks.add(new SceneBookmarkEntry(scene.sceneId, scene.name, 0L));
        }
        return marks;
    }

    /**
     * Retrieve a list of all the member scenes that the user directly owns.
     */
    public List<SceneBookmarkEntry> getOwnedScenes (int memberId)
        throws PersistenceException
    {
        return getOwnedScenes(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    /**
     * Given a list of scene ids, return a map containing the current names, indexed by scene id.
     */
    public IntMap<String> identifyScenes (Set<Integer> sceneIds)
        throws PersistenceException
    {
        IntMap<String> names = IntMaps.newHashIntMap();
        // TODO: use a @Computed record?
        Where where = new Where(new In(SceneRecord.SCENE_ID_C, sceneIds));
        for (SceneRecord scene : findAll(SceneRecord.class, where)) {
            names.put(scene.sceneId, scene.name);
        }
        return names;
    }

    // from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
        throws PersistenceException
    {
        // ensure that the update has been applied
        if (model.version != update.getSceneVersion() + update.getVersionIncrement()) {
            log.warning("Refusing to apply update " + update +
                        ", wrong version " + model.version + ".");
            return;
        }
        // now pass it to the accumulator who will take it from here
        _accumulator.add(update);
    }

    // from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        return new UpdateList(); // we don't do scene updates
    }

    // from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        if (scene == null) {
            throw new NoSuchSceneException(sceneId);
        }
        MsoySceneModel model = scene.toSceneModel();

        // load up all of our furni data
        List<FurniData> flist = Lists.newArrayList();
        Where where = new Where(SceneFurniRecord.SCENE_ID_C, sceneId);
        for (SceneFurniRecord furni : findAll(SceneFurniRecord.class, where)) {
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
        if (model.decor.itemId == 0) { // still the default?
            // the scene specified no or an invalid decor, just load up the default
            model.decor = MsoySceneModel.defaultMsoySceneModelDecor();
        }

        return model;
    }

    /**
     * Load the SceneRecord for the given sceneId
     */
    public SceneRecord loadScene (int sceneId)
        throws PersistenceException
    {
        return load(SceneRecord.class, new Where(SceneRecord.SCENE_ID_C, sceneId));
    }

    /**
     * Load the SceneRecords for the given sceneIds.
     */
    public List<SceneRecord> loadScenes (Set<Integer> sceneIds)
        throws PersistenceException
    {
        if (sceneIds.size() == 0) {
            return Collections.emptyList();
        } else {
            return findAll(SceneRecord.class, new Where(new In(SceneRecord.SCENE_ID_C, sceneIds)));
        }
    }

    /**
     * Saves the specified update to the database.
     */
    protected void persistUpdate (SceneUpdate update)
        throws PersistenceException
    {
        if (update instanceof ModifyFurniUpdate) {
            applyFurniUpdate((ModifyFurniUpdate) update);
        } else if (update instanceof SceneAttrsUpdate) {
            applySceneAttrsUpdate((SceneAttrsUpdate) update);
        } else {
            log.warning("Requested to apply unknown update " + update + ".");
        }

        // update the scene version (which will already be the new version because the update has
        // been applied)
        int newVersion = update.getSceneVersion() + update.getVersionIncrement();
        updatePartial(SceneRecord.class, update.getSceneId(), SceneRecord.VERSION, newVersion);
        log.info("Updated version of " + update.getSceneId() + " to " + newVersion + ".");
    }

    /**
     * Apply a furniture changing update.
     */
    protected void applyFurniUpdate (ModifyFurniUpdate update)
        throws PersistenceException
    {
        if (update.furniRemoved != null) {
            for (FurniData data : update.furniRemoved) {
                delete(SceneFurniRecord.class,
                       SceneFurniRecord.getKey(update.getSceneId(), data.id));
            }
        }
        if (update.furniAdded != null) {
            for (FurniData data : update.furniAdded) {
                insert(new SceneFurniRecord(update.getSceneId(), data));
            }
        }
    }

    /**
     * Apply an update that changes the basic scene attributes.
     */
    protected void applySceneAttrsUpdate (SceneAttrsUpdate update)
        throws PersistenceException
    {
        updatePartial(
            SceneRecord.class, update.getSceneId(),
            SceneRecord.NAME, update.name,
            SceneRecord.ACCESS_CONTROL, update.accessControl,
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
     * Create a new blank room for the specified member.
     *
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (byte ownerType, int ownerId, String roomName, String portalAction,
                                boolean firstTime)
        throws PersistenceException
    {
        // determine the scene id to clone
        SceneRecord.Stock stock = null;
        switch (ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            stock = firstTime ? SceneRecord.Stock.FIRST_MEMBER_ROOM :
                SceneRecord.Stock.EXTRA_MEMBER_ROOM;
            break;
        case MsoySceneModel.OWNER_TYPE_GROUP:
            stock = firstTime ? SceneRecord.Stock.FIRST_MEMBER_ROOM :
                SceneRecord.Stock.EXTRA_MEMBER_ROOM;
            break;
        }

        // load up the stock scene
        SceneRecord record = null;
        if (stock != null) {
            record = load(SceneRecord.class, SceneRecord.getKey(stock.getSceneId()));
        }

        // if we fail to load a stock scene, just create a totally blank scene
        if (record == null) {
            log.info("Unable to find stock scene to clone [type=" + ownerType + "].");
            MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
            model.ownerType = ownerType;
            model.ownerId = ownerId;
            model.version = 1;
            model.name = roomName;
            return insertScene(model);
        }

        // fill in our new bits and write out our new scene
        record.accessControl = MsoySceneModel.ACCESS_EVERYONE;
        record.ownerType = ownerType;
        record.ownerId = ownerId;
        record.name = roomName;
        record.version = 1;
        record.sceneId = 0;
        insert(record);

        // now load up furni from the stock scene
        Where where = new Where(SceneFurniRecord.SCENE_ID_C, stock.getSceneId());
        for (SceneFurniRecord furni : findAll(SceneFurniRecord.class, where)) {
            furni.sceneId = record.sceneId;
            // if the scene has a portal pointing to the default public space; rewrite it to point
            // to our specified new portal destination (if we have one)
            if (portalAction != null && furni.actionType == FurniData.ACTION_PORTAL &&
                furni.actionData != null && furni.actionData.startsWith(
                    SceneRecord.Stock.PUBLIC_ROOM.getSceneId() + ":")) {
                furni.actionData = portalAction;
            }
            insert(furni);
        }

        return record.sceneId;
    }

    public void transferSceneOwnership (int sceneId, byte ownerType, int ownerId) 
        throws PersistenceException
    {
        updatePartial(SceneRecord.class, sceneId, SceneRecord.OWNER_TYPE, ownerType,
                      SceneRecord.OWNER_ID, ownerId);
    }

    /**
     * Insert a new scene, with furni and all, into the database and return the newly assigned
     * sceneId.
     */
    protected int insertScene (MsoySceneModel model)
        throws PersistenceException
    {
        SceneRecord scene = new SceneRecord(model);
        insert(scene);
        for (FurniData data : model.furnis) {
            insert(new SceneFurniRecord(scene.sceneId, data));
        }
        return scene.sceneId;
    }

    protected void checkCreateStockScene (SceneRecord.Stock stock)
        throws PersistenceException
    {
        // if it's already created, we're good to go
        if (load(SceneRecord.class, SceneRecord.getKey(stock.getSceneId())) != null) {
            return;
        }

        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = stock.getSceneId();
        model.version = 1;
        model.name = stock.getName();

        if (stock != SceneRecord.Stock.PUBLIC_ROOM) {
            FurniData f = new FurniData();
            f.id = 1;
            f.media = new MediaDesc("e8b660ec5aa0aa30dab46b267daf3b80996269e7.swf");
            f.loc = new MsoyLocation(1, 0, 0.5, 0);
            f.scaleX = 1.4f;
            f.actionType = FurniData.ACTION_PORTAL;
            f.actionData = SceneRecord.Stock.PUBLIC_ROOM.getSceneId() + ":" +
                SceneRecord.Stock.PUBLIC_ROOM.getName();
            model.addFurni(f);
        }

        log.info("Creating stock scene " + stock + ".");
        insertScene(model);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SceneRecord.class);
        classes.add(SceneFurniRecord.class);
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

    /** Utility class that compresses related scene updates. */
    protected UpdateAccumulator _accumulator;
    
    /** Internal reference to the decor repository, used to load up decor for each scene. */
    protected DecorRepository _decorRepo;

    /** The maximum number of updates to store for each scene. */
    protected static final int MAX_UPDATES_PER_SCENE = 16;
}
