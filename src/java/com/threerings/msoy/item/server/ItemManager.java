//
// $Id$

package com.threerings.msoy.item.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.LRUHashMap;
import com.samskivert.util.Tuple;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.util.ItemEnum;

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
                ItemRepository irepo = (ItemRepository)
                    Class.forName(repclass).newInstance();
                irepo.init(conProv);
                _repos.put(itype.toString(), irepo);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to prepare item repository " +
                        "[class=" + repclass + "].", e);
            }
        }
    }

    /** Maps string identifier to repository for all digital item types. */
    protected HashMap<String,ItemRepository> _repos = new
        HashMap<String,ItemRepository>();

    /** TEMP: a cache of item list indexed on (user,type). This will soon be
     * replaced with our fancy cache. */
    protected LRUHashMap<Tuple<Integer,String>,ArrayList<Item>> _itemCache =
        new LRUHashMap<Tuple<Integer,String>,ArrayList<Item>>(100);
}
