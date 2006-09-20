//
// $Id$

package com.threerings.msoy.item.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
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

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.util.ItemEnum;

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
    public void init (ConnectionProvider conProv)
        throws PersistenceException
    {
        // create our item repositories
        for (ItemEnum itype : ItemEnum.values()) {
            if (itype.equals(ItemEnum.UNUSED)) {
                continue;
            }

            // do some magic to determine the name of the ItemRepository
            // derived class based on the Item classname
            String iclass = itype.getItemClass().getName();
            iclass = iclass.substring(iclass.lastIndexOf(".")+1);
            String repclass = ItemRepository.class.getName();
            int didx = repclass.lastIndexOf(".");
            repclass = repclass.substring(0, didx+1) + iclass +
                // skip the Item in ItemRepository
                repclass.substring(didx+5);

            // create and initialize this repository
            try {
                @SuppressWarnings("unchecked") ItemRepository<Item> irepo =
                    (ItemRepository<Item>)Class.forName(repclass).newInstance();
                irepo.init(conProv);
                _repos.put(itype, irepo);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to prepare item repository " +
                        "[class=" + repclass + "].", e);
            }
        }

        // register our invocation service
        MsoyServer.invmgr.registerDispatcher(new ItemDispatcher(this), true);
    }

    // from ItemProvider
    public void getInventory (
        ClientObject caller, String type,
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
        loadInventory(memberObj.getMemberId(), etype,
            new ResultListener<ArrayList<Item>>() {
                public void requestCompleted (ArrayList<Item> result) {
                    Item[] items = new Item[result.size()];
                    result.toArray(items);
                    listener.requestProcessed(items);
                }

                public void requestFailed (Exception cause) {
                    log.warning("Unable to retrieve inventory " +
                        "[cause=" + cause + "].");
                    listener.requestFailed(InvocationCodes.INTERNAL_ERROR);
                }
            });
    }

    /**
     * Inserts the supplied item into the system. The item should be fully
     * configured, and an item id will be assigned during the insertion
     * process. Success or failure will be communicated to the supplied result
     * listener.
     */
    public void insertItem (final Item item, ResultListener<Item> rlist)
    {
        // map this items type back to an enum
        ItemEnum type = ItemEnum.valueOf(item.getType());
        if (type == null) {
            rlist.requestFailed(
                new Exception("Unknown item type '" + item.getType() + "'."));
            return;
        }

        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(new RepositoryListenerUnit<Item>(rlist) {
            public Item invokePersistResult () throws PersistenceException {
                repo.insertItem(item);
                return item;
            }
            public void handleSuccess () {
                super.handleSuccess();
                // add the item to the user's cached inventory
                updateUserCache(item);
            }
        });
    }

    /**
     * Loads up the inventory of items of the specified type for the specified
     * member. The results may come from the cache and will be cached after
     * being loaded from the database.
     */
    public void loadInventory (final int memberId, ItemEnum type,
                               ResultListener<ArrayList<Item>> rlist)
    {
        // first check the cache
        final Tuple<Integer,ItemEnum> key =
            new Tuple<Integer,ItemEnum>(memberId, type);
        ArrayList<Item> items = _itemCache.get(key);
        if (items != null) {
            rlist.requestCompleted(items);
            return;
        }

        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }

        // and load their items; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(rlist) {
            public ArrayList<Item> invokePersistResult ()
                throws PersistenceException {
                ArrayList<Item> list = repo.loadOriginalItems(memberId);
                list.addAll(repo.loadClonedItems(memberId));
                return list;
            }
            public void handleSuccess () {
                _itemCache.put(key, _result);
                super.handleSuccess();
            }
        });
    }

    /**
     * Fetches the entire catalog of listed items of the given type.
     */
    public void loadCatalog (
            int memberId, ItemEnum type,
            ResultListener<ArrayList<CatalogListing>> rlist)
    {
        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }

        // and load the catalog
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<CatalogListing>>(rlist) {
            public ArrayList<CatalogListing> invokePersistResult ()
                throws PersistenceException {
                return repo.loadCatalog();
            }
        });
    }

    /**
     * Purchases a given item for a given member from the catalog by
     * creating a new clone row in the appropriate database table.
     */
    public void purchaseItem (
            final int memberId, final int itemId, ItemEnum type,
            ResultListener<Item> rlist)
    {
        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }

        // and perform the purchase
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(rlist) {
            public Item invokePersistResult () throws PersistenceException {
                // load the item being purchased
                Item item = repo.loadItem(itemId);
                // sanity check it
                if (item.ownerId != -1) {
                    throw new PersistenceException(
                        "Can't purchase unlisted item [itemId=" + itemId + "]");
                }
                // create the row in the database!
                repo.insertClone(item, memberId);
                // and finally mark it as ours
                item.ownerId = memberId;
                return item;
            }
        });
    }
    
    /**
     * Lists the given item in the catalog by creating a new item row and
     * a new catalog row and returning the immutable form of the item.
     */

    public void listItem (
            final int itemId, ItemEnum type, ResultListener<Item> rlist)
    {
        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }
        
        // and perform the listing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(rlist) {
            public Item invokePersistResult () throws PersistenceException {
                // load the original item
                Item listItem = repo.loadItem(itemId);
                if (listItem == null) {
                    throw new PersistenceException(
                        "Can't find object to list [itemId = " + itemId + "]");
                }
//                if (listItem.parentId != -1) {
//                    throw new PersistenceException(
//                        "Can't list a cloned object [itemId=" + itemId + "]");
//                }
                if (listItem.ownerId == -1) {
                    throw new PersistenceException(
                        "Object is already listed [itemId=" + itemId + "]");
                }
                // and reset the owner
                listItem.ownerId = -1;
                // then insert it into the catalog
                return repo.insertIntoCatalog(listItem);
            }
        });
    }
    
    /**
     * Remix a clone, turning it back into a full-featured original.
     */
    public void remixItem(
            final int itemId, String type, ResultListener<Item> rlist)
    {
        // locate the appropriate repository
        final ItemRepository<Item> repo = _repos.get(type);
        if (repo == null) {
            rlist.requestFailed(
                new Exception("No repository registered for " + type + "."));
            return;
        }

        // and perform the remixing
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<Item>(rlist) {
            public Item invokePersistResult () throws PersistenceException {
                // load a copy of the clone to modify
                Item item = repo.loadClone(itemId);
                // make it ours
                item.creatorId = item.ownerId;
                // forget whence it came
                item.parentId = -1;
                // insert it as a genuinely new item
                repo.insertItem(item);
                // and finally delete the old clone
                repo.deleteClone(itemId);
                return item;
            }
        });

    }

    /**
     * Called when an item is newly created and should be inserted into the
     * owning user's inventory cache.
     */
    protected void updateUserCache (Item item)
    {
        ItemEnum type = ItemEnum.valueOf(item.getType());
        if (type == null) {
            log.warning("Item reported invalid type '" + item + "': " +
                        item.getType() + ".");
            return;
        }

        ArrayList<Item> items = _itemCache.get(
            new Tuple<Integer,ItemEnum>(item.ownerId, type));
        if (items != null) {
            items.add(item);
        }
    }

    /** Maps string identifier to repository for all digital item types. */
    protected HashMap<ItemEnum,ItemRepository<Item>> _repos = new
        HashMap<ItemEnum,ItemRepository<Item>>();

    /** A soft reference cache of item list indexed on (user,type). */
    protected SoftCache<Tuple<Integer,ItemEnum>,ArrayList<Item>> _itemCache =
        new SoftCache<Tuple<Integer,ItemEnum>,ArrayList<Item>>();
}
