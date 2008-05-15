//
// $Id$

package com.threerings.msoy.item.server;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.Predicate;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.jdbc.depot.PersistenceContext;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.whirled.server.SceneManager;
import com.threerings.whirled.server.SceneRegistry;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;

import com.threerings.msoy.peer.server.GameNodeAction;
import com.threerings.msoy.person.data.GameAwardPayload;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.server.RoomManager;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.Prize;

import com.threerings.msoy.item.data.ItemCodes;
// we'll avoid import verbosity in this rare case
import com.threerings.msoy.item.server.persist.*;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
@EventThread
public class ItemManager
    implements ItemProvider
{
    /**
     * An exception that may be thrown if an item repository doesn't exist.
     */
    public static class MissingRepositoryException extends Exception
    {
        public MissingRepositoryException (byte type)
        {
            super("No repository registered for " + type + ".");
        }
    } /* End: static class MissingRepositoryException. */

    /**
     * Initializes the item manager, which will establish database connections for all of its item
     * repositories.
     */
    public void init (PersistenceContext ctx, MsoyEventLogger eventLog)
        throws PersistenceException
    {
        _eventLog = eventLog;

        // create our various repositories
        registerRepository(Item.AUDIO, new AudioRepository(ctx));
        registerRepository(Item.AVATAR, _avatarRepo = new AvatarRepository(ctx));
        registerRepository(Item.DECOR, _decorRepo = new DecorRepository(ctx));
        registerRepository(Item.DOCUMENT, new DocumentRepository(ctx));
        registerRepository(Item.FURNITURE, new FurnitureRepository(ctx));
        registerRepository(Item.TOY, new ToyRepository(ctx));
        registerRepository(Item.GAME, _gameRepo = new GameRepository(ctx));
        registerRepository(Item.PET, _petRepo = new PetRepository(ctx));
        registerRepository(Item.PHOTO, new PhotoRepository(ctx));
        registerRepository(Item.VIDEO, new VideoRepository(ctx));
        registerRepository(Item.LEVEL_PACK, new LevelPackRepository(ctx));
        registerRepository(Item.ITEM_PACK, new ItemPackRepository(ctx));
        registerRepository(Item.TROPHY_SOURCE, _tsourceRepo = new TrophySourceRepository(ctx));
        registerRepository(Item.PRIZE, new PrizeRepository(ctx));
        registerRepository(Item.PROP, new PropRepository(ctx));
        _listRepo = new ItemListRepository(ctx);

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), MsoyCodes.WORLD_GROUP);
    }

    /**
     * Provides a reference to the {@link GameRepository} which is used for nefarious ToyBox
     * purposes.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Provides a reference to the {@link PetRepository} which is used to load pets into rooms.
     */
    public PetRepository getPetRepository ()
    {
        return _petRepo;
    }

    /**
     * Provides a reference to the {@link AvatarRepository} which is used to load pets into rooms.
     */
    public AvatarRepository getAvatarRepository ()
    {
        return _avatarRepo;
    }

    /**
     * Provides a reference to the {@link DecorRepository} which is used to load room decor.
     */
    public DecorRepository getDecorRepository ()
    {
        return _decorRepo;
    }

    /**
     * Provides a reference to the {@link TrophySourceRepository}.
     */
    public TrophySourceRepository getTrophySourceRepository ()
    {
        return _tsourceRepo;
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord, ?, ?, ?> getRepository (ItemIdent ident, ResultListener<?> rl)
    {
        return getRepository(ident.type, rl);
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord, ?, ?, ?> getRepository (byte type, ResultListener<?> rl)
    {
        try {
            return getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            rl.requestFailed(mre);
            return null;
        }
    }

    /**
     * Returns the repository used to manage items of the specified type.
     */
    public ItemRepository<ItemRecord, ?, ?, ?> getRepository (
        byte type, InvocationService.InvocationListener lner)
    {
        try {
            return getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            lner.requestFailed(ItemCodes.E_INTERNAL_ERROR);
            return null;
        }
    }

    /**
     * Returns an iterator of item types for which we have repositories.
     */
    public Iterable<Byte> getRepositoryTypes ()
    {
        return _repos.keySet();
    }

    /**
     * Returns the repository used to manage items of the specified type. Throws a service
     * exception if the supplied type is invalid.
     */
    public ItemRepository<ItemRecord, ?, ?, ?> getRepository (byte type)
        throws ServiceException
    {
        try {
            return getRepositoryFor(type);
        } catch (MissingRepositoryException mre) {
            log.warning("Requested invalid repository type " + type + ".");
            throw new ServiceException(ItemCodes.INTERNAL_ERROR);
        }
    }

    /**
     * Get the specified item.
     */
    public void getItem (final ItemIdent ident, ResultListener<Item> lner)
    {
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>("getItem", lner) {
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
        final LookupList list = new LookupList();
        try {
            for (ItemIdent ident : ids) {
                list.addItem(ident);
            }

        } catch (MissingRepositoryException mre) {
            lner.requestFailed(mre);
            return;
        }

        // do it all at once
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Item>>("getItems", lner) {
            public List<Item> invokePersistResult () throws Exception {
                // create a list to hold the results
                List<Item> items = Lists.newArrayList();
                // mass-lookup items, a repo at a time
                for (Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]> tup : list) {
                    for (ItemRecord rec : tup.left.loadItems(tup.right)) {
                        items.add(rec.toItem());
                    }
                }
                return items;
            }
        });
    }

    public List<ItemListInfo> getItemLists (int memberId)
        throws PersistenceException
    {
        if (MsoyServer.omgr.isDispatchThread()) {
            throw new IllegalStateException("Must be called from the invoker");
        }

        // load up the user's lists
        List<ItemListInfoRecord> records = _listRepo.loadInfos(memberId);

        int nn = records.size();
        List<ItemListInfo> list = Lists.newArrayListWithCapacity(nn);
        for (int ii = 0; ii < nn; ii++) {
            list.add(records.get(ii).toItemListInfo());
        }

        return list;
    }

    public ItemListInfo createItemList (int memberId, byte type, String name)
    {
        // TODO
        return null;
    }

    public void addItemToList (ItemListInfo info, Item item)
    {
        // TODO: easy addition without having to rewrite old stuff
    }

    public void loadItemList (final int listId, ResultListener<List<Item>> lner)
    {
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<Item>>("loadItemList", lner) {
            public List<Item> invokePersistResult () throws Exception {
                // first, look up the list
                ItemListInfoRecord infoRecord = _listRepo.loadInfo(listId);
                ItemIdent[] idents = _listRepo.loadList(listId);

                // now we're going to load all of these items
                LookupList lookupList = new LookupList();
                for (ItemIdent ident : idents) {
                    try {
                        lookupList.addItem(ident);
                    } catch (MissingRepositoryException mre) {
                        log.warning("Omitting bogus item from list: " + ident);
                    }
                }

                // now look up all those items
                HashMap<ItemIdent, Item> items = new HashMap<ItemIdent, Item>();
                // mass-lookup items, a repo at a time
                for (Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]> tup : lookupList) {
                    for (ItemRecord rec : tup.left.loadItems(tup.right)) {
                        Item item = rec.toItem();
                        items.put(item.getIdent(), item);
                    }
                }

                // prune any items that need pruning
                pruneItemsFromList(infoRecord, items.values());

                // then, if we're missing any items, we need to re-save the list
                if (idents.length != items.size()) {
                    List<ItemIdent> newIdents = Lists.newArrayListWithCapacity(items.size());
                    for (ItemIdent ident : idents) {
                        if (items.containsKey(ident)) {
                            newIdents.add(ident);
                        }
                    }

                    // now save the list
                    idents = new ItemIdent[newIdents.size()];
                    newIdents.toArray(idents);
                    _listRepo.saveList(listId, idents);
                }

                // finally, return all the items in list order
                List<Item> list = Lists.newArrayListWithCapacity(idents.length);
                for (ItemIdent ident : idents) {
                    list.add(items.get(ident));
                }
                return list;
            }
        });
    }

    /**
     * Awards the specified prize to the specified member.
     */
    public void awardPrize (final int memberId, final int gameId, final String gameName,
                            final Prize prize, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(prize.targetType, listener);
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>("awardPrize", listener) {
            public Item invokePersistResult () throws Exception {
                CatalogRecord<ItemRecord> listing = repo.loadListing(prize.targetCatalogId, true);
                if (listing == null) {
                    throw new InvocationException(ItemCodes.E_NO_SUCH_ITEM);
                }
                if (listing.item.creatorId != prize.creatorId) {
                    log.warning("Refusing request to award prize due to owner mismatch " +
                                "[prize=" + prize + ", item=" + listing.item + "].");
                    throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
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
//                 String subject = MsoyServer.msgMan.getBundle("server").get(
//                     "m.got_prize_subject", _result.name);
//                 String body = MsoyServer.msgMan.getBundle("server").get("m.got_prize_body");
//                 MsoyServer.mailMan.deliverMessage(
//                     // TODO: sender should be special system id
//                     memberId, memberId, subject, body, new GameAwardPayload(
//                         gameId, gameName, GameAwardPayload.PRIZE,
//                         _result.name, _result.getThumbnailMedia()),
//                     true, new ResultListener.NOOP<Void>());
            }
        });
    }

    /**
     * Depending on the type of the list, prune any items are not supposed to be in it legally.
     */
    protected void pruneItemsFromList (ItemListInfoRecord infoRecord, Collection<Item> items)
        throws PersistenceException
    {
        Predicate<Item> pred;
        switch (infoRecord.type) {
        default:
            throw new PersistenceException("Do not know how to prune items from lists of type " +
                infoRecord.type);
            // implicit break

        case ItemListInfo.VIDEO_PLAYLIST: // fall through to AUDIO_PLAYLIST
        case ItemListInfo.AUDIO_PLAYLIST:
            // the items must all be owned
            final int memberId = infoRecord.memberId;
            pred = new Predicate<Item>() {
                public boolean isMatch (Item item) {
                    return (item.ownerId == memberId);
                }
            };
            break;

        case ItemListInfo.CATALOG_BUNDLE:
            // the items must all be listed in the catalog
            pred = new Predicate<Item>() {
                public boolean isMatch (Item item) {
                    return (item.ownerId == 0); // TODO: this catches other cases besides listed?
                }
            };
            break;
        }

        // filter to keep only the items that match the predicate
        pred.filter(items);
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

        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(itemType, lner);
        if (repo == null) {
            return; // getRepository already informed the listener about this problem
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Object>("updateItemUsage", lner) {
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
                log.log(Level.WARNING, "Failed to resolve updated avatar. [id=" + avatarId + "]",
                    cause);
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
            // if they're wearing it, update that.
            if (avatar.equals(memObj.avatar)) {
                memObj.setAvatar(avatar);
                MsoyServer.memberMan.updateOccupantInfo(memObj);
            }

            // probably we'll update it in their cache, too.
            if (memObj.avatarCache.contains(avatar)) {
                memObj.updateAvatarCache(avatar);

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
                MsoyServer.memberMan.updateOccupantInfo(memObj);
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
        final LookupList unused = new LookupList();
        final LookupList scened = new LookupList();

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

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Object>("updateItemsUsage", lner) {
            public Object invokePersistResult () throws Exception {
                for (Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]> tup : unused) {
                    tup.left.markItemUsage(tup.right, Item.UNUSED, 0);
                }
                for (Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]> tup : scened) {
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
        byte type = rec.getType();
        if (type == Item.AVATAR) {
            MemberNodeActions.avatarUpdated(rec.ownerId, rec.itemId);

        } else if (type == Item.GAME) {
            MsoyServer.peerMan.invokeNodeAction(new GameUpdatedAction(((GameRecord) rec).gameId));
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
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(type, lner);
        if (repo == null) {
            return;
        }

        // load ye items
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<List<Item>>("loadRecentlyTouched", lner) {
            public List<Item> invokePersistResult () throws Exception {
                List<ItemRecord> list = repo.loadRecentlyTouched(memberId, maxCount);
                List<Item> returnList = Lists.newArrayListWithCapacity(list.size());
                for (int ii = 0, nn = list.size(); ii < nn; ii++) {
                    returnList.add(list.get(ii).toItem());
                }
                return returnList;
            }
        });
    }

    /**
     * Fetches the tags for a given item.
     */
    public void getTags (final ItemIdent ident, ResultListener<Collection<String>> lner)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<String>>("getTags", lner) {
            public Collection<String> invokePersistResult () throws Exception {
                List<String> result = Lists.newArrayList();
                for (TagNameRecord tagName : repo.getTagRepository().getTags(ident.itemId)) {
                    result.add(tagName.tag);
                }
                return result;
            }
        });
    }

    /**
     * Fetch the tagging history for a given item.
     */
    public void getTagHistory (final ItemIdent ident,
                               ResultListener<Collection<TagHistory>> lner)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>("getTagHistory", lner) {
            public Collection<TagHistory> invokePersistResult () throws Exception {
                Map<Integer, MemberRecord> memberCache = new HashMap<Integer, MemberRecord>();
                List<TagHistory> list = Lists.newArrayList();
                for (TagHistoryRecord record :
                         repo.getTagRepository().getTagHistoryByTarget(ident.itemId)) {
                    // TODO: we should probably cache in MemberRepository
                    MemberRecord memRec = memberCache.get(record.memberId);
                    if (memRec == null) {
                        memRec = MsoyServer.memberRepo.loadMember(record.memberId);
                        memberCache.put(record.memberId, memRec);
                    }

                    TagNameRecord tag = repo.getTagRepository().getTag(record.tagId);
                    TagHistory history = new TagHistory();
                    history.member = memRec.getName();
                    history.tag = tag.tag;
                    history.action = record.action;
                    history.time = new Date(record.time.getTime());
                    list.add(history);
                }
                return list;
            }
        });
    }

    /**
     * Fetch the list of recently assigned tags for the specified member.
     */
    public void getRecentTags (final int memberId, ResultListener<Collection<TagHistory>> lner)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>("getRecentTags", lner) {
            public Collection<TagHistory> invokePersistResult () throws Exception {
                MemberRecord memRec = MsoyServer.memberRepo.loadMember(memberId);
                MemberName memName = memRec.getName();
                List<TagHistory> list = Lists.newArrayList();
                for (Entry<Byte, ItemRepository<ItemRecord, ?, ?, ?>> entry :
                         _repos.entrySet()) {
                    ItemRepository<ItemRecord, ?, ?, ?> repo = entry.getValue();
                    for (TagHistoryRecord record :
                             repo.getTagRepository().getTagHistoryByMember(memberId)) {
                        TagNameRecord tag = record.tagId == -1 ? null :
                            repo.getTagRepository().getTag(record.tagId);
                        TagHistory history = new TagHistory();
                        history.member = memName;
                        history.tag = tag == null ? null : tag.tag;
                        history.action = record.action;
                        history.time = new Date(record.time.getTime());
                        list.add(history);
                    }
                }
                return list;
            }
        });
    }

    /**
     * Add the specified tag to the specified item or remove it. Return a tag history object if the
     * tag did not already exist.
     */
    public void tagItem (final ItemIdent ident, final int taggerId, String rawTagName,
                         final boolean doTag, ResultListener<TagHistory> lner)
    {
        // sanitize the tag name
        final String tagName = rawTagName.trim().toLowerCase();

        if (!TagNameRecord.VALID_TAG.matcher(tagName).matches()) {
            lner.requestFailed(new IllegalArgumentException("Invalid tag '" + tagName + "'"));
            return;
        }

        // locate the appropriate repository
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        // and perform the tagging
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<TagHistory>("tagItem", lner) {
            public TagHistory invokePersistResult () throws Exception {
                long now = System.currentTimeMillis();

                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new PersistenceException("Missing item for tagItem [item=" + ident + "]");
                }
                int originalId = (item.sourceId != 0) ? item.sourceId : ident.itemId;

                // map tag to tag id
                TagNameRecord tag = repo.getTagRepository().getOrCreateTag(tagName);

                // and do the actual work
                TagHistoryRecord historyRecord = doTag ?
                    repo.getTagRepository().tag(originalId, tag.tagId, taggerId, now) :
                    repo.getTagRepository().untag(originalId, tag.tagId, taggerId, now);
                if (historyRecord != null) {
                    // look up the member
                    MemberRecord mrec = MsoyServer.memberRepo.loadMember(taggerId);
                    // and create the return value
                    TagHistory history = new TagHistory();
                    history.member = mrec.getName();
                    history.tag = tag.tag;
                    history.action = historyRecord.action;
                    history.time = new Date(historyRecord.time.getTime());
                    return history;
                }
                return null;
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
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>("setFlags", lner) {
            public Void invokePersistResult () throws Exception {
                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new PersistenceException("Can't find item [item=" + ident + "]");
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
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident, lner);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Void>("setMature", lner) {
            public Void invokePersistResult () throws Exception {
                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new PersistenceException("Can't find item [item=" + ident + "]");
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
        final HashIntMap<ItemRepository<ItemRecord, ?, ?, ?>> repos =
            new HashIntMap<ItemRepository<ItemRecord, ?, ?, ?>>();

        // get all the repos for all item idents
        for (ItemIdent ident : idents) {
            if (! repos.containsKey(ident.type)) {
                // get a new repo, and save in the map
                ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(ident.type, rl);
                if (repo == null) {
                    return; // error already reported to listener...
                } else {
                    repos.put(ident.type, repo);
                }
            }
        }

        // pull item names from repos
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<String[]>("getItemNames", new ResultAdapter<String[]>(rl)) {
            public String[] invokePersistResult () throws Exception {
                String[] itemNames = new String[idents.length];
                for (int ii = 0; ii < idents.length; ii++) {
                    ItemIdent ident = idents[ii];
                    ItemRecord rec = repos.get(ident.type).loadItem(ident.itemId);
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
    public void reclaimItem (ClientObject caller, final ItemIdent item,
                             final InvocationService.ConfirmListener lner)
        throws InvocationException
    {
        final MemberObject user = (MemberObject) caller;
        if (user.isGuest()) {
            throw new InvocationException(ItemCodes.E_ACCESS_DENIED);
        }

        if (item.type == Item.AVATAR) {
            log.log(Level.WARNING, "Tried to reclaim invalid item type [type=" + item.type +
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
                    MsoyServer.screg.resolveScene(result.location,
                        new SceneRegistry.ResolutionListener() {
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
                                log.log(Level.WARNING, "Scene failed to resolve. [id=" + sceneId +
                                    "]", reason);
                                lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
                            }
                        });
                } else {
                    // TODO: avatar reclamation will be possible
                    log.log(Level.WARNING, "Item to be reclaimed is neither decor nor furni " +
                        "[type=" + result.getType() + ", id=" + result.itemId + "]");
                    lner.requestFailed(InvocationCodes.INTERNAL_ERROR);
                    return;
                }
            }
            public void requestFailed (Exception cause) {
                log.log(Level.WARNING, "Unable to retrieve item.", cause);
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
        final ItemRepository<ItemRecord, ?, ?, ?> repo = getRepository(itemType, lner);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>("getRandomCatalogItem", lner) {
            public Item invokePersistResult () throws Exception {
                CatalogRecord<? extends ItemRecord> record;
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

    @SuppressWarnings("unchecked")
    protected void registerRepository (byte itemType, ItemRepository repo)
    {
        _repos.put(itemType, repo);
        repo.init(itemType, MsoyServer.memoryRepo, _eventLog);
    }

    /**
     * Get the specified ItemRepository. This method is called both from the dobj thread and the
     * servlet handler threads but need not be synchronized because the repositories table is
     * created at server startup time and never modified.
     */
    protected ItemRepository<ItemRecord, ?, ?, ?> getRepositoryFor (byte type)
        throws MissingRepositoryException
    {
        ItemRepository<ItemRecord, ?, ?, ?> repo = _repos.get(type);
        if (repo == null) {
            throw new MissingRepositoryException(type);
        }
        return repo;
    }

    /**
     * A class that helps manage loading or storing a bunch of items that may be spread in
     * difference repositories.
     */
    protected class LookupList
        implements Iterable<Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]>>
    {
        /**
         * Add the specified item id to the list.
         */
        public void addItem (ItemIdent ident)
            throws MissingRepositoryException
        {
            addItem(ident.type, ident.itemId);
        }

        /**
         * Add the specified item id to the list.
         */
        public void addItem (byte itemType, int itemId)
            throws MissingRepositoryException
        {
            LookupType lt = _byType.get(itemType);
            if (lt == null) {
                lt = new LookupType(itemType, getRepositoryFor(itemType));
                _byType.put(itemType, lt);
            }
            lt.addItemId(itemId);
        }

        public void removeItem (byte itemType, int itemId)
        {
            LookupType lt = _byType.get(itemType);
            if (lt != null) {
                lt.removeItemId(itemId);
            }
        }

        // from Iterable
        public Iterator<Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]>> iterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<ItemRepository<ItemRecord, ?, ?, ?>, int[]>(
                        lookup.repo, lookup.getItemIds());
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public Iterator<Tuple<Byte, int[]>> typeIterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<Byte, int[]>>() {
                public boolean hasNext () {
                    return itr.hasNext();
                }
                public Tuple<Byte, int[]> next () {
                    LookupType lookup = itr.next();
                    return new Tuple<Byte, int[]>(lookup.type, lookup.getItemIds());
                }
                public void remove () {
                    throw new UnsupportedOperationException();
                }
            };
        }

        protected class LookupType
        {
            /** The item type associated with this list. */
            public byte type;

            /** The repository associated with this list. */
            public ItemRepository<ItemRecord, ?, ?, ?> repo;

            /**
             * Create a new LookupType for the specified repository.
             */
            public LookupType (byte type, ItemRepository<ItemRecord, ?, ?, ?> repo)
            {
                this.type = type;
                this.repo = repo;
            }

            /**
             * Add the specified item to the list.
             */
            public void addItemId (int id)
            {
                _ids.add(id);
            }

            public void removeItemId (int id)
            {
                _ids.remove(id);
            }

            /**
             * Get all the item ids in this list.
             */
            public int[] getItemIds ()
            {
                return _ids.toIntArray();
            }

            protected ArrayIntSet _ids = new ArrayIntSet();
        }

        /** A mapping of item type to LookupType record of repo / ids. */
        protected HashMap<Byte, LookupType> _byType = new HashMap<Byte, LookupType>();
    } /* End: class LookupList. */

    /** Notifies other nodes when a game record is updated. */
    protected static class GameUpdatedAction extends GameNodeAction
    {
        public GameUpdatedAction (int gameId) {
            super(gameId);
        }

        @Override protected void execute () {
            MsoyServer.gameReg.gameUpdatedOnPeer(_gameId);
        }
    }

    /** A reference to our game repository. We'd just look this up from the table but we can't
     * downcast an ItemRepository to a GameRepository, annoyingly. */
    protected GameRepository _gameRepo;

    /** A reference to our pet repository. See {@link #_gameRepository} for complaint. */
    protected PetRepository _petRepo;

    /** A reference to our avatar repository. See {@link #_gameRepository} for complaint. */
    protected AvatarRepository _avatarRepo;

    /** A reference to our decor repository. See {@link #_gameRepository} for complaint. */
    protected DecorRepository _decorRepo;

    /** A reference to our trophy source repository. See {@link #_gameRepository} for complaint. */
    protected TrophySourceRepository _tsourceRepo;

    /** Maps byte type ids to repository for all digital item types. */
    protected Map<Byte, ItemRepository<ItemRecord, ?, ?, ?>> _repos = Maps.newHashMap();

    /** The special repository that stores item lists. */
    protected ItemListRepository _listRepo;

    /** Reference to the event logger. */
    protected MsoyEventLogger _eventLog;
}
