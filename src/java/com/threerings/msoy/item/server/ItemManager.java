//
// $Id$

package com.threerings.msoy.item.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.RepositoryListenerUnit;
import com.samskivert.util.Invoker;
import com.samskivert.util.LRUHashMap;
import com.samskivert.util.ResultListener;
import com.samskivert.util.Tuple;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.server.MsoyServer;

import static com.threerings.msoy.Log.log;

/**
 * Manages digital items and their underlying repositories.
 */
public class ItemManager
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

        // and insert the item; notifying the listener on success or failure
        MsoyServer.invoker.postUnit(
            new RepositoryListenerUnit<ArrayList<Item>>(rlist) {
            public ArrayList<Item> invokePersistResult ()
                throws PersistenceException {
                return repo.loadItems(memberId);
            }
            public void handleSuccess () {
                _itemCache.put(key, _result);
                super.handleSuccess();
            }
        });
    }

    /** Maps string identifier to repository for all digital item types. */
    protected HashMap<ItemEnum,ItemRepository<Item>> _repos = new
        HashMap<ItemEnum,ItemRepository<Item>>();

    /** TEMP: a cache of item list indexed on (user,type). This will soon be
     * replaced with our fancy cache. */
    protected LRUHashMap<Tuple<Integer,ItemEnum>,ArrayList<Item>> _itemCache =
        new LRUHashMap<Tuple<Integer,ItemEnum>,ArrayList<Item>>(100);
}
