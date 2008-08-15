//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.persist.CountRecord;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.SceneAttrsUpdate;

import static com.threerings.msoy.Log.log;

/**
 * Provides scene storage services for the msoy server.
 */
@Singleton @BlockingThread
public class MsoySceneRepository extends DepotRepository
    implements SceneRepository
{
    @Inject public MsoySceneRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Creates our default scenes and starts up our accumulator.
     */
    public void init ()
        throws PersistenceException
    {
        // create our stock scenes if they are not yet created
        for (SceneRecord.Stock stock : SceneRecord.Stock.values()) {
            checkCreateStockScene(stock);
        }

        // initialize our update accumulator
        _accumulator.init(this);
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
     * Retrieve a list of all the scenes that the user owns.
     */
    public List<SceneRecord> getOwnedScenes (byte ownerType, int memberId)
        throws PersistenceException
    {
        Where where = new Where(new Logic.And(new Equals(SceneRecord.OWNER_TYPE_C, ownerType),
                                              new Equals(SceneRecord.OWNER_ID_C, memberId)));
        return findAll(SceneRecord.class, where);
    }

    /**
     * Retrieve a list of all the member scenes that the user directly owns.
     */
    public List<SceneRecord> getOwnedScenes (int memberId)
        throws PersistenceException
    {
        return getOwnedScenes(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    /**
     * Return the scene name for the specified id, or null (no exception) if the scene
     * doesn't exist.
     */
    public String identifyScene (int sceneId)
        throws PersistenceException
    {
        // TODO: use a @Computed record?
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        return (scene == null) ? null : scene.name;
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
        int targetVers = update.getSceneVersion() + update.getVersionIncrement();
        if (model.version != targetVers) {
            log.warning("Refusing to apply update, wrong version [want=" + model.version +
                        ", have=" + targetVers + ", update=" + update + "].");
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
        int finalVersion = update.getSceneVersion() + update.getVersionIncrement();
        persistUpdates(Collections.singleton(update), finalVersion);
    }

    /**
     * Saves the provided set of updates to the database. Errors applying any of the individual
     * updates are caught and logged so that one update application failure does not prevent
     * subsequent updates from failing.
     */
    protected void persistUpdates (Iterable<? extends SceneUpdate> updates, int finalVersion)
    {
        int sceneId = 0;
        for (SceneUpdate update : updates) {
            sceneId = update.getSceneId();
            try {
                applyUpdate(update);
            } catch (PersistenceException pe) {
                log.warning("Failed to apply scene update " + update +
                        " from " + StringUtil.toString(updates) + ".", pe);
            }
        }
        if (sceneId != 0) {
            try {
                updatePartial(SceneRecord.class, sceneId, SceneRecord.VERSION, finalVersion);
            } catch (PersistenceException pe) {
                log.warning("Failed to update scene to final version [id=" + sceneId +
                        ", fvers=" + finalVersion + "].", pe);
            }
        }
    }

    /**
     * Applies an update that adds, removes or changes furni.
     */
    protected void applyUpdate (SceneUpdate update)
        throws PersistenceException
    {
        if (update instanceof FurniUpdate.Add) {
            insert(new SceneFurniRecord(update.getSceneId(), ((FurniUpdate)update).data));

        } else if (update instanceof FurniUpdate.Change) {
            update(new SceneFurniRecord(update.getSceneId(), ((FurniUpdate)update).data));

        } else if (update instanceof FurniUpdate.Remove) {
            delete(SceneFurniRecord.class,
                   SceneFurniRecord.getKey(update.getSceneId(), ((FurniUpdate)update).data.id));

        } else if (update instanceof SceneAttrsUpdate) {
            SceneAttrsUpdate scup = (SceneAttrsUpdate)update;
            updatePartial(
                SceneRecord.class, update.getSceneId(),
                SceneRecord.NAME, scup.name,
                SceneRecord.ACCESS_CONTROL, scup.accessControl,
                SceneRecord.DECOR_ID, scup.decor.itemId,
                SceneRecord.AUDIO_ID, scup.audioData.itemId,
                SceneRecord.AUDIO_MEDIA_HASH, SceneUtil.flattenMediaDesc(scup.audioData.media),
                SceneRecord.AUDIO_MEDIA_TYPE, scup.audioData.media.mimeType,
                SceneRecord.AUDIO_VOLUME, scup.audioData.volume,
                SceneRecord.ENTRANCE_X, scup.entrance.x,
                SceneRecord.ENTRANCE_Y, scup.entrance.y,
                SceneRecord.ENTRANCE_Z, scup.entrance.z);

        } else {
            log.warning("Unable to apply unknown furni update [class=" + update.getClass() +
                        ", update=" + update + "].");
        }
    }

    /**
     * Create a new blank room for the specified member.
     * @param ownerType May be an individual member or a group
     * @param ownerId
     * @param roomName
     * @param portalAction Where to link the new room's door to
     * @param firstTime Is this the first room this owner has created?
     * @param gameId If >= 0, add the furni representation of this game to the new room
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (byte ownerType, int ownerId, String roomName, String portalAction,
                                boolean firstTime, Game game)
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
            stock = (game != null) ? SceneRecord.Stock.GAME_GROUP_HALL :
                (firstTime ? SceneRecord.Stock.FIRST_GROUP_HALL :
                 SceneRecord.Stock.EXTRA_GROUP_HALL);
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

            // for game groups, point the appropriate furni to the right game
            if (stock == SceneRecord.Stock.GAME_GROUP_HALL &&
                (furni.actionType == FurniData.ACTION_LOBBY_GAME)) {
                final String gameAction = game.gameId + ":" + game.name;
                furni.actionData = gameAction;
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

    /** Loads the room properties. */
    public List<RoomPropertyRecord> loadProperties (int ownerId, int sceneId)
        throws PersistenceException
    {
        return findAll(RoomPropertyRecord.class, new Where(
            RoomPropertyRecord.OWNER_ID_C, ownerId,
            RoomPropertyRecord.SCENE_ID_C, sceneId));
    }

    /** Saves a room property, deleting if the value is null. */
    public void storeProperty (RoomPropertyRecord record)
        throws PersistenceException
    {
        if (record.value == null) {
            delete(record);
        } else {
            store(record);
        }
    }

    public void setCanonicalImage (int sceneId, byte[] canonicalHash, byte canonicalType,
        byte[] thumbnailHash, byte thumbnailType)
        throws PersistenceException
    {
        updatePartial(SceneRecord.class, sceneId,
            SceneRecord.CANONICAL_IMAGE_HASH, canonicalHash,
            SceneRecord.CANONICAL_IMAGE_TYPE, canonicalType,
            SceneRecord.THUMBNAIL_HASH, thumbnailHash,
            SceneRecord.THUMBNAIL_TYPE, thumbnailType);
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
        classes.add(RoomPropertyRecord.class);
    }

    /** Utility class that compresses related scene updates. */
    @Inject protected UpdateAccumulator _accumulator;

    /** Internal reference to the decor repository, used to load up decor for each scene. */
    @Inject protected DecorRepository _decorRepo;
}
