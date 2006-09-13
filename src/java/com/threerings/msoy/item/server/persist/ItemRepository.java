//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.Item;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<T extends Item> extends JORARepository
{
    /**
     * Creates an uninitialized item repository. {@link #init} must
     * subsequently be called to prepare it for operation.
     */
    public ItemRepository ()
    {
        super(null, null);
    }

    /**
     * Initializes this item repository and connects to its database server.
     */
    public void init (ConnectionProvider provider)
    {
        _provider = provider;
        configureDatabaseIdent(getDatabaseIdent());
    }

    /**
     * Loads all items owned by the specified member.
     */
    public ArrayList<T> loadItems (int ownerId)
        throws PersistenceException
    {
        return loadAll(getTable(), "where OWNER_ID = " + ownerId);
    }

    /**
     * Loads an item with the specified identifier.
     */
    public T loadItem (int itemId)
        throws PersistenceException
    {
        return load(getTable(), "where ITEM_ID = " + itemId);
    }

    /**
     * Inserts the supplied item into the database. {@link Item#itemId} will be
     * filled in as a result of this call.
     */
    public void insertItem (T item)
        throws PersistenceException
    {
        item.itemId = insert(getTable(), item);
    }

    /**
     * Returns the database identifier for this item's database. The default is
     * <code>itemdb</code> but if we need to partition our item tables across
     * databases we can override this method on a per-type basis.
     */
    protected String getDatabaseIdent ()
    {
        return "itemdb";
    }

    /** Returns the table via which we can manipulate the item table. */
    protected abstract Table<T> getTable ();
}
