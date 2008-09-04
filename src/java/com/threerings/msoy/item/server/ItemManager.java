//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.depot.DatabaseException;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.RootDObjectManager;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.server.MsoyGameRegistry;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerMessages;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.admin.data.ServerConfigObject;

import com.threerings.msoy.peer.server.GameNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.web.data.ServiceException;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Prize;

import com.threerings.msoy.item.data.ItemCodes;

// we'll avoid import verbosity in this rare case
import com.threerings.msoy.item.server.persist.*;

import com.threerings.msoy.item.server.ItemLogic.LookupList;
import com.threerings.msoy.item.server.ItemLogic.MissingRepositoryException;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.server.RoomManager;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
@Singleton @EventThread
public class ItemManager
    implements ItemProvider
{
    @Inject public ItemManager (InvocationManager invmgr)
    {
        // register our invocation service
        invmgr.registerDispatcher(new ItemDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Initializes the item manager.
     */
    public void init ()
    {
        _itemLogic.init();

        ItemRepository.setNewAndHotDropoffDays(RuntimeConfig.server.newAndHotDropoffDays);

        RuntimeConfig.server.addListener(new AttributeChangeListener() {
            public void attributeChanged (AttributeChangedEvent event) {
                if (ServerConfigObject.NEW_AND_HOT_DROPOFF_DAYS.equals(event.getName())) {
                    final int days = event.getIntValue();
                    _invoker.postUnit(new Invoker.Unit("updateNewAndHotDropoffDays") {
                        public boolean invoke () {
                            ItemRepository.setNewAndHotDropoffDays(days);
                            return false;
                        }
                    });
                }
            }
        });
    }

    /**
     * Provides a reference to the {@link GameRepository} which is used for nefarious ToyBox
     * purposes.
     */
    public GameRepository getGameRepository ()
    {
        return _itemLogic.getGameRepository();
    }

    /**
     * Provides a reference to the {@link PetRepository} which is used to load pets into rooms.
     */
    public PetRepository getPetRepository ()
    {
        return _itemLogic.getPetRepository();
    }

    /**
     * Provides a reference to the {@link AvatarRepository} which is used to load pets into rooms.
     */
    public AvatarRepository getAvatarRepository ()
    {
        return _itemLogic.getAvatarRepository();
    }

    /**
     * Provides a reference to the {@link DecorRepository} which is used to load room decor.
     */
    public DecorRepository getDecorRepository ()
    {
        return _itemLogic.getDecorRepository();
    }

    /**
     * Provides a reference to the {@link TrophySourceRepository}.
     */
    public TrophySourceRepository getTrophySourceRepository ()
    {
        return _itemLogic.getTrophySourceRepository();
    }

    /**
     * TODO: This is a blocking call. Get rid of this and replace
     * calls to this method with calls to ItemLogic.
     */
    public ItemRepository<ItemRecord> getRepository (byte itemType)
        throws ServiceException
    {
        return _itemLogic.getRepository(itemType);
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord> getRepository (ItemIdent ident, ResultListener<?> rl)
    {
        return getRepository(ident.type, rl);
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord> getRepository (byte type, ResultListener<?> rl)
    {
        try {
            return _itemLogic.getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            rl.requestFailed(mre);
            return null;
        }
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord> getRepository (
        byte type, InvocationService.InvocationListener lner)
    {
        try {
            return _itemLogic.getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            lner.requestFailed(ItemCodes.E_INTERNAL_ERROR);
            return null;
        }
    }

    /**
     * Get the specified item.
     */
    public void getItem (final ItemIdent ident, ResultListener<Item> lner)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }
        _invoker.postUnit(new RepositoryListenerUnit<Item>("getItem", lner) {
            public Item invokePersistResult () throws Exception {
                ItemRecord rec = repo.loadItem(ident.itemId);
                if (rec == null) {
                    throw new InvocationException(ItemCodes.E_NO_SUCH_ITEM);
                }
                return rec.toItem();
            }
        });
    }

    /**
     * Mass-load the specified items. If any type is invalid, none are returned. If specific
     * itemIds are invalid, they are omitted from the result list.
     */
    public void getItems (Collection<ItemIdent> ids, ResultListener<List<Item>> lner)
    {
        final LookupList list = _itemLogic.new LookupList();
        try {
            for (ItemIdent ident : ids) {
                list.addItem(ident);
            }

        } catch (MissingRepositoryException mre) {
            lner.requestFailed(mre);
            return;
        }

        // do it all at once
        _invoker.postUnit(new RepositoryListenerUnit<List<Item>>("getItems", lner) {
            public List<Item> invokePersistResult () throws Exception {
                // create a list to hold the results
                List<Item> items = Lists.newArrayList();
                // mass-lookup items, a repo at a time
                for (Tuple<ItemRepository<ItemRecord>, int[]> tup : list) {
                    for (ItemRecord rec : tup.left.loadItems(tup.right)) {
                        items.add(rec.toItem());
                    }
                }
                return items;
            }
        });
    }

    public void loadItemList (final int listId, ResultListener<List<Item>> lner)
    {
        _invoker.postUnit(new RepositoryListenerUnit<List<Item>>("loadItemList", lner) {
            public List<Item> invokePersistResult () throws Exception {
                return _itemLogic.loadItemList(listId);
            }
        });
    }

    /**
     * Awards the specified prize to the specified member.
     */
    public void awardPrize (final int memberId, final int gameId, final String gameName,
                            final Prize prize, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(prize.targetType, listener);
        _invoker.postUnit(new RepositoryListenerUnit<Item>("awardPrize", listener) {
            public Item invokePersistResult () throws Exception {
                CatalogRecord listing = repo.loadListing(prize.targetCatalogId, true);
                if (listing == null) {
                    throw new InvocationException(ItemCodes.E_NO_SUCH_ITEM);
                }
                if (listing.item.creatorId != prize.creatorId) {
                    throw new InvocationException(MsoyGameCodes.E_PRIZE_CREATOR_MISMATCH);
                }
                log.info("Awarding prize " + listing + " to " + memberId + ".");
                Item item = repo.insertClone(listing.item, memberId, 0, 0).toItem();
                _eventLog.prizeEarned(memberId, gameId, prize.ident, prize.targetType);
                return item;
            }

            public void handleSuccess () {
                super.handleSuccess();

// TODO: post to their feed
//                 // send them a mail message as well
//                 String subject = _serverMsgs.getBundle("server").get(
//                     "m.got_prize_subject", _result.name);
//                 String body = _serverMsgs.getBundle("server").get("m.got_prize_body");
//                 _mailMan.deliverMessage(
//                     // TODO: sender should be special system id
//                     memberId, memberId, subject, body, new GameAwardPayload(
//                         gameId, gameName, GameAwardPayload.PRIZE,
//                         _result.name, _result.getThumbnailMedia()),
//                     true, new ResultListener.NOOP<Void>());
            }
        });
    }

    /**
     * Helper function: updates usage of avatar items.  This method assumes that the specified
     * items are both valid and owned by the user in question. The supplied listener will be
     * notified of success with null.
     *
     * @see #updateItemUsage(byte, byte, int, int, int, int, ResultListener<Object>)
     */
    public void updateItemUsage (int memberId, Avatar oldAvatar, Avatar newAvatar,
                                 ResultListener<Object> lner)
    {
        updateItemUsage(Item.AVATAR, Item.USED_AS_AVATAR, memberId, memberId,
                        (oldAvatar != null) ? oldAvatar.itemId : 0,
                        (newAvatar != null) ? newAvatar.itemId : 0, lner);
    }

    /**
     * Update usage of any items. Old item will be marked as unused, and new item will be
     * marked with the itemUseType id.
     *
     * This method assumes that the specified items are both valid and owned by the user in
     * question. The supplied listener will be notified of success with null.
     */
    public void updateItemUsage (
        final byte itemType, final byte itemUseType, final int memberId, final int locationId,
        final int oldItemId, final int newItemId, ResultListener<Object> lner)
    {
        if (oldItemId == newItemId) {
            lner.requestCompleted(null); // mr. no-op
            return;
        }

        final ItemRepository<ItemRecord> repo = getRepository(itemType, lner);
        if (repo == null) {
            return; // getRepository already informed the listener about this problem
        }

        _invoker.postUnit(new RepositoryListenerUnit<Object>("updateItemUsage", lner) {
            public Object invokePersistResult () throws Exception {
                if (oldItemId != 0) {
                    repo.markItemUsage(new int[] { oldItemId }, Item.UNUSED, 0);
                }
                if (newItemId != 0) {
                    repo.markItemUsage(new int[] { newItemId }, itemUseType, locationId);
                }
                return null;
            }
        });
    }

    /**
     * Called when an avatar item is updated.
     */
    public void avatarUpdatedOnPeer (final MemberObject memObj, final int avatarId)
    {
        getItem(new ItemIdent(Item.AVATAR, avatarId), new ResultListener<Item>() {
            public void requestCompleted (Item avatar) {
                avatarUpdatedOnPeer(memObj, (Avatar) avatar);
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to resolve updated avatar. [id=" + avatarId + "]", cause);
            }
        });
    }

    /**
     * Called when an avatar item is updated.
     */
    public void avatarUpdatedOnPeer (MemberObject memObj, Avatar avatar)
    {
        memObj.startTransaction();
        try {
            boolean remove = (avatar.ownerId != memObj.getMemberId());

            // if they're wearing it, update that.
            if (avatar.equals(memObj.avatar)) {
                memObj.setAvatar(remove ? null : avatar);
                _memberMan.updateOccupantInfo(memObj);
            }

            // probably we'll update it in their cache, too.
            if (memObj.avatarCache.contains(avatar)) {
                if (remove) {
                    memObj.removeFromAvatarCache(avatar.getKey());
                } else {
                    memObj.updateAvatarCache(avatar);
                }

            } else if (remove) {
                // nothing, they don't have it in their cache and we want to remove it anyway

            } else if (memObj.avatarCache.size() < MemberObject.AVATAR_CACHE_SIZE) {
                memObj.addToAvatarCache(avatar);

            } else {
                Avatar oldest = avatar;
                for (Avatar av : memObj.avatarCache) {
                    if (oldest.lastTouched > av.lastTouched) {
                        oldest = av;
                    }
                }
                if (oldest != avatar) {
                    memObj.removeFromAvatarCache(oldest.getKey());
                    memObj.addToAvatarCache(avatar);
                }
            }
        } finally {
            memObj.commitTransaction();
        }
    }

    /**
     * Called to effect the deletion of an avatar on a member's userobject.
     */
    public void avatarDeletedOnPeer (MemberObject memObj, int avatarId)
    {
        memObj.startTransaction();
        try {
            if ((memObj.avatar != null) && (memObj.avatar.itemId == avatarId)) {
                // the user is wearing this item: delete
                memObj.setAvatar(null);
                _memberMan.updateOccupantInfo(memObj);
            }
            ItemIdent ident = new ItemIdent(Item.AVATAR, avatarId);
            if (memObj.avatarCache.containsKey(ident)) {
                memObj.removeFromAvatarCache(ident);
            }
        } finally {
            memObj.commitTransaction();
        }
    }

    /**
     * Update usage of the specified items.
     *
     * The supplied listener will be notified of success with null.
     */
    public void updateItemUsage (final int editorMemberId, final int sceneId,
                                 FurniData[] removedFurni, FurniData[] addedFurni,
                                 ResultListener<Object> lner)
    {
        final LookupList unused = _itemLogic.new LookupList();
        final LookupList scened = _itemLogic.new LookupList();

        try {
            ArrayIntSet props = null;
            if (removedFurni != null) {
                for (FurniData furni : removedFurni) {
                    // allow removal of 'props'
                    if (furni.itemType == Item.NOT_A_TYPE) {
                        if (props == null) {
                            props = new ArrayIntSet();
                        }
                        props.add(furni.id);
                        continue;
                    }
                    unused.addItem(furni.itemType, furni.itemId);
                }
            }

            if (addedFurni != null) {
                for (FurniData furni :addedFurni) {
                    if (furni.itemType == Item.NOT_A_TYPE) {
                        // it's only legal to add props that were already there
                        if (props == null || !props.contains(furni.id)) {
                            lner.requestFailed(new Exception("Furni added with invalid item " +
                                                             "source " + furni + "."));
                            return;
                        }
                        continue;
                    }
                    scened.addItem(furni.itemType, furni.itemId);
                    unused.removeItem(furni.itemType, furni.itemId);
                }
            }

        } catch (MissingRepositoryException mre) {
            lner.requestFailed(mre);
            return;
        }

        _invoker.postUnit(new RepositoryListenerUnit<Object>("updateItemsUsage", lner) {
            public Object invokePersistResult () throws Exception {
                for (Tuple<ItemRepository<ItemRecord>, int[]> tup : unused) {
                    tup.left.markItemUsage(tup.right, Item.UNUSED, 0);
                }
                for (Tuple<ItemRepository<ItemRecord>, int[]> tup : scened) {
                    tup.left.markItemUsage(tup.right, Item.USED_AS_FURNITURE, sceneId);
                }
                return null;
            }
        });
    }

    /**
     * Informs the runtime world that an item was created and inserted into the database.
     */
    public void itemCreated (ItemRecord rec)
    {
        if (rec.getType() == Item.AVATAR) {
            MemberNodeActions.avatarUpdated(rec.ownerId, rec.itemId);
        }
    }

    /**
     * Called when the user has purchased an item from the catalog, updates their runtime inventory
     * if they are online.
     */
    public void itemPurchased (Item item)
    {
        if (item.getType() == Item.AVATAR) {
            MemberNodeActions.avatarUpdated(item.ownerId, item.itemId);
        }
    }

    /**
     * Informs the runtime world that an item was updated in the database. Worn avatars will be
     * updated, someday items being used as furni or decor in rooms will also magically be updated.
     */
    public void itemUpdated (ItemRecord rec)
    {
        itemUpdated(rec, 0);
    }

    /**
     * Informs the runtime world that an item was updated in the database. Worn avatars will be
     * updated, someday items being used as furni or decor in rooms will also magically be updated.
     *
     * @param overrideMemberId an alternate memberId to process, in case the item was removed
     * from a member's inventory.
     */
    public void itemUpdated (ItemRecord rec, int overrideMemberId)
    {
        byte type = rec.getType();
        if (type == Item.AVATAR) {
            int memberId = (overrideMemberId == 0) ? rec.ownerId : overrideMemberId;
            MemberNodeActions.avatarUpdated(memberId, rec.itemId);

        } else if (type == Item.GAME) {
            _peerMan.invokeNodeAction(new GameUpdatedAction(((GameRecord) rec).gameId));
        }
    }

    /**
     * Informs the runtime world that an item was deleted from the database.
     */
    public void itemDeleted (ItemRecord record)
    {
        if (record.getType() == Item.AVATAR) {
            MemberNodeActions.avatarDeleted(record.ownerId, record.itemId);
        }
    }

    /**
     * Load at most maxCount recently-touched items from the specified user's inventory.
     */
    public void loadRecentlyTouched (
        final int memberId, byte type, final int maxCount, ResultListener<List<Item>> lner)
    {
        // locate the appropriate repo
        final ItemRepository<ItemRecord> repo = getRepository(type, lner);
        if (repo == null) {
            return;
        }

        // load ye items
        _invoker.postUnit(
            new RepositoryListenerUnit<List<Item>>("loadRecentlyTouched", lner) {
            public List<Item> invokePersistResult () throws Exception {
                List<ItemRecord> list = repo.loadRecentlyTouched(memberId, maxCount);
                List<Item> returnList = Lists.newArrayListWithExpectedSize(list.size());
                for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                    returnList.add(list.get(ii).toItem());
                }
                return returnList;
            }
        });
    }

    /**
     * Atomically sets or clears one or more flags on an item.
     * TODO: If things get really tight, this could use updatePartial() later.
     */
    public void setFlags (final ItemIdent ident, final byte mask, final byte value,
                          ResultListener<Void> lner)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        _invoker.postUnit(new RepositoryListenerUnit<Void>("setFlags", lner) {
            public Void invokePersistResult () throws Exception {
                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new DatabaseException("Can't find item [item=" + ident + "]");
                }
                item.flagged = (byte) ((item.flagged & ~mask) | value);
                repo.updateOriginalItem(item, false);
                return null;
            }
        });
    }

    /**
     * Sets or clears the 'mature' flag.
     */
    public void setMature (final ItemIdent ident, final boolean value, ResultListener<Void> lner)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        _invoker.postUnit(new RepositoryListenerUnit<Void>("setMature", lner) {
            public Void invokePersistResult () throws Exception {
                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new DatabaseException("Can't find item [item=" + ident + "]");
                }
                item.mature = value;
                repo.updateOriginalItem(item, false);
                return null;
            }
        });
    }

    // from ItemProvider
    public void getCatalogId (
        ClientObject caller, final ItemIdent ident, final InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;

        getItem(ident, new ResultAdapter<Item>(rl) {
            public void requestCompleted (Item item) {
                if ((item.ownerId == user.getMemberId()) || // if the user owns it,
                        // OR, it's not in the catalog but the user is support
                        ((item.catalogId == 0) && user.tokens.isSupport())) {
                    rl.requestProcessed(null); // send them straight through to the detail page

                } else {
                    // otherwise, redirect them to the catalog
                    rl.requestProcessed(Integer.valueOf(item.catalogId));
                }
                // do NOT call super
            }
        });
    }

    // from ItemProvider
    public void getItemNames (ClientObject caller, final ItemIdent[] idents,
                              InvocationService.ResultListener rl)
        throws InvocationException
    {
        // pull item names from repos
        _invoker.postUnit(
            new RepositoryListenerUnit<String[]>("getItemNames", new ResultAdapter<String[]>(rl)) {
            public String[] invokePersistResult () throws Exception {
                String[] itemNames = new String[idents.length];
                for (int ii = 0; ii < idents.length; ii++) {
                    ItemIdent ident = idents[ii];
                    ItemRecord rec = _itemLogic.getRepository(ident.type).loadItem(ident.itemId);
                    if (rec != null) {
                        itemNames[ii] = rec.name;
                    }
                }
                return itemNames;
            }
        });
    }

    // from ItemProvider
    public void peepItem (ClientObject caller, ItemIdent ident, InvocationService.ResultListener rl)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.isGuest()) {
            throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
        }

        getItem(ident, new ResultAdapter<Item>(rl) {
            public void requestCompleted (Item item) {
                if (item.ownerId == user.getMemberId()) {
                    super.requestCompleted(item);

                } else {
                    _listener.requestFailed(ItemCodes.E_ACCESS_DENIED);
                }
            }
        });
    }

    // from ItemProvider
    public void deleteItem (ClientObject caller, final ItemIdent ident,
                            final InvocationService.ConfirmListener cl)
        throws InvocationException
    {
        // Disabled until this feature can be further considered
        // There are security concerns with allowing any user to destroy another user's item
        /*
        final MemberObject user = (MemberObject) caller;

        getItem(ident, new ResultListener<Item>() {
            public void requestCompleted (final Item result) {
                final byte type = result.getType();
                if (type == Item.DECOR || type == Item.PET || result.used == Item.USED_AS_FURNITURE) {
                    _sceneReg.resolveScene(result.location, new SceneRegistry.ResolutionListener() {
                        public void sceneWasResolved (SceneManager scene) {
                            RoomManager room = (RoomManager)scene;
                            if ( ! room.ensureEntityControl(user, ident, "selfDestruct")) {
                                cl.requestFailed(ItemCodes.E_ACCESS_DENIED);
                                return;
                            }

                            if (type == Item.DECOR) {
                                room.reclaimDecor(user);
                            } else if (type == Item.PET) {
                                // TODO
                                //room.reclaimPet(ident, user);
                            } else {
                                room.reclaimItem(ident, user);
                            }

                            ResultListener<Void> rl = new ResultListener.NOOP<Void>();
                            _invoker.postUnit(new RepositoryListenerUnit<Void>("deleteItem", rl) {
                                public Void invokePersistResult () throws Exception {
                                    ItemRepository<ItemRecord> repo = getRepository(ident.type);
                                    repo.deleteItem(ident.itemId);
                                    cl.requestProcessed();
                                    return null;
                                }
                            });
                        }

                        public void sceneFailedToResolve (int sceneId, Exception reason) {
                            log.warning("Scene failed to resolve. [id=" + sceneId + "]", reason);
                            cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
                        }
                    });
                } else {
                    log.warning("Tried to reclaim invalid item type [type=" + ident.type +
                        ", id=" + ident.itemId + "]");
                    cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            }

            public void requestFailed (Exception cause) {
                log.warning("Failed to resolve item for deletion. [type=" + ident.type +
                    ", id=" + ident.itemId + "].");
                cl.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
        */
    }

    // from ItemProvider
    public void reclaimItem (ClientObject caller, final ItemIdent item,
                             final InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.isGuest()) {
            throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
        }

        if (item.type == Item.AVATAR) {
            log.warning("Tried to reclaim invalid item type [type=" + item.type +
                ", id=" + item.itemId + "]");
            throw new InvocationException(InvocationCodes.INTERNAL_ERROR);
        }

        getItem(item, new ResultListener<Item>() {
            public void requestCompleted (final Item result) {
                if (result.ownerId != user.getMemberId()) {
                    lner.requestFailed(ItemCodes.E_ACCESS_DENIED);
                    return;
                }
                final byte type = result.getType();
                if (type == Item.DECOR || type == Item.AUDIO ||
                    result.used == Item.USED_AS_FURNITURE) {
                    _sceneReg.resolveScene(result.location, new SceneRegistry.ResolutionListener() {
                        public void sceneWasResolved (SceneManager scmgr) {
                            if (type == Item.DECOR) {
                                ((RoomManager)scmgr).reclaimDecor(user);
                            } else if (type == Item.AUDIO) {
                                ((RoomManager)scmgr).reclaimAudio(user);
                            } else {
                                ((RoomManager)scmgr).reclaimItem(item, user);
                            }
                            lner.requestProcessed();
                        }
                        public void sceneFailedToResolve (int sceneId, Exception reason) {
                            log.warning("Scene failed to resolve. [id=" + sceneId + "]", reason);
                            lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
                        }
                    });
                } else {
                    // TODO: avatar reclamation will be possible
                    log.warning("Item to be reclaimed is neither decor nor furni " +
                                "[type=" + result.getType() + ", id=" + result.itemId + "]");
                    lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
                    return;
                }
            }
            public void requestFailed (Exception cause) {
                log.warning("Unable to retrieve item.", cause);
                lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
            }
        });
    }

    /**
     * Get a random item out of the catalog.
     *
     * @param tags limits selection to one that matches any of these tags. If omitted, selection is
     * from all catalog entries.
     */
    public void getRandomCatalogItem (
        final byte itemType, final String[] tags, ResultListener<Item> lner)
    {
        final ItemRepository<ItemRecord> repo = getRepository(itemType, lner);
        if (repo == null) {
            return;
        }

        _invoker.postUnit(new RepositoryListenerUnit<Item>("getRandomCatalogItem", lner) {
            public Item invokePersistResult () throws Exception {
                CatalogRecord record;
                if (tags == null || tags.length == 0) {
                    record = repo.pickRandomCatalogEntry();

                } else {
                    record = repo.findRandomCatalogEntryByTags(tags);
                }
                if (record == null) {
                    return null;
                }
                return record.item.toItem();
            }
        });
    }

    /** Notifies other nodes when a game record is updated. */
    protected static class GameUpdatedAction extends GameNodeAction
    {
        public GameUpdatedAction (int gameId) {
            super(gameId);
        }

        public GameUpdatedAction () {
        }

        @Override protected void execute () {
            _gameReg.gameUpdatedOnPeer(_gameId);
        }

        @Inject protected transient MsoyGameRegistry _gameReg;
    }

    // our dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected ServerMessages _serverMsgs;
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected RootDObjectManager _omgr;
    @Inject protected SceneRegistry _sceneReg;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberManager _memberMan;
    @Inject protected ItemListRepository _listRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemLogic _itemLogic;

}
