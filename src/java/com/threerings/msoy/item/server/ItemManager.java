//
// $Id$

package com.threerings.msoy.item.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.web.data.MemberName;
import com.threerings.msoy.world.data.FurniData;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.item.web.TagHistory;

import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PetRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.RatingRecord;
import com.threerings.msoy.item.server.persist.TagHistoryRecord;
import com.threerings.msoy.item.server.persist.TagNameRecord;
import com.threerings.msoy.item.server.persist.TagPopularityRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
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
     * Initializes the item manager, which will establish database connections
     * for all of its item repositories.
     */
    @SuppressWarnings("unchecked")
    public void init (ConnectionProvider conProv) throws PersistenceException
    {
        ItemRepository repo;
        // create our various repositories
        repo = new AudioRepository(conProv);
        _repos.put(Item.AUDIO, repo);
        repo = new AvatarRepository(conProv);
        _repos.put(Item.AVATAR, repo);
        repo = new DocumentRepository(conProv);
        _repos.put(Item.DOCUMENT, repo);
        repo = new FurnitureRepository(conProv);
        _repos.put(Item.FURNITURE, repo);
        repo = (_gameRepo = new GameRepository(conProv));
        _repos.put(Item.GAME, repo);
        repo = new PetRepository(conProv);
        _repos.put(Item.PET, repo);
        repo = new PhotoRepository(conProv);
        _repos.put(Item.PHOTO, repo);

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), true);
    }

    /**
     * Provides a reference to the {@link GameRepository} which is used for
     * nefarious ToyBox purposes.
     */
    public GameRepository getGameRepository ()
    {
        return _gameRepo;
    }

    /**
     * Get the specified item.
     */
    public void getItem (final ItemIdent ident, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // TODO: do we have to check cloned items as well?
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                ItemRecord rec = repo.loadItem(ident.itemId);
                return (rec != null) ? rec.toItem() : null;
            }

            public void handleSuccess ()
            {
                if (_result != null) {
                    super.handleSuccess();
                } else {
                    handleFailure(new Exception("No such item"));
                }
            }
        });
    }

    /**
     * Mass-load the specified items. If any type is invalid, none are
     * returned. If specific itemIds are invalid, they are omitted from
     * the result list.
     */
    public void getItems (
        Collection<ItemIdent> ids, ResultListener<ArrayList<Item>> listener)
    {
        final LookupList list = new LookupList();
        try {
            for (ItemIdent ident : ids) {
                list.addItem(ident);
            }

        } catch (MissingRepositoryException mre) {
            listener.requestFailed(mre);
            return;
        }

        // do it all at once
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(listener) {
                public ArrayList<Item> invokePersistResult () throws PersistenceException {
                    // create a list to hold the results
                    ArrayList<Item> items = new ArrayList<Item>();

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

    /**
     * Update usage of the specified items.
     *
     * This method assumes that the specified avatars are both valid
     * and owned by the user in question.
     *
     * The supplied listener will be notified of success with null.
     */
    public void updateItemUsage (
        final int memberId, final Avatar oldAvatar, final Avatar newAvatar,
        final ResultListener<?> listener)
    {
        if (ObjectUtil.equals(oldAvatar, newAvatar)) {
            listener.requestCompleted(null); // mr. no-op
            return;
        }

        final ItemRepository<ItemRecord> repo =
            getRepository(Item.AVATAR, listener);
        if (repo == null) {
            return;
        }

        final int oldId = (oldAvatar == null) ? 0 : oldAvatar.itemId;
        final int newId = (newAvatar == null) ? 0 : newAvatar.itemId;

        @SuppressWarnings("unchecked")
        ResultListener<Object> rlo = (ResultListener<Object>) listener;
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Object>(rlo) {
            public Object invokePersistResult ()
                throws PersistenceException
            {
                if (oldId != 0) {
                    repo.markItemUsage(new int[] { oldId }, Item.UNUSED, 0);
                }
                if (newId != 0) {
                    repo.markItemUsage(new int[] { newId },
                        Item.USED_AS_AVATAR, memberId);
                }
                return null;
            }

            @Override
            public void handleSuccess ()
            {
                super.handleSuccess();

                oldAvatar.used = Item.UNUSED;
                oldAvatar.location = 0;
                newAvatar.used = Item.USED_AS_AVATAR;
                newAvatar.location = memberId;
                updateUserCache(oldAvatar);
                updateUserCache(newAvatar);
            }
        });
    }

    /**
     * Update usage of the specified items.
     *
     * The supplied listener will be notified of success with null.
     */
    public void updateItemUsage (
        final int sceneId, FurniData[] removedFurni, FurniData[] addedFurni,
        ResultListener<?> listener)
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
                            listener.requestFailed(new Exception("Furni " +
                                "added with invalid item source."));
                            return;
                        }
                        continue;
                    }
                    scened.addItem(furni.itemType, furni.itemId);
                    unused.removeItem(furni.itemType, furni.itemId);
                }
            }

        } catch (MissingRepositoryException mre) {
            listener.requestFailed(mre);
            return;
        }

        @SuppressWarnings("unchecked")
        ResultListener<Object> rlo = (ResultListener<Object>) listener;
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Object>(rlo) {
            public Object invokePersistResult ()
                throws PersistenceException
            {
                for (Tuple<ItemRepository<ItemRecord>, int[]> tup : unused) {
                    tup.left.markItemUsage(tup.right, Item.UNUSED, 0);
                }
                for (Tuple<ItemRepository<ItemRecord>, int[]> tup : scened) {
                    tup.left.markItemUsage(tup.right,
                        Item.USED_AS_FURNITURE, sceneId);
                }
                return null;
            }

            @Override
            public void handleSuccess ()
            {
                super.handleSuccess();

                // TODO: crap, the edited items may be ones we don't own.
//                for (Tuple<Byte, int[]> tup : unused.typeIterator()) {
//                    updateUserCache(TODO:ownerId, tup.left, tup.right,
//                        new ItemUpdateOp() {
//                            public void update (Item item) {
//                                item.used = Item.UNUSED;
//                                item.location = 0;
//                            }
//                        });
//                }
//                for (Tuple<Byte, int[]> tup : scenes.typeIterator()) {
//                    updateUserCache(TODO:ownerId, tup.left, tup.right,
//                        new ItemUpdateOp() {
//                            public void update (Item item) {
//                                item.used = Item.USED_AS_FURNITURE;
//                                item.location = sceneId;
//                            }
//                        });
//                }
            }
        });
    }

    /**
     * Inserts the supplied item into the system. The item should be fully
     * configured, and an item id will be assigned during the insertion
     * process. Success or failure will be communicated to the supplied result
     * listener.
     */
    public void insertItem (final Item item, ResultListener<Item> listener)
    {
        final ItemRecord record = ItemRecord.newRecord(item);
        byte type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException {
                repo.insertOriginalItem(record);
                item.itemId = record.itemId;
                return item;
            }
            public void handleSuccess () {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Updates the supplied item. The item should have previously been checked
     * for validity. Success or failure will be communicated to the supplied
     * result listener.
     */
    public void updateItem (final Item item, ResultListener<Item> listener)
    {
        final ItemRecord record = ItemRecord.newRecord(item);
        byte type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and update the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException {
                repo.updateOriginalItem(record);
                return item;
            }
            public void handleSuccess () {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Get the details of specified item: display-friendly names of creator
     * and owner and the rating given to this item by the member specified
     * by memberId.
     *
     * TODO: This method re-reads an ItemRecord that the caller of the
     * function almost certainly already has, which seems kind of wasteful.
     * On the other hand, transmitting that entire Item over the wire seems
     * wasteful, too, and sending in ownerId and creatorId by themselves
     * seems cheesy. If we were to cache ItemRecords in the repository, this
     * would be fine. Will we?
     */
    public void getItemDetail (final ItemIdent ident, final int memberId,
                               ResultListener<ItemDetail> listener)
    {
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<ItemDetail>(listener) {
            public ItemDetail invokePersistResult () throws PersistenceException {
                ItemRecord record = repo.loadItem(ident.itemId);
                ItemDetail detail = new ItemDetail();
                detail.item = record.toItem();
                RatingRecord<ItemRecord> rr = repo.getRating(ident.itemId, memberId);
                detail.memberRating = (rr == null) ? 0 : rr.rating;
                MemberRecord memRec = MsoyServer.memberRepo.loadMember(record.creatorId);
                detail.creator = memRec.getName();
                if (record.ownerId != -1) {
                    memRec = MsoyServer.memberRepo.loadMember(record.ownerId);
                    detail.owner = memRec.getName();
                } else {
                    detail.owner = null;
                }
                return detail;
            }
        });
    }

    /**
     * Loads up the inventory of items of the specified type for the specified
     * member.
     */
    public void loadInventory (final int memberId, byte type,
                               ResultListener<ArrayList<Item>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and load their items; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<ArrayList<Item>>(listener) {
            public ArrayList<Item> invokePersistResult () throws PersistenceException {
                Collection<ItemRecord> list = repo.loadOriginalItems(memberId);
                list.addAll(repo.loadClonedItems(memberId));
                ArrayList<Item> newList = new ArrayList<Item>();
                for (ItemRecord record : list) {
                    newList.add(record.toItem());
                }
                return newList;
            }
        });
    }

    /**
     * Fetches the entire catalog of listed items of the given type.
     */
    public void loadCatalog (byte type, ResultListener<List<CatalogListing>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        // and load the catalog
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<List<CatalogListing>>(listener) {
            public List<CatalogListing> invokePersistResult () throws PersistenceException {
                IntSet members = new ArrayIntSet();
                List<CatalogListing> list = new ArrayList<CatalogListing>();
                // fetch catalog records and loop over them
                for (CatalogRecord record : repo.loadCatalog()) {
                    // convert them to listings
                    list.add(record.toListing());
                    // and keep track of which member names we need to look up
                    members.add(record.item.creatorId);
                }
                IntMap<MemberName> map = new HashIntMap<MemberName>();
                int[] idArr = members.toIntArray();
                // now look up the names and build a map of memberId -> MemberName
                for (MemberNameRecord record: MsoyServer.memberRepo.loadMemberNames(idArr)) {
                    map.put(record.memberId, new MemberName(record.name, record.memberId));
                }
                // finally fill in the listings using the map
                for (CatalogListing listing : list) {
                    listing.creator = map.get(listing.creator.getMemberId());
                }
                return list;
            }
        });
    }

    /**
     * Purchases a given item for a given member from the catalog by
     * creating a new clone row in the appropriate database table.
     */
    public void purchaseItem (final int memberId, final ItemIdent ident,
                              ResultListener<Item> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the purchase
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load the item being purchased
                ItemRecord item = repo.loadOriginalItem(ident.itemId);
                // sanity check it
                if (item.ownerId != -1) {
                    throw new PersistenceException(
                        "Can only clone listed items [itemId=" +
                        item.itemId + "]");
                }
                // create the clone row in the database!
                int cloneId = repo.insertClone(item.itemId, memberId);
                // then dress the loaded item up as a clone
                item.ownerId = memberId;
                item.parentId = item.itemId;
                item.itemId = cloneId;
                return item.toItem();
            }

            @Override
            public void handleSuccess ()
            {
                super.handleSuccess();

                updateUserCache(_result);
            }
        });
    }

    /**
     * Lists the given item in the catalog by creating a new item row and
     * a new catalog row and returning the immutable form of the item.
     */
    public void listItem (final ItemIdent ident,
                          ResultListener<CatalogListing> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the listing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<CatalogListing>(listener) {
            public CatalogListing invokePersistResult ()
                throws PersistenceException
            {
                // load a copy of the original item
                ItemRecord listItem = repo.loadOriginalItem(ident.itemId);
                if (listItem == null) {
                    throw new PersistenceException(
                        "Can't find object to list [item= " + ident + "]");
                }
                if (listItem.ownerId == -1) {
                    throw new PersistenceException(
                        "Object is already listed [item=" + ident + "]");
                }
                // reset the owner
                listItem.ownerId = -1;
                // and the iD
                listItem.itemId = 0;
                // then insert it as the immutable copy we list
                repo.insertOriginalItem(listItem);
                // and finally create & insert the catalog record
                CatalogRecord record = repo.insertListing(
                    listItem, new Timestamp(System.currentTimeMillis()));
                return record.toListing();
            }
        });
    }

    /**
     * Remix a clone, turning it back into a full-featured original.
     */
    public void remixItem (final ItemIdent ident, ResultListener<Item> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load a copy of the clone to modify
                ItemRecord item = repo.loadClone(ident.itemId);
                if (item == null) {
                    throw new PersistenceException(
                        "Can't find item [item=" + ident + "]");
                }
                // TODO: make sure we should not use the original creator here
                // make it ours
                item.creatorId = item.ownerId;
                // let the object forget whence it came
                int originalId = item.parentId;
                item.parentId = -1;
                // insert it as a genuinely new item
                item.itemId = 0;
                repo.insertOriginalItem(item);
                // delete the old clone
                repo.deleteClone(ident.itemId);
                // copy tags from the original to the new item
                repo.copyTags(
                    originalId, item.itemId, item.ownerId,
                    System.currentTimeMillis());
                return item.toItem();
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // update the item in the user's cached inventory
                updateUserCache(_result);
            }
        });

    }

    /** Fetch the rating a user has given an item, or 0. */
    public void getRating (final ItemIdent ident, final int memberId,
                           ResultListener<Byte> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Byte>(listener) {
                public Byte invokePersistResult () throws PersistenceException {
                    RatingRecord<ItemRecord> record =
                        repo.getRating(ident.itemId, memberId);
                    return record != null ? record.rating : 0;
                }
            });
    }

    /** Fetch the most popular tags across all items. */
    public void getPopularTags (
        byte type, final int rows,
        ResultListener<Map<String, Integer>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(type, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Map<String, Integer>>(listener) {
                public Map<String, Integer> invokePersistResult () throws PersistenceException {
                    Map<String, Integer> result = new HashMap<String, Integer>();
                    for (TagPopularityRecord record : repo.getPopularTags(rows)) {
                        result.put(record.tag, record.count);
                    }
                    return result;
                }
            });

    }

    /** Fetch the tags for a given item. */
    public void getTags (final ItemIdent ident,
                         ResultListener<Collection<String>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<String>>(listener) {
                public Collection<String> invokePersistResult ()
                        throws PersistenceException {
                    ArrayList<String> result = new ArrayList<String>();
                    for (TagNameRecord tagName : repo.getTags(ident.itemId)) {
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
                               ResultListener<Collection<TagHistory>> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>(listener) {
            public Collection<TagHistory> invokePersistResult () throws PersistenceException {
                Map<Integer, MemberRecord> memberCache = new HashMap<Integer, MemberRecord>();
                ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                for (TagHistoryRecord<ItemRecord> record :
                        repo.getTagHistoryByItem(ident.itemId)) {
                    // TODO: we should probably cache in MemberRepository
                    MemberRecord memRec = memberCache.get(record.memberId);
                    if (memRec == null) {
                        memRec = MsoyServer.memberRepo.loadMember(record.memberId);
                        memberCache.put(record.memberId, memRec);
                    }

                    TagNameRecord tag = repo.getTag(record.tagId);
                    TagHistory history = new TagHistory();
                    history.item = new ItemIdent(ident.type, ident.itemId);
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

    /** Fetch the tagging history for any item by  a given member. */
    public void getTagHistory (final int memberId,
                               ResultListener<Collection<TagHistory>> listener)
    {
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Collection<TagHistory>>(listener) {
            public Collection<TagHistory> invokePersistResult ()
                throws PersistenceException {
                MemberRecord memRec = MsoyServer.memberRepo.loadMember(memberId);
                MemberName memName = memRec.getName();
                ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                for (Entry<Byte, ItemRepository<ItemRecord>> entry : _repos.entrySet()) {
                    byte type = entry.getKey();
                    ItemRepository<ItemRecord> repo = entry.getValue();
                    for (TagHistoryRecord<ItemRecord> record :
                             repo.getTagHistoryByMember(memberId)) {
                        TagNameRecord tag = record.tagId == -1 ? null : repo.getTag(record.tagId);
                        TagHistory history = new TagHistory();
                        history.item = new ItemIdent(type, record.itemId);
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
     * Records the specified member's rating of an item.
     */
    public void rateItem (final ItemIdent ident, final int memberId, final byte rating,
                          ResultListener<Float> listener)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Float>(listener) {
            public Float invokePersistResult ()
                throws PersistenceException {
                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new PersistenceException("Can't find item [item=" + ident + "]");
                }

                int originalId;
                if (item.parentId != -1) {
                    // it's a clone: use the parent ID
                    originalId = item.parentId;
                } else {
                    // not a clone; make sure we're not trying to rate a mutable
                    if (item.ownerId != -1) {
                        throw new PersistenceException(
                            "Can't rate mutable object [item=" + ident + "]");
                    }
                    // and use our real ID
                    originalId = ident.itemId;
                }

                // record this player's rating and obtain the new summarized rating
                return repo.rateItem(originalId, memberId, rating);
            }
        });
    }

    /**
     * Add the specified tag to the specified item. Return a tag history object
     * if the tag did not already exist.
     */
    public void tagItem (ItemIdent ident, int taggerId, String tagName,
                         ResultListener<TagHistory> listener)
    {
        itemTagging(ident, taggerId, tagName, listener, true);
    }

    /**
     * Remove the specified tag from the specified item. Return a tag history
     * object if the tag existed.
     */
    public void untagItem (ItemIdent ident, int taggerId, String tagName,
                           ResultListener<TagHistory> listener)
    {
        itemTagging(ident, taggerId, tagName, listener, false);
    }

    // from ItemProvider
    public void getInventory (
        ClientObject caller, final byte type,
        final InvocationService.InvocationListener listener)
        throws InvocationException
    {
        final MemberObject memberObj = (MemberObject) caller;
        if (memberObj.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }

        if (memberObj.isInventoryResolving(type)) {
            // already loaded/resolving!
            return; // this is not an error condition, we expect that
            // some other entity is loading and the user will notice soon
            // enough.
        }

        // mark the item type as resolving
        memberObj.setResolvingInventory(
            memberObj.resolvingInventory | (1 << type));

        // then, load that type
        loadInventory(memberObj.getMemberId(), type, new ResultListener<ArrayList<Item>>() {
            public void requestCompleted (ArrayList<Item> result)
            {
                // apply the changes
                memberObj.startTransaction();
                try {
                    for (Item item : result) {
                        memberObj.addToInventory(item);
                    }
                    memberObj.setLoadedInventory(
                        memberObj.loadedInventory | (1 << type));

                } finally {
                    memberObj.commitTransaction();
                }
            }

            public void requestFailed (Exception cause)
            {
                log.warning("Unable to retrieve inventory [cause=" + cause + "].");
                listener.requestFailed(InvocationCodes.INTERNAL_ERROR);

                // we're not resolving anymore.. oops
                memberObj.setResolvingInventory(
                    memberObj.resolvingInventory & ~(1 << type));
            }
        });
    }

    /**
     * Does the facade work for tagging.
     */
    protected void itemTagging (
        final ItemIdent ident, final int taggerId, final String rawTagName,
        ResultListener<TagHistory> listener, final boolean doTag)
    {
        // sanitize the tag name
        final String tagName = rawTagName.trim().toLowerCase();

        if (!validTag.matcher(tagName).matches()) {
            listener.requestFailed(new IllegalArgumentException(
                "Invalid tag [tag=" + tagName + "]"));
            return;
        }

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = getRepository(ident, listener);
        if (repo == null) {
            return;
        }

        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<TagHistory>(listener) {
            public TagHistory invokePersistResult () throws PersistenceException {
                long now = System.currentTimeMillis();

                ItemRecord item = repo.loadItem(ident.itemId);
                if (item == null) {
                    throw new PersistenceException(
                        "Can't find item [item=" + ident + "]");
                }
                int originalId = item.parentId != -1 ? item.parentId : ident.itemId;

                // map tag to tag id
                TagNameRecord tag = repo.getTag(tagName);

                // and do the actual work
                TagHistoryRecord<ItemRecord> historyRecord = doTag ?
                    repo.tagItem(originalId, tag.tagId, taggerId, now) :
                    repo.untagItem(originalId, tag.tagId, taggerId, now);
                if (historyRecord != null) {
                    // look up the member
                    MemberRecord mrec = MsoyServer.memberRepo.loadMember(taggerId);
                    // and create the return value
                    TagHistory history = new TagHistory();
                    history.item = new ItemIdent(ident.type, originalId);
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
     * Called when an item is updated or created and we should ensure
     * that the user's MemberObject cache of their inventory is up-to-date.
     */
    protected void updateUserCache (ItemRecord rec)
    {
        updateUserCache(rec, null);
    }

    /**
     * Called when an item is updated or created and we should ensure
     * that the user's MemberObject cache of their inventory is up-to-date.
     */
    protected void updateUserCache (Item item)
    {
        updateUserCache(null, item);
    }

    /**
     * Internal cache-updatey method that takes a record or an item.
     */
    protected void updateUserCache (ItemRecord rec, Item item)
    {
        // first locate the owner
        int ownerId;
        byte type;
        if (item == null) {
            ownerId = rec.ownerId;
            type = rec.getType();
        } else {
            ownerId = item.ownerId;
            type = item.getType();
        }
        MemberObject memObj = MsoyServer.lookupMember(ownerId);

        // if found and this item's inventory type is loaded or resolving,
        // update or add it. (If we're resolving, we might be the first
        // adding the item. That's ok, nothing should think it's
        // actually loaded until the inventoryLoaded flag is set.
        if (memObj != null && memObj.isInventoryResolving(type)) {
            if (item == null) {
                item = rec.toItem(); // lazy-create when we need it.
            }
            if (memObj.inventory.contains(item)) {
                memObj.updateInventory(item);
            } else {
                memObj.addToInventory(item);
            }
        }
    }

    /**
     * Update changed items that are already loaded in a user's inventory.
     */
    protected void updateUserCache (int ownerId, byte type,
        int[] ids, ItemUpdateOp op)
    {
        MemberObject memObj = MsoyServer.lookupMember(ownerId);
        if (memObj != null && memObj.isInventoryLoaded(type)) {
            memObj.startTransaction();
            try {
                for (int id : ids) {
                    Item item = memObj.inventory.get(new ItemIdent(type, id));
                    if (item == null) {
                        // TODO: this possibly a bigger error and we should
                        // maybe throw an exception
                        log.warning("Unable to update missing item: " + item);
                        continue;
                    }
                    op.updateItem(item);
                    memObj.updateInventory(item);
                }
            } finally {
                memObj.commitTransaction();
            }
        }
    }

    /**
     * Helper function for mapping ident to repository.
     */
    protected ItemRepository<ItemRecord> getRepository (
        ItemIdent ident, ResultListener<?> listener)
    {
        return getRepository(ident.type, listener);
    }

    /**
     * Helper function for mapping ident to repository.
     */
    protected ItemRepository<ItemRecord> getRepository (
        byte type, ResultListener<?> listener)
    {
        try {
            return getRepository(type);

        } catch (MissingRepositoryException mre) {
            listener.requestFailed(mre);
            return null;
        }
    }

    /**
     * Get the specified ItemRepository.
     */
    protected ItemRepository<ItemRecord> getRepository (byte type)
        throws MissingRepositoryException
    {
        ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            throw new MissingRepositoryException(type);
        }
        return repo;
    }


    /** Contains a reference to our game repository. We'd just look this up
     * from the table but we can't downcast an ItemRepository<ItemRecord> to a
     * GameRepository, annoyingly. */
    protected GameRepository _gameRepo;

    /**
     *  A class that helps manage loading or storing a bunch of items
     *  that may be spread in difference repositories.
     */
    protected class LookupList
        implements Iterable<Tuple<ItemRepository<ItemRecord>, int[]>>
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
                lt = new LookupType(itemType, getRepository(itemType));
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
        public Iterator<Tuple<ItemRepository<ItemRecord>, int[]>> iterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<ItemRepository<ItemRecord>, int[]>>() {
                public boolean hasNext ()
                {
                    return itr.hasNext();
                }

                public Tuple<ItemRepository<ItemRecord>, int[]> next ()
                {
                    LookupType lookup = itr.next();
                    return new Tuple<ItemRepository<ItemRecord>, int[]>(
                        lookup.repo, lookup.getItemIds());
                }

                public void remove ()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public Iterator<Tuple<Byte, int[]>> typeIterator ()
        {
            final Iterator<LookupType> itr = _byType.values().iterator();
            return new Iterator<Tuple<Byte, int[]>>() {
                public boolean hasNext ()
                {
                    return itr.hasNext();
                }

                public Tuple<Byte, int[]> next ()
                {
                    LookupType lookup = itr.next();
                    return new Tuple<Byte, int[]>(
                        lookup.type, lookup.getItemIds());
                }

                public void remove ()
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        protected class LookupType
        {
            /** The item type associated with this list. */
            public byte type;

            /** The repository associated with this list. */
            public ItemRepository<ItemRecord> repo;

            /**
             * Create a new LookupType for the specified repository.
             */
            public LookupType (byte type, ItemRepository<ItemRecord> repo)
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
        protected HashMap<Byte, LookupType> _byType =
            new HashMap<Byte, LookupType>();
    } /* End: class LookupList. */

    /**
     * An interface for updating an item in a user's cache.
     */
    protected interface ItemUpdateOp
    {
        /**
         * Update the specified item.
         */
        public void updateItem (Item item);
    }

    /** A regexp pattern to validate tags. */
    protected static final Pattern validTag =
        Pattern.compile("[a-z](_?[a-z0-9]){2,18}");

    /** Maps byte type ids to repository for all digital item types. */
    protected HashMap<Byte, ItemRepository<ItemRecord>> _repos =
        new HashMap<Byte, ItemRepository<ItemRecord>>();
}
