//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.JORARepository;
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
     * Loads all original items owned by the specified member.
     */
    public ArrayList<T> loadOriginalItems (int ownerId)
        throws PersistenceException
    {
        return loadAll(getTable(), "where OWNER_ID = " + ownerId);
    }
    
    /**
     * Loads all cloned items owned by the specified member.
     */
    public ArrayList<T> loadClonedItems (final int ownerId)
        throws PersistenceException
    {
        // TODO: this is a crazy inefficient way to do what will hopefully
        // TODO: be a simple join in the new ORM system -- I left it like this
        // TODO: because it impacts the rest of the architecture the least
        return execute(new Operation<ArrayList<T>>() {
            public ArrayList<T> invoke (
                Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                ArrayList<T> list = new ArrayList<T> ();
                PreparedStatement query = null;
                try {
                    query = conn.prepareStatement(
                        "  select ITEM_ID, ORIGINAL_ITEM_ID " +
                        "    from " + (getTypeEponym() + "_CLONES ") +
                        "   where OWNER_ID = ?");
                    query.setInt(1, ownerId);
                    ResultSet rs = query.executeQuery();
                    while (rs.next()) {
                        int itemId = rs.getInt(1);
                        int originalItemId = rs.getInt(2);
    
                        // now load the original item and make it ours
                        T item = loadItem(originalItemId);
                        item.parentId = originalItemId;
                        item.itemId = itemId;
                        item.ownerId = ownerId;
                        list.add(item);
                    }
    
                } finally {
                    JDBCUtil.close(query);
                }
                return list;
            }
        });
    }

    /**
     * Loads the clone with the given ID.
     */
    public T loadClone (final int cloneId)
        throws PersistenceException
    {
        return execute(new Operation<T>() {
            public T invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement query = null;
                try {
                    query = conn.prepareStatement(
                        "  select ORIGINAL_ITEM_ID, OWNER_ID " +
                        "    from " + (getTypeEponym() + "_CLONES ") +
                        "   where ITEM_ID = ?");
                    query.setInt(1, cloneId);
                    ResultSet rs = query.executeQuery();
                    if (!rs.next()) {
                        throw new PersistenceException(
                            "No such clone [itemId=" + cloneId + "]");
                    }
                    T item = loadItem(rs.getInt(1));
                    item.parentId = item.itemId;
                    item.itemId = cloneId;
                    item.ownerId = rs.getInt(2);
                    return item;
                    
                } finally {
                    JDBCUtil.close(query);
                }
            }
        });
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
        item.itemId = getNextId();
        insert(getTable(), item);
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
                        "    from " + (getTypeEponym() + "_CATALOG"));
                    while (rs.next()) {
                        int itemId = rs.getInt(1);
                        Timestamp listedDate = rs.getTimestamp(2);
                        CatalogListing listing = new CatalogListing();
                        listing.item = loadItem(itemId);
                        // create a java.util.Date so GWT doesn't flip out
                        listing.listedDate = new Date(listedDate.getTime());
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
     * the catalog: create a row in the item table for a new, immutable
     * version of the item, then an associated row in the catalog table.
     */
    public T insertIntoCatalog (final T item)
        throws PersistenceException
    {
        if (item.ownerId != -1) {
            throw new PersistenceException(
                "Can't list owned item [ownerId=" + item.ownerId + "]");
        }
        // create a new row for the new item, giving the object a new id
        insertItem(item);
        // and add it to the actual catalog
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "insert into " + (getTypeEponym() + "_CATALOG") + " " +
                        "        set ITEM_ID = ?," +
                        "            LISTED_DATE = ?");
                    stmt.setInt(1, item.itemId);
                    stmt.setTimestamp(
                        2, new Timestamp(System.currentTimeMillis()));
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
     * Insert an item clone into the database with the given owner.
     * This fills itemId with the next available unique ID and ownerId
     * with the supplied value for the new owner.
     * */
    public void insertClone (final Item item, final int newOwnerId)
        throws PersistenceException
    {
        if (item.parentId != -1) {
            throw new PersistenceException(
                "Can't clone a clone [itemId=" + item.itemId + "]");
        }
        if (item.ownerId != -1) {
            throw new PersistenceException(
                "Can only clone listed items [itemId=" + item.itemId + "]");
        }
        final int cloneId = getNextId();
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "insert into " + (getTypeEponym() + "_CLONES") + " " +
                        "        set ITEM_ID = ?," +
                        "            ORIGINAL_ITEM_ID = ?," +
                        "            OWNER_ID = ?");
                    stmt.setInt(1, cloneId);
                    stmt.setInt(2, item.itemId);
                    stmt.setInt(3, newOwnerId);
                    stmt.executeUpdate();
                    item.ownerId = newOwnerId;
                    item.itemId = cloneId;
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /** Delete an item clone from the database */
    public void deleteClone (final int cloneId)
        throws PersistenceException
    {
        executeUpdate(new Operation<Void>() {
            public Void invoke (Connection conn, DatabaseLiaison liaison)
                throws SQLException, PersistenceException
            {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        "delete from " + (getTypeEponym() + "_CLONES") + " " +
                        "      where ITEM_ID = ?");
                    stmt.setInt(1, cloneId);
                    stmt.executeUpdate();
                    
                } finally {
                    JDBCUtil.close(stmt);
                }
                return null;
            }
        });
    }

    /**
     * Fetch the next available ID from the sequence table, atomically
     * increasing the ID by one. Each item type has a row with a TYPE
     * value set to FURNITURE, PHOTO, etc; see getTypeEponym().
     */
    protected int getNextId ()
        throws PersistenceException
    {
        return executeUpdate(new Operation<Integer>() {
            public Integer invoke (
                    Connection conn, DatabaseLiaison liaison)
            throws SQLException, PersistenceException
            {
                PreparedStatement select = null;
                PreparedStatement update = null;
                try {
                    select = conn.prepareStatement(
                        "     select NEXT_ID " +
                        "       from " + getTypeEponym() + "_IDSEQ " +
                        " for update ");
                    update = conn.prepareStatement(
                        "  update " + getTypeEponym() + "_IDSEQ " +
                        "     set NEXT_ID = ?");
                    ResultSet rs = select.executeQuery();
                    if (!rs.next()) {
                        throw new PersistenceException(
                            "Couldn't find next unique ID [tableId=" +
                            getTypeEponym() + "]");
                    }
                    int val = rs.getInt(1);
                    update.setInt(1, val + 1);
                    update.executeUpdate();
                    return Integer.valueOf(val);
                } finally {
                    JDBCUtil.close(select);
                    JDBCUtil.close(update);
                }
            }
        });
    }

    @Override // from JORARepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        super.migrateSchema(conn, liaison);

        if (!JDBCUtil.tableExists(conn, getTypeEponym() + "_IDSEQ")) {
            JDBCUtil.createTableIfMissing(
                conn, getTypeEponym() + "_IDSEQ", new String[] {
                    "NEXT_ID integer not null",
                }, "");
            conn.createStatement().execute(
                "insert into " + getTypeEponym() + "_IDSEQ " +
                "        set NEXT_ID = 1");
        }

        JDBCUtil.createTableIfMissing(
            conn, (getTypeEponym() + "_CLONES"), new String[] {
                "ITEM_ID integer not null primary key",
                "ORIGINAL_ITEM_ID integer not null",
                "OWNER_ID integer not null",
            }, "");
        JDBCUtil.createTableIfMissing(
            conn, (getTypeEponym() + "_CATALOG"), new String[] {
                "ITEM_ID integer not null",
                "LISTED_DATE datetime not null",
            }, "");
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
     * Returns the name of the _CATALOG table associated with the main table.
     */
    protected abstract String getTypeEponym ();

}
