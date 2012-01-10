//
// $Id$

package com.threerings.msoy.room.server;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.samskivert.servlet.util.ServiceWaiter;

import com.samskivert.jdbc.RepositoryUnit;

import com.threerings.util.MessageBundle;

import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService.ResultListener;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneFurniRecord;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import static com.threerings.msoy.Log.log;

/**
 * Handles scene related logic.
 */
@Singleton
public class SceneLogic
    implements SceneRepository
{
    // from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
        throws PersistenceException, NoSuchSceneException
    {
        SceneRecord scene = _sceneRepo.loadScene(sceneId);
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
            if (grec != null) {
                model.ownerName = grec.toGroupName();
                model.gameId = grec.gameId;
            }
            break;

        case MsoySceneModel.OWNER_TYPE_TRANSIENT:
            // Nobody
            break;

        default:
            log.warning("Unable to populate owner name, unknown ownership type", new Exception());
            break;
        }

        List<FurniData> flist = Lists.newArrayList();
        for (SceneFurniRecord furni : _sceneRepo.loadFurni(sceneId)) {
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

    // from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
        throws PersistenceException
    {
        return new UpdateList(); // we don't do scene updates
    }

    public void flushUpdates (final int sceneId, final ResultListener listener)
    {
        // trigger a flush of any furniture updates
        _invoker.postUnit(new RepositoryUnit("flushUpdates") {
            @Override public void invokePersist () throws Exception {
                // clear out any pending updates
                _accumulator.flushUpdates(sceneId);
            }
            @Override public void handleSuccess () {
                listener.requestProcessed(null);
            }
            @Override public void handleFailure (Exception e) {
                listener.requestFailed(e.getMessage());
            }
        });
    }

    // from interface SceneRepository
    public Object loadExtras (int sceneId, SceneModel model)
        throws PersistenceException
    {
        MsoySceneModel mmodel = (MsoySceneModel) model;
        Set<ItemIdent> memoryIds = Sets.newHashSet();
        for (FurniData furni : mmodel.furnis) {
            if (furni.itemType != MsoyItemType.NOT_A_TYPE) {
                memoryIds.add(furni.getItemIdent());
            }
        }
        if (mmodel.decor != null) {
            memoryIds.add(mmodel.decor.getIdent());
        }
        RoomExtras extras = new RoomExtras();
        if (memoryIds.size() > 0) {
            extras.memories = _memoryRepo.loadMemories(memoryIds);
        }
        extras.playlist = Lists.transform(_audioRepo.loadItemsByLocation(sceneId),
            new ItemRecord.ToItem<Audio>());
        return extras;
    }

    // from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
        throws PersistenceException
    {
        // ensure that the update has been applied
        int targetVers = update.getSceneVersion() + update.getVersionIncrement();
        if (model.version != targetVers) {
            log.warning("Refusing to apply update, wrong version",
               "want", model.version, "have", targetVers, "update", update);
            return;
        }
        // now pass it to the accumulator who will take it from here
        _accumulator.add(update);
    }

    /**
     * Creates a new blank room for the specified member.
     *
     * @param ownerType may be an individual member or a group.
     * @param portalAction to where to link the new room's door.
     */
    public SceneRecord createBlankRoom (byte ownerType, int ownerId, int stockSceneId,
        boolean privileged, int themeId, String roomName, String portalAction)
    {
        // load up the stock scene
        SceneRecord record = _sceneRepo.loadScene(stockSceneId);

        // if we fail to load a stock scene, just create a totally blank scene
        if (record == null) {
            log.info("Unable to find stock scene to clone", "type", ownerType);
            MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
            model.ownerType = ownerType;
            model.ownerId = ownerId;
            model.version = 1;
            model.name = roomName;
            model.themeId = themeId;
            return _sceneRepo.insertScene(model);
        }

        // fill in our new bits and write out our new scene
        record.accessControl = MsoySceneModel.ACCESS_EVERYONE;
        record.ownerType = ownerType;
        record.ownerId = ownerId;
        record.themeGroupId = themeId;
        record.name = roomName;
        record.version = 1;
        record.sceneId = 0;
        _sceneRepo.insertScene(record);

        // now load up furni from the stock scene
        for (SceneFurniRecord furni : _sceneRepo.loadFurni(stockSceneId)) {
            furni.sceneId = record.sceneId;

            // if this is a member room with "real" furniture in it, clone the furni items
            if (furni.itemId != 0) {
                try {
                    // first load the actual item that's in the template scene
                    ItemRepository<ItemRecord> repo = _itemLogic.getRepository(furni.itemType);
                    ItemRecord stockItem = repo.loadItem(furni.itemId);
                    if (stockItem.catalogId == 0) {
                        log.warning("Unlisted item in room template; skipping",
                            "sceneId", furni.sceneId, "itemType", furni.itemType, "itemId",
                            furni.itemId);
                        continue;
                    }

                    // load its associated catalog listing
                    CatalogRecord listing = repo.loadListing(stockItem.catalogId, true);
                    if (!privileged) {
                        if (listing.basisId != 0) {
                            log.warning("Listing for item in room template is derived; skipping",
                                "sceneId", furni.sceneId, "itemType", furni.itemType, "itemId",
                                furni.itemId, "pricing", listing.pricing, "basis", listing.basisId);
                            continue;
                        }
                        if (themeId != 0 && listing.brandId != themeId) {
                            log.warning("Listing for item in room template is not owned by theme; skipping",
                                "sceneId", furni.sceneId, "itemType", furni.itemType, "itemId",
                                furni.itemId, "brand", listing.brandId);
                            continue;
                        }
                    }

                    // create a new clone, pretty much exactly as if we were buying it
                    ItemRecord clone = repo.insertClone(listing.item, ownerId, Currency.BARS, 0);
                    // in fact, log it as if we bought it
                    _itemLogic.itemPurchased(clone, Currency.BARS, 0);

                    furni.itemId = clone.itemId;

                } catch (Exception e) {
                    log.warning("Failed to create new furni clone from room template; skipping",
                        "sceneId", furni.sceneId, "itemType", furni.itemType, "itemId",
                        furni.itemId);
                    continue;
                }
            }

            // if the scene has a portal pointing to the default public space; rewrite it to point
            // to our specified new portal destination (if we have one)
            if (portalAction != null && furni.actionType == FurniData.ACTION_PORTAL &&
                furni.actionData != null && furni.actionData.startsWith(
                    SceneRecord.Stock.PUBLIC_ROOM.getSceneId() + ":")) {
                furni.actionData = portalAction;
            }
            _sceneRepo.insertFurni(furni);
        }

        return record;
    }

    public void validateAllTemplateFurni (final int groupId, final int sceneId,
        final ServiceWaiter<Void> waiter)
    {
        _invoker.postUnit(new RepositoryUnit("validateAllTemplateFurni") {
            @Override public void invokePersist () throws Exception {
                // go through the furni and make sure they're sane
                for (SceneFurniRecord rec : _sceneRepo.loadFurni(sceneId)) {
                    if (rec.itemType != MsoyItemType.NOT_A_TYPE && rec.itemId != 0) {
                        String err = validateOneTemplateFurni(
                            groupId, sceneId, rec.itemType, rec.itemId);
                        if (err != null) {
                            throw new ServiceException(err);
                        }
                    }
                }
            }
            @Override public void handleSuccess () {
                waiter.requestCompleted(null);
            }
            @Override public void handleFailure (Exception e) {
                waiter.requestFailed(e);
            }
        });
    }

    public String validateOneTemplateFurni (int themeId, int sceneId, MsoyItemType itemType, int itemId)
        throws ServiceException
    {
        // make sure the item is stamped
        if (!_itemLogic.getRepository(itemType).isThemeStamped(themeId, itemId)) {
            return furniError(RoomCodes.E_FURNI_NOT_STAMPED, itemType, itemId);
        }

        if (sceneId == 0) {
            return null;
        }

        // but if we are, we need to do sanity tests on the item
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(itemType);
        ItemRecord stockItem = repo.loadItem(itemId);
        // it has to be listed
        if (stockItem.catalogId == 0) {
            return furniError(RoomCodes.E_TEMPLATE_FURNI_NOT_LISTED, itemType, itemId);
        }

        CatalogRecord listing = repo.loadListing(stockItem.catalogId, true);
        // the listing must be brand owned (by the theme in question)
        if (listing.brandId != themeId) {
            return furniError(RoomCodes.E_TEMPLATE_LISTING_NOT_OWNED, itemType, itemId);
        }
        // and the item must not be derived from anything else
        if (listing.basisId != 0) {
            return furniError(RoomCodes.E_TEMPLATE_LISTING_DERIVED, itemType, itemId);
        }
        // else all is well
        return null;
    }

    protected String furniError (String code, MsoyItemType itemType, int itemId)
    {
        try {
            ItemRecord item = _itemLogic.getRepository(itemType).loadItem(itemId);
            if (item != null) {
                return MessageBundle.tcompose(code, item.name);
            }
        } catch (ServiceException e) {
            e.printStackTrace();
        }
        return MessageBundle.tcompose(code, itemType + ":" + itemId);
    }

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected AudioRepository _audioRepo;
    @Inject protected DecorRepository _decorRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected ThemeRepository _themeRepo;
    @Inject protected UpdateAccumulator _accumulator;
}
