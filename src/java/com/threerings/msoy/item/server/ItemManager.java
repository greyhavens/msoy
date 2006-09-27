//
// $Id$

package com.threerings.msoy.item.server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.ResultListener;
import com.samskivert.util.SoftCache;
import com.samskivert.util.Tuple;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.server.InvocationException;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.DocumentRepository;
import com.threerings.msoy.item.server.persist.FurnitureRepository;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.PhotoRepository;
import com.threerings.msoy.item.server.persist.RatingRecord;
import com.threerings.msoy.item.server.persist.TagHistoryRecord;
import com.threerings.msoy.item.server.persist.TagNameRecord;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.TagHistory;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
public class ItemManager
    implements ItemProvider
{
    /**
     * Initializes the item manager, which will establish database connections
     * for all of its item repositories.
     */
    @SuppressWarnings("unchecked")
    public void init (ConnectionProvider conProv) throws PersistenceException
    {
        _repos.put(ItemEnum.DOCUMENT,
            (ItemRepository) new DocumentRepository(conProv));
        _repos.put(ItemEnum.FURNITURE,
            (ItemRepository) new FurnitureRepository(conProv));
        _repos.put(ItemEnum.GAME,
            (ItemRepository) new GameRepository(conProv));
        _repos.put(ItemEnum.PHOTO,
            (ItemRepository) new PhotoRepository(conProv));

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), true);
    }

    // from ItemProvider
    public void getInventory (ClientObject caller, String type,
            final InvocationService.ResultListener listener)
        throws InvocationException
    {
        MemberObject memberObj = (MemberObject) caller;
        if (memberObj.isGuest()) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
        // go ahead and throw a RuntimeException if 'type' is bogus
        ItemEnum etype = Enum.valueOf(ItemEnum.class, type);

        // then, load that type
        // TODO: not everything!
        loadInventory(
            memberObj.getMemberId(), etype,
            new ResultListener<ArrayList<Item>>() {
                public void requestCompleted (ArrayList<Item> result)
                {
                    Item[] items = new Item[result.size()];
                    result.toArray(items);
                    listener.requestProcessed(items);
                }

                public void requestFailed (Exception cause)
                {
                    log.warning("Unable to retrieve inventory " + "[cause="
                        + cause + "].");
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            });
    }

    /**
     * Get the specified item.
     */
    public void getItem (
        ItemEnum type, final int itemId, ResultListener<Item> listener)
    {
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            listener.requestFailed(new Exception("No repository registered " +
                "for " + type + "."));
            return;
        }

        // TODO: do we have to check cloned items as well?
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(listener) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                return repo.loadItem(itemId).toItem();
            }
        });
    }

    /**
     * Inserts the supplied item into the system. The item should be fully
     * configured, and an item id will be assigned during the insertion
     * process. Success or failure will be communicated to the supplied result
     * listener.
     */
    public void insertItem (final Item item, ResultListener<Item> waiter)
    {
        final ItemRecord record = ItemRecord.newRecord(item); 
        ItemEnum type = record.getType();

        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(
            waiter) {
            public Item invokePersistResult ()
                throws PersistenceException
            {
                repo.insertItem(record);
                item.itemId = record.itemId;
                return item;
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(record);
            }
        });
    }

    /**
     * Loads up the inventory of items of the specified type for the specified
     * member. The results may come from the cache and will be cached after
     * being loaded from the database.
     */
    public void loadInventory (final int memberId, ItemEnum type,
            ResultListener<ArrayList<Item>> waiter)
    {
        // first check the cache
        final Tuple<Integer, ItemEnum> key =
            new Tuple<Integer, ItemEnum>(memberId, type);
//      TODO: Disable cache for the moment
        if (false) {
        Collection<ItemRecord> items = _itemCache.get(key);
        if (items != null) {
            ArrayList<Item> list = new ArrayList<Item>();
            for (ItemRecord record : items) {
                list.add(record.toItem());
            }
            waiter.requestCompleted(list);
            return;
        }
        }
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and load their items; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(waiter) {
                    public ArrayList<Item> invokePersistResult ()
                        throws PersistenceException
                    {
                        Collection<ItemRecord> list =
                            repo.loadOriginalItems(memberId);
                        list.addAll(repo.loadClonedItems(memberId));
                        ArrayList<Item> newList = new ArrayList<Item>();
                        for (ItemRecord record : list) {
                            newList.add(record.toItem());
                        }
                        return newList;
                    }

                    public void handleSuccess ()
                    {
// TODO: The cache needs some rethinking, I figure.
//                        _itemCache.put(key, _result);
                        super.handleSuccess();
                    }
                });
    }

    /**
     * Fetches the entire catalog of listed items of the given type.
     */
    public void loadCatalog (int memberId, ItemEnum type,
            ResultListener<ArrayList<CatalogListing>> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and load the catalog
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<CatalogListing>>(waiter) {
                public ArrayList<CatalogListing> invokePersistResult ()
                    throws PersistenceException
                {
                    ArrayList<CatalogListing> list =
                        new ArrayList<CatalogListing>();
                    for (CatalogRecord record : repo.loadCatalog()) {
                        list.add(record.toListing());
                    }
                    return list;
                }
            });
    }

    /**
     * Purchases a given item for a given member from the catalog by
     * creating a new clone row in the appropriate database table.
     */
    public void purchaseItem (final int memberId, final int itemId,
            ItemEnum type, ResultListener<Item> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and perform the purchase
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(waiter) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load the item being purchased
                ItemRecord item = repo.loadItem(itemId);
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
        });
    }

    /**
     * Lists the given item in the catalog by creating a new item row and
     * a new catalog row and returning the immutable form of the item.
     */
    public void listItem (final int itemId, ItemEnum type,
            ResultListener<CatalogListing> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }

        // and perform the listing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<CatalogListing>(waiter) {
            public CatalogListing invokePersistResult ()
                throws PersistenceException
            {
                // load a copy of the original item
                ItemRecord listItem = repo.loadItem(itemId);
                if (listItem == null) {
                    throw new PersistenceException(
                        "Can't find object to list [itemId = " + itemId + "]");
                }
                if (listItem.ownerId == -1) {
                    throw new PersistenceException(
                        "Object is already listed [itemId=" + itemId + "]");
                }
                // reset the owner
                listItem.ownerId = -1;
                // and the iD
                listItem.itemId = 0;
                // then insert it as the immutable copy we list
                repo.insertItem(listItem);
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
    public void remixItem (
            final int itemId, ItemEnum type, ResultListener<Item> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(waiter) {
            public Item invokePersistResult () throws PersistenceException
            {
                // load a copy of the clone to modify
                _item = repo.loadClone(itemId);
                // TODO: make sure we should not use the original creator here
                // make it ours
                _item.creatorId = _item.ownerId;
                // let the object forget whence it came
                int originalId = _item.parentId;
                _item.parentId = -1;
                // insert it as a genuinely new item
                _item.itemId = 0;
                repo.insertItem(_item);
                // delete the old clone
                repo.deleteClone(itemId);
                // copy tags from the original to the new item
                repo.copyTags(
                    originalId, _item.itemId, _item.ownerId,
                    System.currentTimeMillis());
                return _item.toItem();
            }

            public void handleSuccess ()
            {
                super.handleSuccess();
                // add the item to the user's cached inventory
//                updateUserCache(_item);
            }

            protected ItemRecord _item;
        });

    }

    /** Fetch the rating a user has given an item, or 0. */
    public void getRating (
        final int itemId, ItemEnum type, final int memberId,
        ResultListener<Byte> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Byte>(waiter) {
                public Byte invokePersistResult () throws PersistenceException {
                    RatingRecord<ItemRecord> record =
                        repo.getRating(itemId, memberId);
                    return record != null ? record.rating : 0;
                }
            });
    }
    
    /** Fetch the tagging history for a given item. */
    public void getTagHistory (
        final int itemId, ItemEnum type,
        ResultListener<Iterable<TagHistory>> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Iterable<TagHistory>>(waiter) {
                public Iterable<TagHistory> invokePersistResult ()
                        throws PersistenceException {
                    HashMap<Integer, MemberRecord> memberCache =
                        new HashMap<Integer, MemberRecord>();
                    ArrayList<TagHistory> list = new ArrayList<TagHistory>();
                    for (TagHistoryRecord<ItemRecord> record :
                            repo.getTagHistory(itemId)) {
                        // we should probably go through/cache in MemberManager
                        MemberRecord memRec = memberCache.get(record.memberId);
                        if (memRec == null) {
                            memRec = MsoyServer.memberRepo.loadMember(
                                record.memberId);
                            memberCache.put(record.memberId, memRec);
                        }
                        TagNameRecord tag = repo.getTag(record.tagId);
                        TagHistory history = new TagHistory();
                        history.itemId = record.itemId;
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

    /** Let a member rate an object. */
    public void rateItem (
        final int itemId, ItemEnum type, final int memberId,
        final byte rating, ResultListener<Item> waiter)
    {
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(waiter) {
                public Item invokePersistResult () throws PersistenceException {
                    ItemRecord item = repo.loadItem(itemId);
                    int originalId;
                    if (item == null) {
                        item = repo.loadClone(itemId);
                        if (item == null) {
                            throw new PersistenceException(
                                "Can't find item [itemId=" + itemId + "]");
                        }
                        originalId = item.parentId;
                    } else {
                        // make sure we're not trying to rate a mutable
                        if (item.ownerId != -1) {
                            throw new PersistenceException(
                                "Can't rate mutable object [itemId=" +
                                itemId + "]");
                        }
                        originalId = itemId;
                    }
                    item.rating = repo.rateItem(originalId, memberId, rating);
                    return item.toItem();
                }
            });

    }

    /** Add the specified tag to the specified item. Return a tag history
     *  object if the tag did not already exist. */
    public void tagItem (
        int itemId, ItemEnum type, int taggerId, String tagName,
        ResultListener<TagHistory> waiter)
    {
        itemTagging(
            itemId, type, taggerId, tagName.trim().toLowerCase(),
            waiter, true);
    }

    /** Remove the specified tag from the specified item. Return a tag history
     *  object if the tag existed. */
    public void untagItem (
        int itemId, ItemEnum type, int taggerId, String tagName,
        ResultListener<TagHistory> waiter)
    {
        itemTagging(
            itemId, type, taggerId, tagName.trim().toLowerCase(),
            waiter, false);
    }

    // do the facade work for tagging
    protected void itemTagging (
        final int itemId, ItemEnum type, final int taggerId,
        final String tagName, ResultListener<TagHistory> waiter,
        final boolean doTag)
    {
        if (!validTag.matcher(tagName).matches()) {
            waiter.requestFailed(new IllegalArgumentException(
                "Invalid tag [tag=" + tagName + "]"));
            return;
        }
        // locate the appropriate repository
        final ItemRepository<ItemRecord> repo = _repos.get(type);
        if (repo == null) {
            waiter.requestFailed(new Exception("No repository registered for "
                + type + "."));
            return;
        }
        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<TagHistory>(waiter) {
                public TagHistory invokePersistResult ()
                        throws PersistenceException {
                    long now = System.currentTimeMillis();

                    ItemRecord item = repo.loadItem(itemId);
                    int originalId;
                    if (item == null) {
                        // it's probably a clone
                        item = repo.loadClone(itemId);
                        if (item == null) {
                            throw new PersistenceException(
                                "Can't find item [itemId=" + itemId + "]");
                        }
                        // in which case we fetch the original
                        originalId = item.parentId;
                    } else {
                        originalId = itemId;
                    }
                    // map tag to tag id
                    TagNameRecord tag = repo.getTag(tagName);
                    // do the actual work
                    TagHistoryRecord<ItemRecord> historyRecord = doTag ? 
                        repo.tagItem(originalId, tag.tagId, taggerId, now) :
                        repo.untagItem(originalId, tag.tagId, taggerId, now);

                    // finally look up the member
                    MemberRecord member = MsoyServer.memberRepo.loadMember(
                        historyRecord.memberId);
                    // and create the return value
                    TagHistory history = new TagHistory();
                    history.itemId = originalId;
                    history.member = member.getName();
                    history.tag = tag.tag;
                    history.action = historyRecord.action;
                    history.time = new Date(historyRecord.time.getTime());
                    return history;
                }
        });
    }

    /**
     * Called when an item is newly created and should be inserted into the
     * owning user's inventory cache.
     */
    protected void updateUserCache (ItemRecord item)
    {
        ItemEnum type = item.getType();
        Collection<ItemRecord> items =
            _itemCache.get(new Tuple<Integer, ItemEnum>(item.ownerId, type));
        if (items != null) {
            items.add(item);
        }
    }

    /** A regexp pattern to validate tags. */
    protected static final Pattern validTag =
        Pattern.compile("[a-z](_?[a-z0-9]){2,59}");

    /** Maps string identifier to repository for all digital item types. */
    protected HashMap<ItemEnum, ItemRepository<ItemRecord>> _repos =
        new HashMap<ItemEnum, ItemRepository<ItemRecord>>();

    /** A soft reference cache of item list indexed on (user,type). */
    protected SoftCache<Tuple<Integer, ItemEnum>, Collection<ItemRecord>> _itemCache =
        new SoftCache<Tuple<Integer, ItemEnum>, Collection<ItemRecord>>();
}
