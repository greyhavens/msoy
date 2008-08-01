//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Logic;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.server.persist.CountRecord;

@Singleton
@BlockingThread
public class ItemListRepository extends DepotRepository
{
    @Inject
    public ItemListRepository(PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Inserts the given list info record.
     *
     * @throws PersistenceException if the given record already has an id
     */
    public void createList (ItemListInfoRecord record)
        throws PersistenceException
    {
        if (record.listId != 0) {
            throw new IllegalArgumentException("Can't insert existing list with id [" + record.listId + "]");
        }
        insert(record);
    }

    /**
     * Deletes a list and removes all of its items.
     *
     * @param listId the ID of the list to delete.
     */
    public void deleteList (final int listId)
        throws PersistenceException
    {
        // delete all of the elements from the list
        deleteAll(ItemListElementRecord.class, new Where(ItemListElementRecord.LIST_ID_C, listId),
            new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(
                ItemListElementRecord.class) {
                public boolean testForEviction (Serializable key, ItemListElementRecord record)
                {
                    return record.listId == listId;
                }
        });

        // delete the list info
        delete(ItemListInfoRecord.class, listId);
    }

    /**
     * Gets the number of items in a list.
     *
     * @param listId the ID of the list to size up.
     */
    public int getSize (int listId)
        throws PersistenceException
    {
        CountRecord size = load(CountRecord.class,
            new FromOverride(ItemListElementRecord.class),
            new Where(ItemListElementRecord.LIST_ID_C, listId));

        return size.count;
    }

    /**
     * Load the list info with the given id.
     */
    public ItemListInfoRecord loadInfo (int listId)
        throws PersistenceException
    {
        return load(ItemListInfoRecord.class, listId);
    }

    /**
     * Load all the ItemListInfos for the specified member.
     */
    public List<ItemListInfoRecord> loadInfos (int memberId)
        throws PersistenceException
    {
        // List<ItemListInfoRecord> list = Collections.emptyList();
        return findAll(ItemListInfoRecord.class, new Where(ItemListInfoRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Load all of a member's lists of a particular type.
     */
    public List<ItemListInfoRecord> loadInfos (int memberId, byte type)
        throws PersistenceException
    {
        // query by type
        Where where = new Where(ItemListInfoRecord.MEMBER_ID_C, Integer.valueOf(memberId),
            ItemListInfoRecord.TYPE_C, Integer.valueOf(type));
        return findAll(ItemListInfoRecord.class, where);
    }

    /**
     * Load the identifiers for the items in the list with the given id.
     */
    public ItemIdent[] loadList (int listId)
        throws PersistenceException
    {
        List<ItemListElementRecord> list = findAll(ItemListElementRecord.class,
            new Where(ItemListElementRecord.LIST_ID_C, listId),
            OrderBy.ascending(ItemListElementRecord.SEQUENCE_C));

        int size = list.size();
        ItemIdent[] idents = new ItemIdent[size];
        for (int ii = 0; ii < size; ii++) {
            idents[ii] = list.get(ii).toItemIdent();
        }

        return idents;
    }

    /**
     * Load a limited number of the item IDs in the list with the given id. Useful for
     * pagination in case the number of items in a list is exceptionally large.
     *
     * @param listId the list to query.
     * @param offset the starting index for the results.
     * @param count the number of results to return.
     * @return an array containing the results.
     */
    public ItemIdent[] loadList (int listId, int offset, int count)
        throws PersistenceException
    {
        List<ItemListElementRecord> list = findAll(ItemListElementRecord.class,
            new Where(ItemListElementRecord.LIST_ID_C, listId),
            OrderBy.ascending(ItemListElementRecord.SEQUENCE_C),
            new Limit(offset, count));

        int size = list.size();
        ItemIdent[] idents = new ItemIdent[size];
        for (int ii = 0; ii < size; ii++) {
            idents[ii] = list.get(ii).toItemIdent();
        }

        return idents;
    }

    /**
     * Adds the given item to the end of the specified list
     *
     * @param listId identifies the list.
     * @param item the ID of the item to add.
     */
    public void addItem (int listId, ItemIdent item)
        throws PersistenceException
    {
        ItemListElementRecord record = new ItemListElementRecord(listId, item, getSize(listId));
        insert(record);
    }

    /**
     * Removes an item from the given list.
     *
     * @param listId identifies the list.
     * @param item the ID of the item to remove.
     * @return returns true if the item was successfully removed, false if the item
     *    was not an element of the list to begin with.
     */
    public boolean removeItem (int listId, ItemIdent item)
        throws PersistenceException
    {
        // get the item's current location in the list
        final ItemListElementRecord record = loadElement(listId, item);

        // delete the item from the list
        int deleted = delete(record);

        if(deleted == 1) {
            // update the location of any items that come after the removed element,
            // shift them to the left to fill in the gap
            shiftItemsLeft(listId, record.sequence);
            return true;
        }

        return false;
    }

    /**
     * Inserts an item into a list at a specific location.
     *
     * @param listId identifies the list.
     * @param item identifies the inserted item.
     * @param index the list index at which to insert the item.
     * @throws PersistenceException
     */
    public void insertItemAt (int listId, ItemIdent item, short index)
        throws PersistenceException
    {
        // shift all of the records between the insert location and the end of the list to the right
        shiftItemsRight(listId, index);

        // insert the record at the specified location
        ItemListElementRecord record = new ItemListElementRecord(listId, item, index);
        insert(record);
    }

    /**
     * Changes an item's location within a list.
     *
     * @param listId identifies the list.
     * @param item identifies the item to move.
     * @param toIndex the location to which to move the item.
     * @throws PersistenceException if the item is not already an element of these list, or if
     *    any other database nastiness occurs.
     */
    public void moveItem (int listId, ItemIdent item, short toIndex)
        throws PersistenceException
    {
        // get the item's current location in the list
        ItemListElementRecord record = loadElement(listId, item);

        if(record == null) {
            throw new PersistenceException("Could not move item ["+item+
                "] it is not an element of list ["+listId+"]");
        }

        short currentIndex = record.sequence;

        // check whether the item is already in the right place
        if (currentIndex != toIndex) {

            // shift all of the records between the current position and
            // the new location
            shiftItems(listId, currentIndex, toIndex);

            // move the record
            record.sequence = toIndex;
            update(record);

        }
    }

    /**
     * Checks to see if the given list contains the given item.
     */
    public boolean contains(int listId, ItemIdent item)
        throws PersistenceException
    {
        return loadElement(listId, item) != null;
    }

    /**
     * Shifts the location of all items in the list from the given index (inclusive) to the end
     * of the list over one space to the right.
     *
     * @param listId identifies the list.
     * @param fromIndex shift everything from this location on to the right.
     */
    protected void shiftItemsRight(int listId, final short fromIndex)
        throws PersistenceException
    {
        Conditionals.GreaterThanEquals after = new Conditionals.GreaterThanEquals(ItemListElementRecord.SEQUENCE_C, fromIndex);
        Map<String, SQLExpression> shift = new HashMap<String, SQLExpression>();
        shift.put(ItemListElementRecord.SEQUENCE, new Arithmetic.Add(ItemListElementRecord.SEQUENCE_C, Integer.valueOf(1)));

        CacheInvalidator invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(ItemListElementRecord.class) {
            public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                return (record != null) && record.sequence >= fromIndex;
            }
        };

        updateLiteral(ItemListElementRecord.class, new Where(after), invalidator, shift);
    }

    /**
     * Shifts all of the items that come after the given index to the left.
     *
     * @param listId identifies the list.
     * @param fromIndex the starting point from which to shift.
     */
    protected void shiftItemsLeft(int listId, final short fromIndex)
        throws PersistenceException
    {
        Conditionals.GreaterThan after = new Conditionals.GreaterThan(ItemListElementRecord.SEQUENCE_C, fromIndex);
        Map<String, SQLExpression> shift = new HashMap<String, SQLExpression>();
        shift.put(ItemListElementRecord.SEQUENCE, new Arithmetic.Sub(ItemListElementRecord.SEQUENCE_C, Integer.valueOf(1)));

        CacheInvalidator invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(ItemListElementRecord.class) {
            public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                return (record != null) && record.sequence > fromIndex;
            }
        };

        updateLiteral(ItemListElementRecord.class, new Where(after), invalidator, shift);
    }

    /**
     * This is used when moving a list element to shift any affected items
     * between the items old location and its new location.
     *
     * @param listId identifies the list.
     * @param fromIndex the item's old location in the list.
     * @param toIndex the item's new location in the list.
     */
    protected void shiftItems(int listId, final short fromIndex, final short toIndex)
        throws PersistenceException
    {
        CacheInvalidator invalidator;
        SQLExpression range;
        Map<String, SQLExpression> shift = new HashMap<String, SQLExpression>();

        if (fromIndex < toIndex) {

            // shift all affected items to the left
            Conditionals.GreaterThan min = new Conditionals.GreaterThan(ItemListElementRecord.SEQUENCE_C, fromIndex);
            Conditionals.LessThanEquals max = new Conditionals.LessThanEquals(ItemListElementRecord.SEQUENCE_C, toIndex);
            range = new Logic.And(min, max);

            shift.put(ItemListElementRecord.SEQUENCE, new Arithmetic.Sub(ItemListElementRecord.SEQUENCE_C, Integer.valueOf(1)));

            invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(ItemListElementRecord.class) {
                public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                    return (record != null) && record.sequence > fromIndex && record.sequence <= toIndex;
                }
            };

        } else {

            // shift affected items to the right (increment by one)
            Conditionals.GreaterThanEquals min = new Conditionals.GreaterThanEquals(ItemListElementRecord.SEQUENCE_C, toIndex);
            Conditionals.LessThan max = new Conditionals.LessThan(ItemListElementRecord.SEQUENCE_C, fromIndex);
            range = new Logic.And(min, max);

            shift.put(ItemListElementRecord.SEQUENCE, new Arithmetic.Add(ItemListElementRecord.SEQUENCE_C, Integer.valueOf(1)));

            invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(ItemListElementRecord.class) {
                public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                    return (record != null) && record.sequence >= toIndex && record.sequence < fromIndex;
                }
            };

        }

        // update all of the affected items
        updateLiteral(ItemListElementRecord.class, new Where(range), invalidator, shift);
    }

    /**
     * Loads a specific item list element record.
     */
    protected ItemListElementRecord loadElement (int listId, ItemIdent item)
        throws PersistenceException
    {
        return load(ItemListElementRecord.class, new Where(ItemListElementRecord.LIST_ID_C, listId,
            ItemListElementRecord.ITEM_ID_C, item.itemId, ItemListElementRecord.TYPE_C, item.type));
    }

    @Override
    // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ItemListElementRecord.class);
        classes.add(ItemListInfoRecord.class);
    }
}
