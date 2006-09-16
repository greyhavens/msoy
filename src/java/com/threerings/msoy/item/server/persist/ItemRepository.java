//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.Item;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<T extends Item>
    extends JORARepository
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
        ArrayList<T> items =
            loadAll(getTable(), "where OWNER_ID = " + ownerId);

        String cloneTableName = getCloneTableName();
        ArrayList<T> clones =
            loadAll(getTable(), cloneTableName,
                " where " + cloneTableName + ".OWNER_ID = " + ownerId +
                "   and ITEM_ID = ORIGINAL_ITEM_ID ");
        // we must explicitly set parentId and ownerId for each clone
        for (T clone : clones) {
            clone.ownerId = ownerId;
            clone.parentId = clone.itemId;
            clone.itemId = -1; // TODO: give clones their own item id
        }
        items.addAll(clones);
        return items;
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

    /** Insert an item clone into the database with the given owner. */
    public void cloneItem (T item, int ownerId)
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "insert into " + getCloneTableName() + " " +
                        "        set ORIGINAL_ITEM_ID = ?," +
                        "            OWNER_ID = ?");
                    stmt.executeUpdate();
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
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
    
    /**
     * Returns the name of the _CLONES table associated with the main table.
     */
    protected abstract String getCloneTableName ();
}
