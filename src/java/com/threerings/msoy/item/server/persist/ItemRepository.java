//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
import com.samskivert.jdbc.Repository.Operation;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class
    ItemRepository<T extends Item>
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

    /**
     * Loads all items in the catalog.
     * TODO: As soon as we're out of the prototyping stage, this will need
     * to turn into a paging method; int offset, int rows perhaps?
     * TODO: This should be a single join over two tables, but there is no
     * way to coerce the current ORM system into doing that, so we iterate &
     * lookup, as placeholder until that day (or until we decide it's the
     * kind of thing that just needs an explicit query).
     * TODO: need a powerful way to supply search criteria.
     */
    public ArrayList<CatalogListing> loadCatalog ()
        throws PersistenceException
    {
        return execute(new Operation<ArrayList<CatalogListing>>() {
            public ArrayList<CatalogListing> invoke (
                Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ArrayList<CatalogListing> list =
                    new ArrayList<CatalogListing> ();
                Statement stmt = conn.createStatement();
                try {
                    ResultSet rs = stmt.executeQuery(
                        "  select ITEM_ID, LISTED_DATE " +
                        "    from " + getCatalogTableName());
                    while (rs.next()) {
                        int itemId = rs.getInt(1);
                        Date listedDate = rs.getDate(2);
                        CatalogListing listing = new CatalogListing();
                        listing.item = loadItem(itemId);
                        listing.listedDate = listedDate;
                        list.add(listing);
                    }

                } finally {
                    JDBCUtil.close(stmt);
                }
                return list;
            }
        });
    }

    /**
     * Perform the low level operations involved with listing an item in
     * the catalog: create a new, immutable version of the item, insert it
     * into the item table, then create a row in the catalog table.
     */
    public T listItem (int itemId)
        throws PersistenceException
    {
        // load the item being listed
        final T item = loadItem(itemId);
        // items in the catalog don't have an owner
        item.ownerId = -1;
        // create a new row for the new item, giving the object a new id
        insertItem(item);
        // finally add it to the actual catalog
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "insert into " + getCatalogTableName() + " " +
                        "        set ITEM_ID = ?," +
                        "            LISTED_DATE = ?");
                    stmt.setInt(1, item.itemId);
                    stmt.setDate(2, new Date(System.currentTimeMillis()));
                    stmt.executeUpdate();
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
        return item;
    }
    
    /**
     * Perform the low level operations involved with purchasing an item:
     * instantiating the immutable item, making a new clone row, returning
     * the tweaked instance.
     */
    public T purchaseItem (int memberId, int itemId)
        throws PersistenceException
    {
        // load the item being purchased
        T item = loadItem(itemId);
        // sanity check it
        if (item.ownerId != -1) {
            throw new PersistenceException(
                "Can't purchase item with owner [itemId=" + itemId + "]");
        }
        // insert a new clone row for us
        cloneItem(itemId, memberId);
        // and mark the instance as ours
        item.ownerId = memberId;
        return item;
    }

    /** Insert an item clone into the database with the given owner. */
    protected void cloneItem (final int itemId, final int ownerId)
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
                    stmt.setInt(1, itemId);
                    stmt.setInt(2, ownerId);
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

    /** Returns the name of the table returned by getTable(). */
    protected abstract String getCatalogTableName ();

    /**
     * Returns the name of the _CLONES table associated with the main table.
     */
    protected abstract String getCloneTableName ();
}
