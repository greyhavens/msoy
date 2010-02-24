//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListQuery;

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
     */
    public void createList (ItemListInfoRecord record)
    {
        Preconditions.checkArgument(
            record.listId == 0, "Can't insert existing list with id [" + record.listId + "]");
        insert(record);
    }

    /**
     * Deletes a list and removes all of its items.
     *
     * @param listId the ID of the list to delete.
     */
    public void deleteList (final int listId)
    {
        // delete all of the elements from the list
        deleteAll(ItemListElementRecord.class, new Where(ItemListElementRecord.LIST_ID, listId));

        // delete the list info
        delete(ItemListInfoRecord.getKey(listId));
    }

    /**
     * Gets the number of items in a list.
     *
     * @param listId the ID of the list to size up.
     */
    public int getSize (int listId)
    {
        CountRecord size = load(CountRecord.class,
            new FromOverride(ItemListElementRecord.class),
            new Where(ItemListElementRecord.LIST_ID, listId));

        return size.count;
    }

    /**
     * Gets the number of items of the given type in a list.
     */
    public int getSize (int listId, byte listType)
    {
        Where where;
        if (listType == Item.NOT_A_TYPE) {
            where = new Where(ItemListElementRecord.LIST_ID, listId);
        } else {
            where = new Where(ItemListElementRecord.LIST_ID, listId,
                              ItemListElementRecord.TYPE, listType);
        }

        CountRecord size = load(CountRecord.class,
                                new FromOverride(ItemListElementRecord.class), where);
        return size.count;
    }

    /**
     * Load the list info with the given id.
     */
    public ItemListInfoRecord loadInfo (int listId)
    {
        return load(ItemListInfoRecord.getKey(listId));
    }

    /**
     * Load all the ItemListInfos for the specified member.
     */
    public List<ItemListInfoRecord> loadInfos (int memberId)
    {
        return findAll(ItemListInfoRecord.class,
                       new Where(ItemListInfoRecord.MEMBER_ID, memberId));
    }

    /**
     * Load all of a member's lists of a particular type.
     */
    public List<ItemListInfoRecord> loadInfos (int memberId, byte type)
    {
        // query by type
        Where where = new Where(ItemListInfoRecord.MEMBER_ID, Integer.valueOf(memberId),
            ItemListInfoRecord.TYPE, Integer.valueOf(type));
        return findAll(ItemListInfoRecord.class, where);
    }

    /**
     * Load the identifiers for the items in the list with the given id.
     */
    public ItemIdent[] loadList (int listId)
    {
        List<ItemListElementRecord> list = findAll(ItemListElementRecord.class,
            new Where(ItemListElementRecord.LIST_ID, listId),
            OrderBy.ascending(ItemListElementRecord.SEQUENCE));
        return Lists.transform(list, ItemListElementRecord.TO_IDENT).toArray(
            new ItemIdent[list.size()]);
    }

    /**
     * Load item IDs from a list with optional query criteria for item type, number of results, and
     * ordering Useful for pagination in case the number of items in a list is exceptionally large.
     *
     * @param query the search criteria for list elements.
     * @return an array containing the results.
     */
    public ItemIdent[] loadList (ItemListQuery query)
    {
        Where where;
        if (query.itemType == Item.NOT_A_TYPE) {
            where = new Where(ItemListElementRecord.LIST_ID, query.listId);
        } else {
            where = new Where(ItemListElementRecord.LIST_ID, query.listId,
                ItemListElementRecord.TYPE, query.itemType);
        }

        OrderBy orderBy;
        if (query.descending) {
            orderBy = OrderBy.descending(ItemListElementRecord.SEQUENCE);
        } else {
            orderBy = OrderBy.ascending(ItemListElementRecord.SEQUENCE);
        }

        List<ItemListElementRecord> results;
        if (query.count > 0) {
            results = findAll(ItemListElementRecord.class, where, orderBy, new Limit(query.offset,
                query.count));
        } else {
            results = findAll(ItemListElementRecord.class, where, orderBy);
        }

        return Lists.transform(results, ItemListElementRecord.TO_IDENT).toArray(
            new ItemIdent[results.size()]);
    }

    /**
     * Adds the given item to the end of the specified list
     *
     * @param listId identifies the list.
     * @param item the ID of the item to add.
     */
    public void addItem (int listId, ItemIdent item)
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
    {
        // get the item's current location in the list
        final ItemListElementRecord record = loadElement(listId, item);

        if (record == null) {
            return false;
        }

        // delete the item from the list
        int deleted = delete(record);

        if (deleted == 1) {
            // update the location of any items that come after the removed
            // element,
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
     */
    public void insertItemAt (int listId, ItemIdent item, short index)
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
     * @throws IllegalArgumentException if the item is not already an element of these list.
     */
    public void moveItem (int listId, ItemIdent item, short toIndex)
    {
        // get the item's current location in the list
        ItemListElementRecord record = loadElement(listId, item);
        if (record == null) {
            throw new IllegalArgumentException(
                "Could not move item [" + item + "]. It is not an element of list [" + listId + "]");
        }

        // check whether the item is already in the right place
        short currentIndex = record.sequence;
        if (currentIndex != toIndex) {
            // shift all of the records between the current position and the new location
            shiftItems(listId, currentIndex, toIndex);

            // move the record
            record.sequence = toIndex;
            update(record);
        }
    }

    /**
     * Checks to see if the given list contains the given item.
     */
    public boolean contains (int listId, ItemIdent item)
    {
        return loadElement(listId, item) != null;
    }

    /**
     * Shifts the location of all items in the list from the given index (inclusive) to the end of
     * the list over one space to the right.
     *
     * @param listId identifies the list.
     * @param fromIndex shift everything from this location on to the right.
     */
    protected void shiftItemsRight (final int listId, final short fromIndex)
    {
        CacheInvalidator invalidator =
            new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(
                ItemListElementRecord.class) {
            public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                return record.listId == listId && record.sequence >= fromIndex;
            }
        };
        SQLExpression findList = ItemListElementRecord.LIST_ID.eq(listId);
        SQLExpression after = ItemListElementRecord.SEQUENCE.greaterEq(fromIndex);
        SQLExpression condition = Ops.and(findList, after);
        updatePartial(ItemListElementRecord.class, new Where(condition), invalidator,
                      ItemListElementRecord.SEQUENCE, ItemListElementRecord.SEQUENCE.plus(1));
    }

    /**
     * Shifts all of the items that come after the given index to the left.
     *
     * @param fromIndex the starting point from which to shift.
     */
    protected void shiftItemsLeft (final int listId, final short fromIndex)
    {
        SQLExpression findList = ItemListElementRecord.LIST_ID.eq(listId);
        SQLExpression after = ItemListElementRecord.SEQUENCE.greaterThan(fromIndex);
        SQLExpression condition = Ops.and(findList, after);
        CacheInvalidator invalidator =
            new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(
                ItemListElementRecord.class) {
            public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                return record.listId == listId && record.sequence > fromIndex;
            }
        };
        updatePartial(ItemListElementRecord.class, new Where(condition), invalidator,
                      ItemListElementRecord.SEQUENCE,
                      ItemListElementRecord.SEQUENCE.minus(Integer.valueOf(1)));
    }

    /**
     * This is used when moving a list element to shift any affected items between the items old
     * location and its new location.
     *
     * @param fromIndex the item's old location in the list.
     * @param toIndex the item's new location in the list.
     */
    protected void shiftItems (final int listId, final short fromIndex, final short toIndex)
    {
        CacheInvalidator invalidator;
        SQLExpression range;
        SQLExpression shift;

        if (fromIndex < toIndex) {
            // shift all affected items to the left
            SQLExpression min = ItemListElementRecord.SEQUENCE.greaterThan(fromIndex);
            SQLExpression max = ItemListElementRecord.SEQUENCE.lessEq(toIndex);
            range = Ops.and(min, max);
            shift = ItemListElementRecord.SEQUENCE.minus(Integer.valueOf(1));
            invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(
                ItemListElementRecord.class) {
                public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                    return record.listId == listId && record.sequence > fromIndex &&
                        record.sequence <= toIndex;
                }
            };

        } else {
            // shift affected items to the right (increment by one)
            SQLExpression min = ItemListElementRecord.SEQUENCE.greaterEq(toIndex);
            SQLExpression max = ItemListElementRecord.SEQUENCE.lessThan(fromIndex);
            range = Ops.and(min, max);
            shift = ItemListElementRecord.SEQUENCE.plus(Integer.valueOf(1));
            invalidator = new CacheInvalidator.TraverseWithFilter<ItemListElementRecord>(
                ItemListElementRecord.class) {
                public boolean testForEviction (Serializable key, ItemListElementRecord record) {
                    return (record != null) && record.sequence >= toIndex &&
                        record.sequence < fromIndex;
                }
            };
        }

        SQLExpression findList = ItemListElementRecord.LIST_ID.eq(listId);
        SQLExpression condition = Ops.and(findList, range);

        // update all of the affected items
        updatePartial(ItemListElementRecord.class, new Where(condition), invalidator,
                      ItemListElementRecord.SEQUENCE, shift);
    }

    /**
     * Loads a specific item list element record.
     */
    protected ItemListElementRecord loadElement (int listId, ItemIdent item)
    {
        return load(ItemListElementRecord.class, new Where(ItemListElementRecord.LIST_ID, listId,
            ItemListElementRecord.ITEM_ID, item.itemId, ItemListElementRecord.TYPE, item.type));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ItemListElementRecord.class);
        classes.add(ItemListInfoRecord.class);
    }
}
