//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<T extends ItemRecord>
    extends DepotRepository
{
    public ItemRepository (ConnectionProvider provider)
    {
        // we'll be using one persistence context per ItemRecord type
        super(provider);
    }

    @Override
    protected String getIdent ()
    {
        return getDatabaseIdent();
    }

    /**
     * Loads all original items owned by the specified member.
     */
    public Collection<T> loadOriginalItems (int ownerId)
        throws PersistenceException
    {
        return findAll(getItemClass(), new Key(ItemRecord.OWNER_ID, ownerId));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public Collection<T> loadClonedItems (int ownerId)
        throws PersistenceException
    {
        Collection<? extends CloneRecord<?>> cloneRecords =
            findAll(getCloneClass(), new Key(CloneRecord.OWNER_ID, ownerId));
        Collection<T> cloneItems = new ArrayList<T>();
        for (CloneRecord<?> record : cloneRecords) {
            T item = loadItem(record.originalItemId);
            item.itemId = record.itemId;
            item.parentId = record.originalItemId;
            item.ownerId = ownerId;
            cloneItems.add(item);
        }
        return cloneItems;
    }

    /**
     * Loads the clone with the given ID.
     */
    public ItemRecord loadClone (int cloneId) throws PersistenceException
    {
        CloneRecord<?> cloneRecord = load(getCloneClass(), cloneId);
        ItemRecord clone = loadItem(cloneRecord.originalItemId);
        clone.parentId = clone.itemId;
        clone.itemId = cloneRecord.itemId;
        clone.ownerId = cloneRecord.ownerId;
        return clone;
    }

    /**
     * Loads an item with the specified identifier.
     */
    public T loadItem (int itemId) throws PersistenceException
    {
        return load(getItemClass(), itemId);
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
    public Collection<? extends CatalogRecord<T>> loadCatalog ()
        throws PersistenceException
    {
        Collection<? extends CatalogRecord<T>> records =
            findAll(getCatalogClass());
        for (CatalogRecord<T> record : records) {
            record.item = loadItem(record.itemId);
        }
        return records;
    }


    /**
     * Inserts the supplied item into the database. {@link Item#itemId} will be
     * filled in as a result of this call.
     */
    public void insertItem (T item) throws PersistenceException
    {
        if (item.itemId != 0) {
            throw new PersistenceException(
                "Can't insert item with existing key [itemId=" + item.itemId
                    + "]");
        }
        insert(item);
    }

    /**
     * Perform the low level operations involved with listing an item in
     * the catalog: create a row in the item table for a new, immutable
     * version of the item, then an associated row in the catalog table.
     * TODO: this method modifies the input value, totally unintuitive!
     */
    public CatalogRecord insertListing (
            ItemRecord listItem, Timestamp listedDate)
        throws PersistenceException
    {
        if (listItem.ownerId != -1) {
            throw new PersistenceException(
                "Can't list item with owner [itemId=" + listItem.itemId + "]");
        }
        CatalogRecord<T> record;
        try {
            record = getCatalogClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        record.item = listItem;
        record.itemId = listItem.itemId;
        record.listedDate = listedDate;
        // and insert it - done!
        insert(record);
        return record;
    }

    /**
     * Insert an item clone into the database with the given owner.
     * This fills itemId with the next available unique ID and ownerId
     * with the supplied value for the new owner.
     * */
    public int insertClone (int itemId, int newOwnerId)
        throws PersistenceException
    {
        CloneRecord<?> record;
        try {
            record = getCloneClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
        record.originalItemId = itemId;
        record.ownerId = newOwnerId;
        insert(record);
        return record.itemId;
    }

    /** Delete an item clone from the database */
    public void deleteClone (int cloneId) throws PersistenceException
    {
        delete(getCloneClass(), cloneId);
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

    protected abstract Class<T> getItemClass ();
    protected abstract Class<? extends CloneRecord<T>> getCloneClass ();
    protected abstract Class<? extends CatalogRecord<T>> getCatalogClass ();
}
