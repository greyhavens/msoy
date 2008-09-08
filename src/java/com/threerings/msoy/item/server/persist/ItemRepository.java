//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntListUtil;
import com.samskivert.util.IntSet;
import com.samskivert.util.QuickSort;
import com.samskivert.util.Tuple;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DatabaseException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.SelectClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.EpochSeconds;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.expression.ValueExp;
import com.samskivert.jdbc.depot.operator.Arithmetic;
import com.samskivert.jdbc.depot.operator.Conditionals;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;

import com.threerings.msoy.room.server.persist.MemoryRepository;

import static com.threerings.msoy.Log.log;

/**
 * Manages a repository of digital items of a particular type.
 */
@BlockingThread
public abstract class ItemRepository<T extends ItemRecord>
    extends DepotRepository
{
    @Computed
    @Entity
    public static class RatingAverageRecord extends PersistentRecord {
        @Computed(fieldDefinition="count(*)")
        public int count;
        @Computed(fieldDefinition="sum(" + RatingRecord.RATING + ")")
        public int sum;
    }

    /**
     *  Update the number of days in the new & hot dropoff adjustment.
     */
    public static void setNewAndHotDropoffDays (int days)
    {
        _newAndHotDropoffSeconds = Math.max(1, days * 24 * 60 * 60); // don't let us div by 0.
    }

    /** The factor by which we split item cost into gold and flow. */
    public final static int FLOW_FOR_GOLD = 600;

    public ItemRepository (PersistenceContext ctx)
    {
        super(ctx);

        _tagRepo = new TagRepository(ctx) {
            @Override
            protected TagRecord createTagRecord () {
                return ItemRepository.this.createTagRecord();
            }
            @Override
            protected TagHistoryRecord createTagHistoryRecord () {
                return ItemRepository.this.createTagHistoryRecord();
            }
        };

        // TEMP added 2008.05.15
        _ctx.registerMigration(getItemClass(), new EntityMigration(17000) {
            @Override
            public boolean runBeforeDefault () {
                return false; // let the damn thing create the column first
            }

            @Override
            public int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                int migrated = 0;
                try {
                    OrderBy order = OrderBy.descending(getItemColumn(ItemRecord.LAST_TOUCHED));
                    final int limit = 1000;
                    int count = 0;
                    while (true) {
                        List<T> list = findAll(getItemClass(), order, new Limit(count, limit));
                        for (T record : list) {
                            RatingAverageRecord average = load(RatingAverageRecord.class,
                                 new FromOverride(getRatingClass()),
                                 new Where(getRatingColumn(RatingRecord.ITEM_ID), record.itemId));
                            if (average.count > 0) {
                                migrated++;
                                updatePartial(getItemClass(), record.itemId,
                                    ItemRecord.RATING_COUNT, average.count);
                            }
                        }
                        if (list.size() < limit) {
                            break;
                        } else {
                            count += limit;
                        }
                    }
                } catch (Exception e) {
                    log.warning("Couldn't migrate: " + e);
                    throw new SQLException();
                }

                log.info("Migrated " + migrated + " records.");
                return migrated;
            }
        });
        // END: temp
    }

    /**
     * Configures this repository with its item type and the memory repository.
     */
    public void init (byte itemType)
    {
        _itemType = itemType;
    }

    /**
     * Returns the item type constant for the type of item handled by this repository.
     */
    public byte getItemType ()
    {
        return _itemType;
    }

    /**
     * Converts a runtime item record to an initialized instance of our persistent item record
     * class.
     */
    public ItemRecord newItemRecord (Item item)
    {
        try {
            T record = getItemClass().newInstance();
            record.fromItem(item);
            return record;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the repository that manages tags for this item.
     */
    public TagRepository getTagRepository ()
    {
        return _tagRepo;
    }

    /**
     * Load an item, or a clone.
     */
    public T loadItem (int itemId)
    {
        // TODO: This will only work for the first two billion clones.
        return itemId > 0 ? loadOriginalItem(itemId) : loadClone(itemId);
    }

    /**
     * Loads an item with the specified identifier. Returns null if no item exists with that
     * identifier.
     */
    public T loadOriginalItem (int itemId)
    {
        return load(getItemClass(), itemId);
    }

    /**
     * Loads the clone with the given identifier. Returns null if no clone exists with that
     * identifier.
     */
    public T loadClone (int cloneId)
    {
        CloneRecord cloneRecord = loadCloneRecord(cloneId);
        if (cloneRecord == null) {
            return null;
        }

        T clone = loadOriginalItem(cloneRecord.originalItemId);
        if (clone == null) {
            throw new DatabaseException(
                "Clone's original does not exist [cloneId=" + cloneId +
                ", originalItemId=" + cloneRecord.originalItemId + "]");
        }
        clone.initFromClone(cloneRecord);
        return clone;
    }

    /**
     * Loads all original items owned by the specified member in the specified suite.
     */
    public List<T> loadOriginalItems (int ownerId, int suiteId)
    {
        Where where;
        if (suiteId == 0) {
            where = new Where(getItemColumn(ItemRecord.OWNER_ID), ownerId);
        } else {
            where = new Where(getItemColumn(ItemRecord.OWNER_ID), ownerId,
                              getItemColumn(SubItemRecord.SUITE_ID), suiteId);
        }
        return findAll(getItemClass(), where);
    }

    /**
     * Loads all original items with the specified suite.
     */
    public List<T> loadOriginalItemsBySuite (int suiteId)
    {
        return findAll(getItemClass(), new Where(getItemColumn(SubItemRecord.SUITE_ID), suiteId));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public List<T> loadClonedItems (int ownerId, int suiteId)
    {
        Where where;
        if (suiteId == 0) {
            where = new Where(getCloneColumn(CloneRecord.OWNER_ID), ownerId);
        } else {
            where = new Where(getCloneColumn(CloneRecord.OWNER_ID), ownerId,
                              getItemColumn(SubItemRecord.SUITE_ID), suiteId);
        }
        return loadClonedItems(where);
    }

    /**
     * Loads up to maxCount items from a user's inventory that were the most recently touched.
     */
    public List<T> loadRecentlyTouched (int ownerId, int maxCount)
    {
        // Since we don't know how many we'll find of each kind (cloned, orig), we load the max
        // from each.
        Limit limit = new Limit(0, maxCount);
        List<T> originals = findAll(
            getItemClass(),
            new Where(getItemColumn(ItemRecord.OWNER_ID), ownerId),
            OrderBy.descending(getItemColumn(ItemRecord.LAST_TOUCHED)),
            limit);
        List<T> clones = loadClonedItems(
            new Where(getCloneColumn(CloneRecord.OWNER_ID), ownerId),
            OrderBy.descending(getCloneColumn(CloneRecord.LAST_TOUCHED)), limit);
        int size = originals.size() + clones.size();

        List<T> list = Lists.newArrayListWithExpectedSize(size);
        list.addAll(originals);
        list.addAll(clones);

        // now, sort by their lastTouched time
        QuickSort.sort(list, new Comparator<T>() {
            public int compare (T o1, T o2) {
                return o2.lastTouched.compareTo(o1.lastTouched);
            }
        });

        // remove any items beyond maxCount
        for (int ii = size - 1; ii >= maxCount; ii--) {
            list.remove(ii);
        }

        return list;
    }

    /**
     * Loads the specified items. Omits missing items from results.
     */
    public List<T> loadItems (int[] itemIds)
    {
        Comparable<?>[] idArr = IntListUtil.box(itemIds);
        List<T> items = resolveClones(loadAll(getCloneClass(), Arrays.asList(idArr)));
        items.addAll(loadAll(getItemClass(), Arrays.asList(idArr)));
        return items;
    }

    /**
     * Loads all items who have a non-zero {@link ItemRecord#flagged} field, limited to a given
     * number of rows.
     */
    public List<T> loadFlaggedItems (int count)
    {
        return findAll(getItemClass(),
                       new Where(new GreaterThan(getItemColumn(ItemRecord.FLAGGED), 0)),
                       new Limit(0, count));
    }

    /**
     * Loads a single clone record by item id.
     */
    public CloneRecord loadCloneRecord (int itemId)
    {
        return load(getCloneClass(), itemId);
    }

    /**
     * Loads all the raw clone records associated with a given original item id. This is
     * potentially a very large dataset.
     */
    public List<CloneRecord> loadCloneRecords (int itemId)
    {
        return findAll(
            getCloneClass(),
            new Where(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID), itemId));
    }

    /**
     * Loads and returns all items (clones and originals) that are "in use" at the specified
     * location.
     */
    public List<T> loadItemsByLocation (int location)
    {
        List<T> items = loadClonedItems(
            new Where(getCloneColumn(CloneRecord.LOCATION), location));
        List<T> citems = findAll(
            getItemClass(), new Where(getItemColumn(ItemRecord.LOCATION), location));
        items.addAll(citems);
        return items;
    }

    /**
     * Mark the specified items as being used in the specified way.
     */
    public void markItemUsage (int[] itemIds, byte usageType, int location)
    {
        Class<T> iclass = getItemClass();
        Class<CloneRecord> cclass = getCloneClass();

        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (int itemId : itemIds) {
            int result;
            if (itemId > 0) {
                result = updatePartial(
                    iclass, itemId, ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, now);
            } else {
                result = updatePartial(
                    cclass, itemId, ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, now);
            }
            // if the item didn't update, point that out to log readers
            if (0 == result) {
                log.info("Attempt to mark item usage matched zero rows [type=" + _itemType +
                         ", itemId=" + itemId + ", usageType=" + usageType +
                         ", location=" + location + "].");
            }
        }
    }

    /**
     * Find a single catalog entry randomly.
     */
    public CatalogRecord pickRandomCatalogEntry ()
    {
        CatalogRecord record = load(getCatalogClass(), new QueryClause[] {
            new Limit(0, 1),
            OrderBy.random()
        });

        if (record != null) {
            record.item = loadOriginalItem(record.listedItemId);
        }
        return record;
    }

    /**
     * Find a single random catalog entry that is tagged with *any* of the specified tags.
     */
    public CatalogRecord findRandomCatalogEntryByTags (String... tags)
    {
        // first find the tag record...
        List<TagNameRecord> tagRecords = getTagRepository().getTags(tags);
        int tagCount = tagRecords.size();
        if (tagCount == 0) {
            return null;
        }

        Integer[] tagIds = new Integer[tagCount];
        for (int ii = 0; ii < tagCount; ii++) {
            tagIds[ii] = tagRecords.get(ii).tagId;
        }

        List<CatalogRecord> records = findAll(getCatalogClass(),
            new Join(getCatalogClass(), CatalogRecord.LISTED_ITEM_ID,
                     getItemClass(), ItemRecord.ITEM_ID),
            new Limit(0, 1),
            OrderBy.random(),
            new Join(getCatalogClass(), CatalogRecord.LISTED_ITEM_ID,
                     getTagRepository().getTagClass(), TagRecord.TARGET_ID),
            new Where(new In(getTagColumn(TagRecord.TAG_ID), tagIds)));

        if (records.isEmpty()) {
            return null;
        }

        CatalogRecord record = records.get(0);
        record.item = loadOriginalItem(record.listedItemId);
        return record;
    }

    /**
     * Counts all items in the catalog that match the supplied query terms.
     */
    public int countListings (boolean mature, String search, int tag, int creator, Float minRating,
                              int suiteId)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(getCatalogClass()));
        clauses.add(new Join(getCatalogClass(), CatalogRecord.LISTED_ITEM_ID,
                             getItemClass(), ItemRecord.ITEM_ID));

        // see if there's any where bits to turn into an actual where clause
        addSearchClause(clauses, mature, search, tag, creator, minRating, suiteId);

        // finally fetch all the catalog records of interest
        return load(CountRecord.class, clauses).count;
    }

    /**
     * Loads all items in the catalog.
     *
     * TODO: This method currently fetches CatalogRecords through a join against ItemRecord,
     *       and then executes a second query against ItemRecord only. This really really has
     *       to be a single join in a sane universe, but that makes significant demands on the
     *       Depot code that we don't know how to handle yet (or possibly some fiddling with
     *       the Item vs Catalog class hierarchies).
     */
    public List<CatalogRecord> loadCatalog (byte sortBy, boolean mature, String search, int tag,
                                  int creator, Float minRating, int suiteId, int offset, int rows)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Join(getCatalogClass(), CatalogRecord.LISTED_ITEM_ID,
                             getItemClass(), ItemRecord.ITEM_ID));
        clauses.add(new Limit(offset, rows));

        // sort out the primary and secondary order by clauses
        List<SQLExpression> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        switch(sortBy) {
        case CatalogQuery.SORT_BY_LIST_DATE:
            addOrderByListDate(obExprs, obOrders);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_RATING:
            addOrderByRating(obExprs, obOrders);
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.ASC);
            break;
        case CatalogQuery.SORT_BY_PRICE_ASC:
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.ASC);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_PRICE_DESC:
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.DESC);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_PURCHASES:
            addOrderByPurchases(obExprs, obOrders);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_NEW_AND_HOT:
            addOrderByNewAndHot(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_FAVORITES:
            addOrderByFavorites(obExprs, obOrders);
            addOrderByRating(obExprs, obOrders);
            break;
        default:
            throw new IllegalArgumentException(
                "Sort method not implemented [sortBy=" + sortBy + "]");
        }
        clauses.add(new OrderBy(obExprs.toArray(new SQLExpression[obExprs.size()]),
                                obOrders.toArray(new OrderBy.Order[obOrders.size()])));

        // see if there's any where bits to turn into an actual where clause
        addSearchClause(clauses, mature, search, tag, creator, minRating, suiteId);

        // finally fetch all the catalog records of interest and resolve their item bits
        return resolveCatalogRecords(findAll(getCatalogClass(), clauses));
    }

    /**
     * Loads up the specified catalog records.
     */
    public List<CatalogRecord> loadCatalog (Collection<Integer> catalogIds)
    {
        return resolveCatalogRecords(loadAll(getCatalogClass(), catalogIds));
    }

    protected List<CatalogRecord> resolveCatalogRecords (List<CatalogRecord> records)
    {
        // load the listed items for each record and then fill them back in
        Map<Integer, T> map = Maps.newHashMap();
        Function<CatalogRecord, Integer> getItemId = new Function<CatalogRecord, Integer>() {
            public Integer apply (CatalogRecord record) {
                return record.listedItemId;
            }
        };
        for (T iRec : loadAll(getItemClass(), Lists.transform(records, getItemId))) {
            map.put(iRec.itemId, iRec);
        }
        for (CatalogRecord record : records) {
            record.item = map.get(record.listedItemId);
        }
        return records;
    }

    /**
     * Load a single catalog listing.
     */
    public CatalogRecord loadListing (int catalogId, boolean loadListedItem)
    {
        CatalogRecord record = load(getCatalogClass(), catalogId);
        if (record != null && loadListedItem) {
            record.item = load(getItemClass(), record.listedItemId);
        }
        return record;
    }

    /**
     * Update either the 'purchases' or the 'returns' field of a catalog listing, and figure out if
     * it's time to reprice it.
     */
    public void nudgeListing (int catalogId, boolean purchased)
    {
        CatalogRecord record = load(getCatalogClass(), catalogId);
        if (record == null) {
            return; // if the listing has been unlisted, we don't need to nudge it.
        }

        Map<String, SQLExpression> updates = Maps.newHashMap();
        if (purchased) {
            updates.put(CatalogRecord.PURCHASES,
                        new Arithmetic.Add(getCatalogColumn(CatalogRecord.PURCHASES), 1));

            int purchases = record.purchases + 1; // for below calculations
            switch (record.pricing) {
            case CatalogListing.PRICING_LIMITED_EDITION:
                if (purchases >= record.salesTarget) {
                    updates.put(CatalogRecord.PRICING,
                                new LiteralExp(""+CatalogListing.PRICING_HIDDEN));
                }
                break;

            case CatalogListing.PRICING_ESCALATE:
                if (purchases == record.salesTarget) {
                    updates.put(CatalogRecord.FLOW_COST,
                                new LiteralExp(""+CatalogListing.escalatePrice(record.flowCost)));
                    updates.put(CatalogRecord.GOLD_COST,
                                new LiteralExp(""+CatalogListing.escalatePrice(record.goldCost)));
                }
                break;
            }

        } else {
            updates.put(CatalogRecord.RETURNS,
                        new Arithmetic.Add(getCatalogColumn(CatalogRecord.RETURNS), 1));
        }

        // finally update the columns we actually modified
        updateLiteral(getCatalogClass(), record.catalogId, updates);
    }

    /**
     * Inserts the supplied item into the database. The {@link ItemRecord#itemId} and the
     * {@link ItemRecord#lastTouched) fields will be filled in as a result of this call.
     */
    public void insertOriginalItem (T item, boolean catalogListing)
    {
        if (item.itemId != 0) {
            throw new IllegalArgumentException("Can't insert item with existing key: " + item);
        }
        item.lastTouched = new Timestamp(System.currentTimeMillis());
        insert(item);
    }

    /**
     * Updates the supplied item in the database. The {@link ItemRecord#lastTouched) field
     * will be filled in as a result of this call.
     */
    public void updateOriginalItem (T item)
    {
        updateOriginalItem(item, true);
    }

    /**
     * Updates the supplied item in the database. The {@link ItemRecord#lastTouched) field
     * will be optionally updated. In general, updateLastTouched should be true.
     */
    public void updateOriginalItem (T item, boolean updateLastTouched)
    {
        if (updateLastTouched) {
            item.lastTouched = new Timestamp(System.currentTimeMillis());
        }
        update(item);
    }

    /**
     * Updates a clone item's override media in the database. This is done when we remix.
     * The {@link CloneRecord#lastTouched) field will be filled in as a result of this call.
     */
    public void updateCloneMedia (CloneRecord cloneRec)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        cloneRec.lastTouched = now;
        cloneRec.mediaStamp = (cloneRec.mediaHash == null) ? null : now;

        updatePartial(getCloneClass(), cloneRec.itemId,
            CloneRecord.MEDIA_HASH, cloneRec.mediaHash,
            CloneRecord.MEDIA_STAMP, cloneRec.mediaStamp,
            CloneRecord.LAST_TOUCHED, cloneRec.lastTouched);
    }

    /**
     * Updates a clone item's override name in the database.
     * The {@link CloneRecord#lastTouched) field will be filled in as a result of this call.
     */
    public void updateCloneName (CloneRecord cloneRec)
    {
        cloneRec.lastTouched = new Timestamp(System.currentTimeMillis());

        updatePartial(getCloneClass(), cloneRec.itemId,
            CloneRecord.NAME, cloneRec.name,
            CloneRecord.LAST_TOUCHED, cloneRec.lastTouched);
    }

    /**
     * Create a row in our catalog table corresponding to the given item record, which should
     * be of the immutable variety.
     */
    public CatalogRecord insertListing (
        ItemRecord listItem, int originalItemId, int pricing, int salesTarget,
        int flowCost, int goldCost, long listingTime)
    {
        if (listItem.ownerId != 0) {
            throw new IllegalArgumentException(
                "Can't list item with owner [itemId=" + listItem.itemId + "]");
        }

        CatalogRecord record;
        try {
            record = getCatalogClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.item = listItem;
        record.listedItemId = listItem.itemId;
        record.originalItemId = originalItemId;
        record.listedDate = new Timestamp(listingTime);
        record.pricing = pricing;
        record.salesTarget = salesTarget;
        record.purchases = record.returns = 0;
        record.flowCost = flowCost;
        record.goldCost = goldCost;
        insert(record);

        // wire this listed item and its original up to the catalog record
        noteListing(record.listedItemId, record.catalogId);
        noteListing(originalItemId, record.catalogId);

        return record;
    }

    /**
     * Updates the pricing for the specified catalog listing.
     */
    public void updatePricing (int catalogId, int pricing, int salesTarget,
                               int flowCost, int goldCost, long updateTime)
    {
        updatePartial(getCatalogClass(), catalogId,
                      // TODO?: CatalogRecord.LISTED_DATE, new Timestamp(updateTime),
                      CatalogRecord.PRICING, pricing,
                      CatalogRecord.SALES_TARGET, salesTarget,
                      CatalogRecord.FLOW_COST, flowCost,
                      CatalogRecord.GOLD_COST, goldCost);
    }

    /**
     * Removes the listing for the specified item from the catalog, returns true if a listing was
     * found and removed, false otherwise.
     */
    public boolean removeListing (CatalogRecord listing)
    {
        // clear out the listing mappings for the original item
        if (listing.originalItemId != 0) {
            noteListing(listing.originalItemId, 0);
        }
        return delete(getCatalogClass(), listing.catalogId) > 0;
    }

    /**
     * Inserts an item clone into the database with the given owner and purchase data. Also fills
     * (@link CloneRecord#itemId) with the next available ID and {@link CloneRecord#ownerId)
     * with the new owner. Finally, updates {@link CloneRecord#lastTouched) and
     * {@link CloneRecord#purchaseTime).
     */
    public ItemRecord insertClone (ItemRecord parent, int newOwnerId, int flowPaid, int goldPaid)
    {
        CloneRecord record;
        try {
            record = getCloneClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.initialize(parent, newOwnerId, flowPaid, goldPaid);
        insert(record);

        ItemRecord newClone = (ItemRecord) parent.clone();
        newClone.initFromClone(record);

        return newClone;
    }

    /**
     * Deletes an item from the repository and all associated data (ratings, tags, tag history).
     * This method does not perform any checking to determine whether it is safe to delete the item
     * so do not call it unless you know the item is not listed in the catalog or otherwise in use.
     */
    public void deleteItem (final int itemId)
    {
        if (itemId < 0) {
            delete(getCloneClass(), itemId);

        } else {
            // delete the item in question
            delete(getItemClass(), itemId);

            // delete rating records for this item (and invalidate the cache properly)
            deleteAll(getRatingClass(), new Where(getRatingColumn(RatingRecord.ITEM_ID), itemId));

            // delete tag records relating to this item
            _tagRepo.deleteTags(itemId);
        }

        // delete any entity memory for this item as well
        _memoryRepo.deleteMemories(_itemType, itemId);
    }

    /**
     * Returns the rating given to the specified item by the specified member or 0 if they've never
     * rated the item.
     */
    public byte getRating (int itemId, int memberId)
    {
        RatingRecord record = load(
            getRatingClass(), RatingRecord.ITEM_ID, itemId, RatingRecord.MEMBER_ID, memberId);
        return (record == null) ? (byte)0 : record.rating;
    }

    /**
     * Insert/update a rating row, calculate the new rating and finally update the item's rating.
     *
     * @return First the new rating as a float.  Also, the tuple will contain true if this newly
     *         added rating qualifies this item as a new "solid" 4+ star rating.  That means that
     *         it either just breached the rating threshold for "solid" and has 4+ stars or just
     *         went from below 4 to above 4, and has more than the requisite "solid" ratings.
     */
    public Tuple<Float, Boolean> rateItem (int itemId, int memberId, byte rating)
    {
        // first create a new rating record
        RatingRecord record;
        try {
            record = getRatingClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // populate and insert it
        record.itemId = itemId;
        record.memberId = memberId;
        record.rating = rating;
        boolean newRater = store(record);

        RatingAverageRecord average =
            load(RatingAverageRecord.class,
                 new FromOverride(getRatingClass()),
                 new Where(getRatingColumn(RatingRecord.ITEM_ID), itemId));

        float newRating = (average.count == 0) ? 0f : average.sum/(float)average.count;
        // and then smack the new value into the item using yummy depot code
        updatePartial(getItemClass(), itemId, ItemRecord.RATING, newRating,
                      ItemRecord.RATING_COUNT, average.count);

        float oldRating = (average.count < 2) ? 0f :
            (average.sum - rating)/(float)(average.count - 1);
        boolean newSolid =
            (average.count == MIN_SOLID_RATINGS && newRating >= 4) ||
            (average.count > MIN_SOLID_RATINGS && newRating >= 4 && (!newRater || oldRating < 4));

        return new Tuple<Float, Boolean>(newRating, newSolid);
    }

    /**
     * Transfers rating records from one record to another. This is used when a catalog listing is
     * updated to migrate the players' individual rating records from the old prototype item to the
     * new one.
     *
     * <p> Note: this destabilizes the rating of the abandoned previous listing, but that rating is
     * meaningless anyway since the item is no longer in the catalog. Ratings should really be on
     * listings not items, but that's a giant fiasco we don't want to deal with.
     */
    public void reassignRatings (final int oldItemId, int newItemId)
    {
        // TODO: this cache eviction might be slow :)
        updatePartial(getRatingClass(), new Where(getRatingColumn(RatingRecord.ITEM_ID), oldItemId),
                      new CacheInvalidator.TraverseWithFilter<RatingRecord>(getRatingClass()) {
                          @Override
                        public boolean testForEviction (Serializable key, RatingRecord record) {
                              return (record.itemId == oldItemId);
                          }
                      }, RatingRecord.ITEM_ID, newItemId);
    }

    /**
     * Safely changes the owner of an item record with a sanity-check against race conditions.
     */
    public void updateOwnerId (ItemRecord item, int newOwnerId)
    {
        Where where;
        Key<?> key;
        if (item.itemId < 0) {
            where = new Where(getCloneColumn(ItemRecord.ITEM_ID), item.itemId,
                              getCloneColumn(ItemRecord.OWNER_ID), item.ownerId);
            key = new Key<CloneRecord>(getCloneClass(), CloneRecord.ITEM_ID, item.itemId);
        } else {
            where = new Where(getItemColumn(ItemRecord.ITEM_ID), item.itemId,
                              getItemColumn(ItemRecord.OWNER_ID), item.ownerId);
            key = new Key<T>(getItemClass(), ItemRecord.ITEM_ID, item.itemId);
        }
        int modifiedRows =  updatePartial(
            item.itemId < 0 ? getCloneClass() : getItemClass(), where, key,
            ItemRecord.OWNER_ID, newOwnerId,
            ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
        if (modifiedRows == 0) {
            throw new DatabaseException("Failed to safely update ownerId [item=" + item +
                                        ", newOwnerId=" + newOwnerId + "]");
        }
    }

    public void incrementFavoriteCount (CatalogRecord record, int increment)
    {
        Map<String, SQLExpression> fieldsToValues = Maps.newHashMap();
        Arithmetic.Add add = new Arithmetic.Add(
            new ColumnExp(getCatalogClass(), CatalogRecord.FAVORITE_COUNT), increment);
        fieldsToValues.put(CatalogRecord.FAVORITE_COUNT, add);

        if (updateLiteral(getCatalogClass(), record.catalogId, fieldsToValues) == 0) {
            log.warning("Could not update favorite count on catalog record.",
                        "catalogId", record.catalogId, "increment", increment);
        }
    }

    /**
     * Notes that the specified original item is now associated with the specified catalog listed
     * item (which may be zero to clear out a listing link).
     */
    protected void noteListing (int originalItemId, int catalogId)
    {
        updatePartial(getItemClass(), originalItemId, ItemRecord.CATALOG_ID, catalogId);
    }

    /**
     * Performs the necessary join to load cloned items matching the supplied where clause.
     */
    protected List<T> loadClonedItems (Where where, QueryClause... clauses)
    {
        // find the appropriate CloneRecords (in the order specified by the passed-in clauses)
        List<QueryClause> clauseList = new ArrayList<QueryClause>(clauses.length + 2);
        clauseList.add(where);
        Collections.addAll(clauseList, clauses);
        clauseList.add(new Join(getCloneClass(), CloneRecord.ORIGINAL_ITEM_ID,
            getItemClass(), ItemRecord.ITEM_ID));
        return resolveClones(findAll(getCloneClass(), clauseList));
    }

    /**
     * Resolves clone records into full item records.
     */
    protected List<T> resolveClones (List<CloneRecord> clones)
    {
        // our work here is done if we didn't find any
        if (clones.isEmpty()) {
            return new ArrayList<T>();
        }

        // create a set of the corresponding original ids
        ArrayIntSet origIds = new ArrayIntSet(clones.size());
        for (CloneRecord clone : clones) {
            origIds.add(clone.originalItemId);
        }

        // find all the originals and insert them into a map
        List<T> originals = loadAll(getItemClass(), origIds);
        HashIntMap<T> records = new HashIntMap<T>(originals.size(), HashIntMap.DEFAULT_LOAD_FACTOR);
        for (T record : originals) {
            records.put(record.itemId, record);
        }

        // now traverse each clone in the originally-returned order and fill in
        // a clone of the ItemRecord to return.
        List<T> results = new ArrayList<T>(clones.size());
        for (CloneRecord clone : clones) {
            // we could just return the record directly, except that we could be loading
            // more than one clone that uses the same original
            T record = records.get(clone.originalItemId);
            @SuppressWarnings(value="unchecked")
            T returnCopy = (T) record.clone();
            returnCopy.initFromClone(clone);
            results.add(returnCopy);
        }

        return results;
    }

    /**
     * Return an sql operator representing a search for the given string in the item's
     * name, description, tags and creator name.
     *
     * an item matches the search query either if there is a full-text match against name
     * and/or description, or if one or more of the search words match one or more of the
     * item's tags; this is accomplished with an 'exists' subquery
     */
    protected SQLOperator buildSearchStringClause (String search)
    {
        List<SQLOperator> matches = Lists.newArrayList();
        // search item name and description
        matches.add(new FullTextMatch(getItemClass(), ItemRecord.FTS_ND, search));

        // look up the first 100 members whose name matches the search term exactly
        List<Integer> memberIds = _memberRepo.findMembersByDisplayName(search, true, 100);
        if (memberIds.size() > 0) {
            matches.add(new In(getItemColumn(ItemRecord.CREATOR_ID), memberIds));
        }

        // to match tags we first have to split our search up into words
        String[] searchTerms = search.toLowerCase().split("\\W+");
        if (searchTerms.length > 0 && searchTerms[0].length() == 0) {
            searchTerms = ArrayUtil.splice(searchTerms, 0, 1);
        }

        // look up each word as a tag
        IntSet tagIds = new ArrayIntSet();
        if (searchTerms.length > 0) {
            for (TagNameRecord tRec : getTagRepository().getTags(searchTerms)) {
                tagIds.add(tRec.tagId);
            }
        }

        // build a query to check tags if one or more tags exists
        if (tagIds.size() > 0) {
            Where where = new Where(
                new And(new Equals(getTagColumn(TagRecord.TARGET_ID),
                                   getCatalogColumn(CatalogRecord.LISTED_ITEM_ID)),
                        new In(getTagColumn(TagRecord.TAG_ID), tagIds)));
            matches.add(new Exists<TagRecord>(new SelectClause<TagRecord>(
                getTagRepository().getTagClass(), new String[] { TagRecord.TAG_ID }, where)));
        }

        // if we ended up with multiple match clauses, OR them all together
        return (matches.size() == 1) ? matches.get(0) : new Or(matches);
    }

    /**
     * Helper function for {@link #countListings} and {@link #loadCatalog}.
     */
    protected void addSearchClause (List<QueryClause> clauses, boolean mature, String search,
                                    int tag, int creator, Float minRating, int suiteId)
    {
        List<SQLOperator> whereBits = Lists.newArrayList();

        if (search != null && search.length() > 0) {
            whereBits.add(buildSearchStringClause(search));
        }

        if (tag > 0) {
            // join against TagRecord
            clauses.add(new Join(getCatalogClass(), CatalogRecord.LISTED_ITEM_ID,
                                 getTagRepository().getTagClass(), TagRecord.TARGET_ID));
            // and add a condition
            whereBits.add(new Equals(getTagColumn(TagRecord.TAG_ID), tag));
        }

        if (creator > 0) {
            whereBits.add(new Equals(getItemColumn(ItemRecord.CREATOR_ID), creator));
        }

        if (!mature) {
            // add a check to make sure ItemRecord.FLAG_MATURE is not set on any returned items
            whereBits.add(new Equals(getItemColumn(ItemRecord.MATURE), false));
        }

        if (minRating != null) {
            whereBits.add(new Conditionals.GreaterThanEquals(
                getItemColumn(ItemRecord.RATING), minRating));
        }

        if (suiteId > 0 && isSubItem()) {
            whereBits.add(new Equals(getItemColumn(SubItemRecord.SUITE_ID), suiteId));
        }

        whereBits.add(new Not(new Equals(getCatalogColumn(CatalogRecord.PRICING),
                                         CatalogListing.PRICING_HIDDEN)));

        clauses.add(new Where(new And(whereBits)));
    }

    protected void addOrderByListDate (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getCatalogColumn(CatalogRecord.LISTED_DATE));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByRating (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getItemColumn(ItemRecord.RATING));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByPrice (List<SQLExpression> exprs, List<OrderBy.Order> orders,
                                    OrderBy.Order order)
    {
        exprs.add(new Arithmetic.Add(getCatalogColumn(CatalogRecord.FLOW_COST),
                                     new Arithmetic.Mul(getCatalogColumn(CatalogRecord.GOLD_COST),
                                                        FLOW_FOR_GOLD)));
        orders.add(order);
    }

    protected void addOrderByPurchases (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        // TODO: someday make an indexed column that represents (purchases-returns)
        exprs.add(getCatalogColumn(CatalogRecord.PURCHASES));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByNewAndHot (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        long nowSeconds = System.currentTimeMillis() / 1000;
        exprs.add(new Arithmetic.Sub(getItemColumn(ItemRecord.RATING),
            new Arithmetic.Div(
                new Arithmetic.Sub(new ValueExp(nowSeconds),
                    new EpochSeconds(getCatalogColumn(CatalogRecord.LISTED_DATE))),
                _newAndHotDropoffSeconds)));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByFavorites (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getCatalogColumn(CatalogRecord.FAVORITE_COUNT));
        orders.add(OrderBy.Order.DESC);
    }

    protected ColumnExp getItemColumn (String cname)
    {
        return new ColumnExp(getItemClass(), cname);
    }

    protected ColumnExp getCatalogColumn (String cname)
    {
        return new ColumnExp(getCatalogClass(), cname);
    }

    protected ColumnExp getCloneColumn (String cname)
    {
        return new ColumnExp(getCloneClass(), cname);
    }

    protected ColumnExp getRatingColumn (String cname)
    {
        return new ColumnExp(getRatingClass(), cname);
    }

    protected ColumnExp getTagColumn (String cname)
    {
        return new ColumnExp(getTagRepository().getTagClass(), cname);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(getItemClass());
        classes.add(getCloneClass());
        classes.add(getCatalogClass());
        classes.add(getRatingClass());
    }

    /**
     * Checks whether the Item class for this repository is a SubItemRecord.
     */
    protected boolean isSubItem ()
    {
        return SubItemRecord.class.isAssignableFrom(getItemClass());
    }

    /**
     * Specific item repositories override this method and indicate the class of item on which they
     * operate.
     */
    protected abstract Class<T> getItemClass ();

    /**
     * Specific item repositories override this method and indicate their item's clone persistent
     * record class.
     */
    protected abstract Class<CloneRecord> getCloneClass ();

    /**
     * Specific item repositories override this method and indicate their item's catalog persistent
     * record class.
     */
    protected abstract Class<CatalogRecord> getCatalogClass ();

    /**
     * Specific item repositories override this method and indicate their item's rating persistent
     * record class.
     */
    protected abstract Class<RatingRecord> getRatingClass ();

    /**
     * Specific item repositories override this method and indicate their item's tag persistent
     * record class.
     */
    protected abstract TagRecord createTagRecord ();

    /**
     * Specific item repositories override this method and indicate their item's tag history
     * persistent record class.
     */
    protected abstract TagHistoryRecord createTagHistoryRecord ();

    /** Used to coerce CatalogRecord derivations when implementing {@link #getCatalogClass}. */
    protected static Class<CatalogRecord> coerceCatalog (Class<? extends CatalogRecord> clazz)
    {
        @SuppressWarnings("unchecked") Class<CatalogRecord> cclazz = (Class<CatalogRecord>)clazz;
        return cclazz;
    }

    /** Used to coerce CloneRecord derivations when implementing {@link #getCloneClass}. */
    protected static Class<CloneRecord> coerceClone (Class<? extends CloneRecord> clazz)
    {
        @SuppressWarnings("unchecked") Class<CloneRecord> cclazz = (Class<CloneRecord>)clazz;
        return cclazz;
    }

    /** Used to coerce RatingRecord derivations when implementing {@link #getRatingClass}. */
    protected static Class<RatingRecord> coerceRating (Class<? extends RatingRecord> clazz)
    {
        @SuppressWarnings("unchecked") Class<RatingRecord> cclazz = (Class<RatingRecord>)clazz;
        return cclazz;
    }

    /** The byte type of our item. */
    protected byte _itemType;

    /** Used to manage our item tags. */
    protected TagRepository _tagRepo;

    // our dependencies
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MemberRepository _memberRepo;

    /** The number of seconds that causes an equivalent drop-off of 1 star in new & hot sorting. */
    protected static int _newAndHotDropoffSeconds = 7 * 60 * 60 * 24;

    /** The minimum number of purchases before we'll start attenuating price based on returns. */
    protected static final int MIN_ATTEN_PURCHASES = 5;

    /** The minimum number of ratings required to qualify a rating as "solid" */
    protected static final int MIN_SOLID_RATINGS = 20;
}
