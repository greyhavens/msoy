//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.sql.Timestamp;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DataMigration;
import com.samskivert.jdbc.depot.DatabaseException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.EpochSeconds;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.expression.ValueExp;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.util.Name;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.HotnessConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

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

        _ratingRepo = new RatingRepository(ctx, SceneRecord.RATING, SceneRecord.RATING_COUNT) {
            @Override
            protected Class<? extends PersistentRecord> getTargetClass () {
                return SceneRecord.class;
            }
            @Override
            protected Class<RatingRecord> getRatingClass () {
                return coerceRating(SceneRatingRecord.class);
            }
        };

        // Remove broken "Decorate Tutorial" furnis from 20,000 old home rooms
        registerMigration(new DataMigration("2008_10_27_remove_decorate_tutorial_furnis") {
            @Override public void invoke () throws DatabaseException {
                deleteAll(SceneFurniRecord.class, new Where(SceneFurniRecord.ITEM_ID_C, 383));
            }
        });
    }

    /**
     * Returns the total number of scenes in the repository.
     */
    public int getSceneCount ()
    {
        return load(CountRecord.class, new FromOverride(SceneRecord.class)).count;
    }

    /**
     * Returns the number of rooms owned by the specified member.
     */
    public int getRoomCount (int memberId)
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
    {
        Where where = new Where(new Logic.And(new Equals(SceneRecord.OWNER_TYPE_C, ownerType),
                                              new Equals(SceneRecord.OWNER_ID_C, memberId)));
        return findAll(SceneRecord.class, where);
    }

    /**
     * Retrieve a list of all the member scenes that the user directly owns.
     */
    public List<SceneRecord> getOwnedScenes (int memberId)
    {
        return getOwnedScenes(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    public RatingRepository getRatingRepository ()
    {
        return _ratingRepo;
    }

    /**
     * Return the scene name for the specified id, or null (no exception) if the scene
     * doesn't exist.
     */
    public String identifyScene (int sceneId)
    {
        // TODO: use a @Computed record?
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        return (scene == null) ? null : scene.name;
    }

    /**
     * Publishes a scene, marking it as a Whirled Tourist trap.
     */
    public void publishScene (int sceneId)
    {
        updatePartial(SceneRecord.class, sceneId,
            SceneRecord.LAST_UPDATED, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Given a list of scene ids, return a map containing the current names, indexed by scene id.
     */
    public IntMap<String> identifyScenes (Set<Integer> sceneIds)
    {
        IntMap<String> names = IntMaps.newHashIntMap();
        // TODO: use a @Computed record?
        for (SceneRecord scene : loadAll(SceneRecord.class, sceneIds)) {
            names.put(scene.sceneId, scene.name);
        }
        return names;
    }

    // from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
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
        throws NoSuchSceneException
    {
        return new UpdateList(); // we don't do scene updates
    }

    // from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
        throws NoSuchSceneException
    {
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        if (scene == null) {
            throw new NoSuchSceneException(sceneId);
        }
        MsoySceneModel model = scene.toSceneModel();

        // populate the name of the owner
        switch (model.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            model.ownerName = _memberRepo.loadMemberName(model.ownerId);
            break;

        case MsoySceneModel.OWNER_TYPE_GROUP:
            GroupRecord grec = _groupRepo.loadGroup(model.ownerId);
            if (grec == null) {
                model.ownerName = new Name("");
                model.gameId = 0;
            } else {
                model.ownerName = new Name(grec.name);
                model.gameId = grec.gameId;
            }
            break;

        default:
            log.warning("Unable to populate owner name, unknown ownership type", new Exception());
            break;
        }

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
    {
        return load(SceneRecord.class, new Where(SceneRecord.SCENE_ID_C, sceneId));
    }

    /**
     * Load the SceneRecords for the given sceneIds.
     */
    public List<SceneRecord> loadScenes (Collection<Integer> sceneIds)
    {
        return loadAll(SceneRecord.class, sceneIds);
    }

    /**
     * Loads "new and hot" rooms...
     */
    public List<SceneRecord> loadScenes (int offset, int rows)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Limit(offset, rows));

        List<SQLExpression> exprs = Lists.newArrayList();
        List<OrderBy.Order> orders = Lists.newArrayList();

        // only load completely public rooms
        clauses.add(new Where(SceneRecord.ACCESS_CONTROL_C, MsoySceneModel.ACCESS_EVERYONE));

        // TODO: Add more sorting options
        long nowSeconds = System.currentTimeMillis() / 1000;
        exprs.add(new Arithmetic.Sub(SceneRecord.RATING_C,
            new Arithmetic.Div(
                new Arithmetic.Sub(new ValueExp(nowSeconds),
                    new EpochSeconds(SceneRecord.LAST_UPDATED_C)),
                _hconfig.getDropoffSeconds())));
        orders.add(OrderBy.Order.DESC);

        clauses.add(new OrderBy(exprs.toArray(new SQLExpression[exprs.size()]),
                                orders.toArray(new OrderBy.Order[orders.size()])));

        return findAll(SceneRecord.class, clauses);
    }

    /**
     * Returns the canonical snapshot image for the specified scene or null if it has none.
     */
    public MediaDesc loadSceneSnapshot (int sceneId)
    {
        SceneRecord scene = loadScene(sceneId);
        return (scene == null) ? null : scene.getSnapshot();
    }

    /**
     * Saves the specified update to the database.
     */
    protected void persistUpdate (SceneUpdate update)
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
            } catch (Exception e) {
                log.warning("Failed to apply scene update " + update +
                        " from " + StringUtil.toString(updates) + ".", e);
            }
        }
        if (sceneId != 0) {
            try {
                updatePartial(SceneRecord.class, sceneId,
                    SceneRecord.VERSION, finalVersion);
            } catch (Exception e) {
                log.warning("Failed to update scene to final version", "id", sceneId,
                        "fvers", finalVersion, e);
            }
        }
    }

    /**
     * Applies an update that adds, removes or changes furni.
     */
    protected void applyUpdate (SceneUpdate update)
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
            log.warning("Unable to apply unknown furni update", "class", update.getClass(),
                        "update", update);
        }
    }

    /**
     * Creates a new blank room for the specified member.
     *
     * @param ownerType may be an individual member or a group.
     * @param portalAction to where to link the new room's door.
     * @param firstTime whether this the first room this owner has created.
     *
     * @return the scene id of the newly created room.
     */
    public int createBlankRoom (byte ownerType, int ownerId, String roomName, String portalAction,
                                boolean firstTime)
    {
        // determine the scene id to clone
        SceneRecord.Stock stock = null;
        switch (ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            stock = firstTime ? SceneRecord.Stock.FIRST_MEMBER_ROOM :
                SceneRecord.Stock.EXTRA_MEMBER_ROOM;
            break;
        case MsoySceneModel.OWNER_TYPE_GROUP:
            stock = firstTime ? SceneRecord.Stock.FIRST_GROUP_HALL :
                 SceneRecord.Stock.EXTRA_GROUP_HALL;
            break;
        }

        // load up the stock scene
        SceneRecord record = null;
        if (stock != null) {
            record = load(SceneRecord.class, SceneRecord.getKey(stock.getSceneId()));
        }

        // if we fail to load a stock scene, just create a totally blank scene
        if (record == null) {
            log.info("Unable to find stock scene to clone", "type", ownerType);
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
    {
        updatePartial(SceneRecord.class, sceneId, SceneRecord.OWNER_TYPE, ownerType,
                      SceneRecord.OWNER_ID, ownerId);
    }

    /** Loads the room properties. */
    public List<RoomPropertyRecord> loadProperties (int ownerId, int sceneId)
    {
        return findAll(RoomPropertyRecord.class, new Where(
            RoomPropertyRecord.OWNER_ID_C, ownerId,
            RoomPropertyRecord.SCENE_ID_C, sceneId));
    }

    /** Saves a room property, deleting if the value is null. */
    public void storeProperty (RoomPropertyRecord record)
    {
        if (record.value == null) {
            delete(record);
        } else {
            store(record);
        }
    }

    public void setCanonicalImage (int sceneId, byte[] canonicalHash, byte canonicalType,
        byte[] thumbnailHash, byte thumbnailType)
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
    {
        SceneRecord scene = new SceneRecord(model);
        insert(scene);
        for (FurniData data : model.furnis) {
            insert(new SceneFurniRecord(scene.sceneId, data));
        }
        return scene.sceneId;
    }

    protected void checkCreateStockScene (SceneRecord.Stock stock)
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
    protected void init ()
    {
        super.init();

        // create our stock scenes if they are not yet created
        for (SceneRecord.Stock stock : SceneRecord.Stock.values()) {
            checkCreateStockScene(stock);
        }

        // initialize our update accumulator
        _accumulator.init(this);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SceneRecord.class);
        classes.add(SceneFurniRecord.class);
        classes.add(RoomPropertyRecord.class);
        classes.add(SceneRatingRecord.class);
    }

    protected RatingRepository _ratingRepo;

    // dependencies
    @Inject protected UpdateAccumulator _accumulator;
    @Inject protected DecorRepository _decorRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected HotnessConfig _hconfig;
}
