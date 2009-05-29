//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.IntSet;
import com.samskivert.util.QuickSort;

import com.samskivert.depot.CacheInvalidator.TraverseWithFilter;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.SelectClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Add;
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.Div;
import com.samskivert.depot.operator.Equals;
import com.samskivert.depot.operator.Exists;
import com.samskivert.depot.operator.FullText;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.GreaterThanEquals;
import com.samskivert.depot.operator.In;
import com.samskivert.depot.operator.LessThan;
import com.samskivert.depot.operator.LessThanEquals;
import com.samskivert.depot.operator.Mul;
import com.samskivert.depot.operator.Not;
import com.samskivert.depot.operator.Or;
import com.samskivert.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.HotnessConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.RecordFunctions;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

import com.threerings.msoy.money.data.all.Currency;

import com.threerings.msoy.room.server.persist.MemoryRepository;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ItemPrices;

import com.threerings.msoy.money.server.MoneyExchange;

import static com.threerings.msoy.Log.log;

/**
 * Manages a repository of digital items of a particular type.
 */
@BlockingThread
public abstract class ItemRepository<T extends ItemRecord>
    extends DepotRepository
{
    @Entity @Computed
    public static class OwnerIdRecord extends PersistentRecord {
        public int itemId;
        public int ownerId;
    }

    /**
     * Encapsulates information regarding a word search, for catalog items or stuff: we look up
     * each word as a tag, we look up each word as one or more creators, and we create a full-
     * text query for it. The resulting Depot expressions are used both to filter and to rank
     * search results.
     */
    public class WordSearch
    {
        public SQLOperator fullTextMatch ()
        {
            return _fts.match();
        }

        public SQLOperator fullTextRank ()
        {
            return _fts.rank();
        }

        public SQLOperator tagExistsExpression (ColumnExp itemColumn)
        {
            if (_tagIds.size() == 0) {
                return null;
            }
            Where where = new Where(
                new And(new Equals(getTagColumn(TagRecord.TARGET_ID), itemColumn),
                        new In(getTagColumn(TagRecord.TAG_ID), _tagIds)));
            return new Exists<TagRecord>(new SelectClause<TagRecord>(
                getTagRepository().getTagClass(), new String[] { TagRecord.TAG_ID.name }, where));
        }

        public SQLOperator madeByExpression ()
        {
            if (_memberIds.size() == 0) {
                return null;
            }
            return new In(getItemColumn(ItemRecord.CREATOR_ID), _memberIds);
        }

        protected WordSearch (String search)
        {
            // first split our search up into words
            String[] searchTerms = search.toLowerCase().split("\\W+");
            if (searchTerms.length > 0 && searchTerms[0].length() == 0) {
                searchTerms = ArrayUtil.splice(searchTerms, 0, 1);
            }

            // look up each word as a tag
            _tagIds = new ArrayIntSet();
            if (searchTerms.length > 0) {
                for (TagNameRecord tRec : getTagRepository().getTags(searchTerms)) {
                    _tagIds.add(tRec.tagId);
                }
            }

            _memberIds = new ArrayIntSet();
            // look up the first 100 members whose name matches each search term exactly
            for (String term : searchTerms) {
                _memberIds.addAll(_memberRepo.findMembersByExactDisplayName(term, 100));
            }

            _fts = new FullText(getItemClass(), ItemRecord.FTS_ND, search);
        }

        protected IntSet _tagIds;
        protected IntSet _memberIds;
        protected FullText _fts;
    }

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

        _ratingRepo = new RatingRepository(ctx, getItemColumn(ItemRecord.ITEM_ID),
            getItemColumn(ItemRecord.RATING_SUM), getItemColumn(ItemRecord.RATING_COUNT)) {
            @Override
            protected Class<? extends PersistentRecord> getTargetClass () {
                return ItemRepository.this.getItemClass();
            }
            @Override
            protected Class<RatingRecord> getRatingClass () {
                return ItemRepository.this.getRatingClass();
            }
        };

        // drop the now unused ItemRecord.rating column
        _ctx.registerMigration(getItemClass(), new SchemaMigration.Drop(
                                   21 * ItemRecord.BASE_MULTIPLIER, "rating"));

        // change suiteId -> gameId
        if (PropRecord.class.isAssignableFrom(getItemClass())) {
            _ctx.registerMigration(getItemClass(), new SchemaMigration.Drop(
                                       22 * ItemRecord.BASE_MULTIPLIER, "gameId"));
        }
        if (GameItemRecord.class.isAssignableFrom(getItemClass())) {
            _ctx.registerMigration(getItemClass(), new SchemaMigration.DropIndex(
                                       22 * ItemRecord.BASE_MULTIPLIER, "ixSuiteId"));
            _ctx.registerMigration(getItemClass(), new SchemaMigration.Rename(
                                       22 * ItemRecord.BASE_MULTIPLIER,
                                       "suiteId", GameItemRecord.GAME_ID));
        }
    }

    /**
     * Configures this repository with its item type
     */
    public void init (byte itemType)
    {
        _itemType = itemType;

        // TEMP: remove a few weeks after 2009/02/24
        registerMigration(new DataMigration("2009_02_24_minprice_" + _itemType) {
            @Override public void invoke () throws DatabaseException {
                int[] byrating = new int[5];
                int adjusted = 0;
                for (byte rating : new byte[] { 5, 4, 3, 2, 1 }) {
                    int minPrice = ItemPrices.getMinimumPrice(Currency.COINS, _itemType, rating);
                    // this is basically what we're doing except not as an update clause:
                    // update CatalogRecord set pricing = minPrice
                    // join ItemRecord on ItemRecord.listedItemId = ItemRecord.itemId
                    // where currency = coins and rating() <= rating and rating() > (rating-1)
                    // and cost < minPrice
                    Join join = new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                                         getItemColumn(ItemRecord.ITEM_ID));
                    Where where = new Where(
                        new And(new Equals(getCatalogColumn(CatalogRecord.CURRENCY),
                                           Currency.COINS.toByte()),
                                new LessThanEquals(getRatingExpression(), rating),
                                new GreaterThan(getRatingExpression(), rating-1),
                                new LessThan(getCatalogColumn(CatalogRecord.COST), minPrice)));
                    for (CatalogRecord crec : findAll(getCatalogClass(), join, where)) {
                        updatePartial(getCatalogKey(crec.catalogId), CatalogRecord.COST, minPrice);
                        adjusted++;
                        byrating[rating-1]++;
                    }
                }
                log.info("Enforced minimum prices", "type", getItemClass().getSimpleName(),
                         "total", adjusted, "byrating", byrating);
            }
        });
        // END TEMP
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
     * Returns the repository that manages ratings for this item.
     */
    public RatingRepository getRatingRepository ()
    {
        return _ratingRepo;
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
        return load(getItemKey(itemId));
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
     * Loads all original items owned by the specified member.
     */
    public List<T> loadOriginals (int ownerId)
    {
        return findAll(getItemClass(), new Where(getItemColumn(ItemRecord.OWNER_ID), ownerId));
    }

    /**
     * Loads all original game items for the specified game.
     */
    public List<T> loadGameOriginals (int gameId)
    {
        return findAll(getItemClass(), new Where(getItemColumn(GameItemRecord.GAME_ID), gameId));
    }

    /**
     * Loads all original game items for the specified game owned by the specified member.
     */
    public List<T> loadGameOriginals (int gameId, int ownerId)
    {
        return findAll(getItemClass(), new Where(getItemColumn(GameItemRecord.GAME_ID), gameId,
                                                 getItemColumn(ItemRecord.OWNER_ID), ownerId));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public List<T> loadClones (int ownerId)
    {
        return loadClonedItems(new Where(getCloneColumn(CloneRecord.OWNER_ID), ownerId));
    }

    /**
     * Loads all cloned game items for the specified game owned by the specified member.
     */
    public List<T> loadGameClones (int gameId, int ownerId)
    {
        return loadClonedItems(new Where(getItemColumn(GameItemRecord.GAME_ID), gameId,
                                         getCloneColumn(CloneRecord.OWNER_ID), ownerId));
    }

    /**
     * Finds all (original and cloned) items owned by the specified player that match the supplied
     * query.
     */
    public List<T> findItems (int ownerId, String query)
    {
        WordSearch queryContext = buildWordSearch(query);
        List<SQLOperator> matches = Lists.newArrayList();

        // original items only match on the text and creator (they cannot be tagged)
        if (queryContext != null) {
            addTextMatchClause(matches, queryContext);
            addCreatorMatchClause(matches, queryContext);
        }

        // locate all matching original items
        List<T> results = findAll(getItemClass(), new Where(
            new And(new Equals(getItemColumn(ItemRecord.OWNER_ID), ownerId),
                    makeSearchClause(matches))));

        // now add the tag match as cloned items can match tags
        if (queryContext != null) {
            addTagMatchClause(matches, getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID), queryContext);
        }

        // add all matching cloned items
        results.addAll(loadClonedItems(new Where(
            new And(new Equals(getCloneColumn(CloneRecord.OWNER_ID), ownerId),
                    makeSearchClause(matches)))));

        return results;
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

        List<T> list = Lists.newArrayListWithCapacity(size);
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
    public List<T> loadItems (Collection<Integer> itemIds)
    {
        List<T> items = resolveClones(loadAll(getCloneClass(), getCloneIds(itemIds)));
        items.addAll(loadAll(getItemClass(), getOriginalIds(itemIds)));
        return items;
    }

    /**
     * Loads the specified items in the specified order. Omits missing items from results.
     * Similar to ItemLogic.loadItems(), but only for this repository's item type.
     */
    public List<T> loadItemsInOrder (List<Integer> itemIds)
    {
        List<T> items = loadItems(itemIds);
        Map<Integer, T> idMap = Maps.newHashMapWithExpectedSize(items.size());
        for (T item : items) {
            idMap.put(item.itemId, item);
        }
        List<T> result = Lists.newArrayListWithCapacity(items.size());
        for (Integer id : itemIds) {
            T item = idMap.get(id);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Loads a single clone record by item id.
     */
    public CloneRecord loadCloneRecord (int itemId)
    {
        return load(getCloneKey(itemId));
    }

    /**
     * Returns the count of clones of the specified master item.
     */
    public int loadCloneRecordCount (int itemId)
    {
        return load(CountRecord.class,
                    new FromOverride(getCloneClass()),
                    new Where(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID), itemId)).count;
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
    public void markItemUsage (Collection<Integer> itemIds, Item.UsedAs usageType, int location)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (int itemId : itemIds) {
            int result;
            if (itemId > 0) {
                result = updatePartial(
                    getItemKey(itemId), ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, now);
            } else {
                result = updatePartial(
                    getCloneKey(itemId), ItemRecord.USED, usageType, ItemRecord.LOCATION, location,
                    ItemRecord.LAST_TOUCHED, now);
            }
            // if the item didn't update, point that out to log readers
            if (0 == result) {
                log.info("Attempt to mark item usage matched zero rows", "type", _itemType,
                    "itemId", itemId, "usageType", usageType, "location", location);
            }
        }
    }

    /**
     * Transfer all items owned by the old owner and in-use in the specified scene.
     */
    public void transferRoomItems (int sceneId, int oldOwnerId, int newOwnerId)
    {
        Preconditions.checkArgument(sceneId != 0);
        Preconditions.checkArgument(getItemClass() != AvatarRecord.class);

        // TODO: non-null cache invalidators?
        updatePartial(getItemClass(), new Where(
            getItemColumn(ItemRecord.OWNER_ID), oldOwnerId,
            getItemColumn(ItemRecord.LOCATION), sceneId),
            null,
            getItemColumn(ItemRecord.OWNER_ID), newOwnerId);
        updatePartial(getCloneClass(), new Where(
            getCloneColumn(CloneRecord.OWNER_ID), oldOwnerId,
            getCloneColumn(CloneRecord.LOCATION), sceneId),
            null,
            getCloneColumn(CloneRecord.OWNER_ID), newOwnerId);
    }

    /**
     * Loads up the owner information for the supplied set of items. The ids may include original
     * and clone records.
     */
    public IntIntMap loadOwnerIds (Collection<Integer> itemIds)
    {
        IntIntMap ownerIds = new IntIntMap();
        Set<Integer> origIds = getOriginalIds(itemIds);
        if (origIds.size() > 0) {
            // we can't use findAll() here because we're doing dynamic FromOverride magic
            for (OwnerIdRecord oidrec : findAll(
                     OwnerIdRecord.class, new FromOverride(getItemClass()),
                     new FieldDefinition(ItemRecord.ITEM_ID, getItemColumn(ItemRecord.ITEM_ID)),
                     new FieldDefinition(ItemRecord.OWNER_ID, getItemColumn(ItemRecord.OWNER_ID)),
                     new Where(new In(getItemColumn(ItemRecord.ITEM_ID), origIds)))) {
                ownerIds.put(oidrec.itemId, oidrec.ownerId);
            }
        }
        Set<Integer> cloneIds = getCloneIds(itemIds);
        if (cloneIds.size() > 0) {
            // we can't use findAll() here because we're doing dynamic FromOverride magic
            for (OwnerIdRecord oidrec : findAll(
                     OwnerIdRecord.class, new FromOverride(getCloneClass()),
                     new FieldDefinition(CloneRecord.ITEM_ID, getCloneColumn(CloneRecord.ITEM_ID)),
                     new FieldDefinition(CloneRecord.OWNER_ID,
                                         getCloneColumn(CloneRecord.OWNER_ID)),
                     new Where(new In(getCloneColumn(CloneRecord.ITEM_ID), cloneIds)))) {
                ownerIds.put(oidrec.itemId, oidrec.ownerId);
            }
        }
        return ownerIds;
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

        List<CatalogRecord> records = findAll(
            getCatalogClass(),
            new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                     getItemColumn(ItemRecord.ITEM_ID)),
            new Limit(0, 1),
            OrderBy.random(),
            new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                     getTagRepository().getTagColumn(TagRecord.TARGET_ID)),
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
    public int countListings (boolean mature, WordSearch search, int tag, int creator,
                              Float minRating)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(getCatalogClass()));
        clauses.add(new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                             getItemColumn(ItemRecord.ITEM_ID)));

        // see if there's any where bits to turn into an actual where clause
        List<SQLOperator> whereBits = Lists.newArrayList();
        addSearchClause(clauses, whereBits, mature, search, tag, creator, minRating, 0);

        // finally fetch all the catalog records of interest
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * A simplified method for loading the top catalog records.
     */
    public List<CatalogRecord> loadCatalog (byte sortBy, int rows)
    {
        return loadCatalog(sortBy, false, null, 0, 0, null, 0, 0, rows);
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
    public List<CatalogRecord> loadCatalog (
        byte sortBy, boolean mature, WordSearch context, int tag, int creator,
        Float minRating, int gameId, int offset, int rows)
    {
        LinkedList<QueryClause> clauses = Lists.newLinkedList();
        clauses.add(new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                             getItemColumn(ItemRecord.ITEM_ID)));

        // sort out the primary and secondary order by clauses
        List<SQLExpression> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        // and keep track of additional constraints on the query
        List<SQLOperator> whereBits = Lists.newArrayList();
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
        case CatalogQuery.SORT_BY_RELEVANCE:
            if (context != null) {
                addOrderByRelevance(obExprs, obOrders, context);
            } // else a hacked URL, give'm unordered results
            break;
        default:
            throw new IllegalArgumentException(
                "Sort method not implemented [sortBy=" + sortBy + "]");
        }
        if (obExprs.size() > 0) {
            clauses.add(new OrderBy(
                obExprs.toArray(new SQLExpression[obExprs.size()]),
                obOrders.toArray(new OrderBy.Order[obOrders.size()])).
                    thenAscending(getCatalogColumn(CatalogRecord.CATALOG_ID)));
        }

        // see if there's any where bits to turn into an actual where clause
        boolean significantlyConstrained = addSearchClause(
            clauses, whereBits, mature, context, tag, creator, minRating, gameId);

        // finally fetch all the catalog records of interest and resolve their item bits
        List<CatalogRecord> records = findAllWithOffset(getCatalogClass(),
            (!significantlyConstrained && sortBy == CatalogQuery.SORT_BY_NEW_AND_HOT) ?
                CacheStrategy.CONTENTS : CacheStrategy.LONG_KEYS, clauses,
                offset, rows);

        return resolveCatalogRecords(records);
    }

    /**
     * A request for N records of at offset O of a certain query is mapped to one or possibly
     * several database queries whose offsets always fall on even {@link #FIND_ALL_CHUNK}
     * integer boundaries, and which generally retrieve fairly large numbers of results.
     *
     * Or, in language that someone might actually understand, illustrated with an example,
     * where we pretend that FIND_ALL_CHUNK is 20:
     *
     *  - Someone looks at the three first result pages of a search, resulting in identical
     *    requests for data with offsets 0, 8 and 16, all with limit 8.
     *  - To satisfy the first two, we request 20 records beginning at offset 0.
     *  - To satisfy the third, we additionally request 20 records beginning at offset 20.
     *
     *  What's the gain? Caching is vital, obviously; in practice we end up doing two database
     *  requests instead of three, and the idea is that requesting 20 records costs pretty much
     *  the same as requesting 8. In practice, FIND_ALL_CHUNK is larger, so the caching gain is
     *  higher.
     *
     * This algorithm requires a collection-query-friendly cache strategy, and makes sense mostly
     * for queries whose execution cost is dominated by an expensive OrderBy operation.
     */
    protected <V extends PersistentRecord> List<V> findAllWithOffset (
        Class<V> pClass, CacheStrategy strategy, LinkedList<QueryClause> clauses,
        int queryIx, int toRead)
    {
        if (strategy == CacheStrategy.NONE || strategy == CacheStrategy.RECORDS) {
            throw new IllegalArgumentException(
                "This algorithm should only be used for cached collection queries.");
        }

        List<V> results = Lists.newArrayList();
        Limit limit = null;

        do {
            // where within a chunk is the data to be found?
            int queryIxInChunk = queryIx % FIND_ALL_CHUNK;
            // find the nearest even chunk boundary (truncated)
            int chunkOffset = queryIx - queryIxInChunk;

            // delete the previous limit, if any; it'll always be at the head of the list
            if (limit != null) {
                clauses.remove(0);
            }
            // and insert a new one, always fetching FIND_ALL_CHUNK items
            limit = new Limit(chunkOffset, FIND_ALL_CHUNK);
            clauses.add(0, limit);

            // fetch the chunk from the database (or from the cache, hopefully)
            List<V> chunk = findAll(pClass, strategy, clauses);

            // figure out how much of the data we read is going to be included in our original
            // request, taking into account that we did not necessarily get a full chunk back
            int relevantInChunk = Math.min(Math.max(0, chunk.size() - queryIxInChunk), toRead);

            if (relevantInChunk > 0) {
                // if any of it is relevant, append it and update our iteration variables
                results.addAll(chunk.subList(queryIxInChunk, queryIxInChunk + relevantInChunk));
                toRead -= relevantInChunk;
                queryIx += relevantInChunk;
            }

            if (chunk.size() < FIND_ALL_CHUNK) {
                // regardless of the original toRead limit, if we read less than a complete chunk,
                // the stream is dry, and we're definitely done -- this is also going to be true
                // anytime relevantInChunk is zero
                toRead = 0;
            }
        } while (toRead > 0);

        return results;
    }

    /**
     * Loads up the specified catalog records.
     */
    public List<CatalogRecord> loadCatalog (Collection<Integer> catalogIds)
    {
        return resolveCatalogRecords(loadAll(getCatalogClass(), catalogIds));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        List<Integer> deletedIds = Lists.newArrayList();

        // delete all purchased clones
        List<Key<CloneRecord>> clones = findAllKeys(
            getCloneClass(), false,
            new Where(new In(getCloneColumn(CloneRecord.OWNER_ID), memberIds)));
        deleteAll(getCloneClass(), KeySet.newKeySet(getCloneClass(), clones));
        deletedIds.addAll(Lists.transform(clones, RecordFunctions.<CloneRecord>getIntKey()));

        // delete all original items that are not listed in the catalog; we could delete the
        // catalog originals but that would make repricing or otherwise fiddling with the catalog
        // listings on the part of the support staff more of a PITA, so we'll leave 'em for now
        List<Key<T>> origs = findAllKeys(
            getItemClass(), false,
            new Where(new And(new In(getItemColumn(ItemRecord.OWNER_ID), memberIds),
                              new Not(new Equals(getItemColumn(ItemRecord.CATALOG_ID), 0)))));
        deleteAll(getItemClass(), KeySet.newKeySet(getItemClass(), origs));
        deletedIds.addAll(Lists.transform(origs, RecordFunctions.<T>getIntKey()));

        // now delete memories for all of the deleted items
        if (!deletedIds.isEmpty()) {
            _memoryRepo.purgeMemories(_itemType, deletedIds);
        }

        // delete tag and rating history for these members
        _tagRepo.purgeMembers(memberIds);
        _ratingRepo.purgeMembers(memberIds);
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

        // match up the listed items to their catalog records, filtering any records that are
        // missing a listed item (shouldn't be possible but of course we're seeing it happen)
        List<CatalogRecord> nrecords = Lists.newArrayList();
        for (CatalogRecord record : records) {
            record.item = map.get(record.listedItemId);
            if (record.item == null) {
                log.warning("CatalogRecord missing listed item " +
                            record.getClass().getName() + record + "!");
            } else {
                nrecords.add(record);
            }
        }
        return nrecords;
    }

    /**
     * Load a single catalog listing.
     */
    public CatalogRecord loadListing (int catalogId, boolean loadListedItem)
    {
        CatalogRecord record = load(getCatalogKey(catalogId));
        if (record != null && loadListedItem) {
            record.item = load(getItemKey(record.listedItemId));
        }
        return record;
    }

    /**
     * Update either the 'purchases' or the 'returns' field of a catalog listing, and figure out if
     * it's time to reprice it.
     * @return the newly assigned cost or the original cost if it did not change
     */
    public int nudgeListing (CatalogRecord record, boolean purchased)
    {
        int newCost = record.cost;
        Map<ColumnExp, SQLExpression> updates = Maps.newHashMap();
        if (purchased) {
            updates.put(CatalogRecord.PURCHASES,
                        new Add(getCatalogColumn(CatalogRecord.PURCHASES), 1));

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
                    newCost = CatalogListing.escalatePrice(record.cost);
                    updates.put(CatalogRecord.COST, new LiteralExp(""+newCost));
                }
                break;
            }

        } else {
            updates.put(CatalogRecord.RETURNS, new Add(getCatalogColumn(CatalogRecord.RETURNS), 1));
        }

        // finally update the columns we actually modified
        updateLiteral(getCatalogKey(record.catalogId), updates);
        return newCost;
    }

    /**
     * Inserts the supplied item into the database. The {@link ItemRecord#itemId} and the
     * {@link ItemRecord#lastTouched} fields will be filled in as a result of this call.
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
     * Updates the supplied item in the database. The {@link ItemRecord#lastTouched} field
     * will be filled in as a result of this call.
     */
    public void updateOriginalItem (T item)
    {
        updateOriginalItem(item, true);
    }

    /**
     * Updates the supplied item in the database. The {@link ItemRecord#lastTouched} field
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
     * The {@link CloneRecord#lastTouched} field will be filled in as a result of this call.
     */
    public void updateCloneMedia (CloneRecord cloneRec)
    {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        cloneRec.lastTouched = now;
        cloneRec.mediaStamp = (cloneRec.mediaHash == null) ? null : now;
        updatePartial(getCloneKey(cloneRec.itemId),
                      CloneRecord.MEDIA_HASH, cloneRec.mediaHash,
                      CloneRecord.MEDIA_STAMP, cloneRec.mediaStamp,
                      CloneRecord.LAST_TOUCHED, cloneRec.lastTouched);
    }

    /**
     * Updates a clone item's override name in the database.
     * The {@link CloneRecord#lastTouched} field will be filled in as a result of this call.
     */
    public void updateCloneName (CloneRecord cloneRec)
    {
        cloneRec.lastTouched = new Timestamp(System.currentTimeMillis());
        updatePartial(getCloneKey(cloneRec.itemId),
                      CloneRecord.NAME, cloneRec.name,
                      CloneRecord.LAST_TOUCHED, cloneRec.lastTouched);
    }

    /**
     * Create a row in our catalog table with the given master item record. {@link
     * ItemRecord#catalogId} will be filled into the supplied master.
     *
     * @return the catalog id of the newly inserted listing.
     */
    public int insertListing (ItemRecord master, int originalItemId, int pricing, int salesTarget,
                              Currency currency, int cost, long listingTime, int basisCatalogId)
    {
        if (master.ownerId != 0) {
            throw new IllegalArgumentException(
                "Can't list item with owner [itemId=" + master.itemId + "]");
        }

        CatalogRecord record;
        try {
            record = getCatalogClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.item = master;
        record.listedItemId = master.itemId;
        record.originalItemId = originalItemId;
        record.listedDate = new Timestamp(listingTime);
        record.pricing = pricing;
        record.salesTarget = salesTarget;
        record.purchases = record.returns = 0;
        record.currency = currency;
        record.cost = cost;
        record.basisId = basisCatalogId;
        insert(record);

        // wire this listed item and its original up to the catalog record
        noteListing(record.listedItemId, record.catalogId);
        noteListing(originalItemId, record.catalogId);
        if (record.basisId > 0) {
            noteBasisAssigned(record.basisId, true);
        }

        // fill this in for the caller
        master.catalogId = record.catalogId;

        return record.catalogId;
    }

    /**
     * Updates the pricing for the specified catalog listing. This does not address the rules
     * pertaining to basis items: the caller must deal with derivative listings.
     */
    public void updatePricing (int catalogId, int pricing, int salesTarget,
                               Currency currency, int cost, long updateTime)
    {
        updatePartial(getCatalogKey(catalogId),
                      // TODO?: CatalogRecord.LISTED_DATE, new Timestamp(updateTime),
                      CatalogRecord.PRICING, pricing,
                      CatalogRecord.SALES_TARGET, salesTarget,
                      CatalogRecord.CURRENCY, currency,
                      CatalogRecord.COST, cost);
    }

    /**
     * Updates the basis catalog id for the given specified listing. Also updates the derivation
     * counts of the old and new basis items.
     */
    public void updateBasis (CatalogRecord record, int newBasisId)
    {
        updatePartial(getCatalogKey(record.catalogId), CatalogRecord.BASIS_ID, newBasisId);
        if (record.basisId > 0) {
            noteBasisAssigned(record.basisId, false);
        }
        if (newBasisId > 0) {
            noteBasisAssigned(newBasisId, true);
        }
    }

    /**
     * Updates the cost of all listings that derive from the given basis by given amount. Does not
     * perform any other checks such as making sure currencies are the same.
     * @return the number of records updated
     */
    public int updateDerivedCosts (final int basisId, int change)
    {
        if (change == 0) {
            return 0;
        }
        TraverseWithFilter<CatalogRecord> invalidator =
            new TraverseWithFilter<CatalogRecord>(getCatalogClass()) {
                protected boolean testForEviction (Serializable key, CatalogRecord record) {
                    return record.basisId == basisId;
                }
            };
        Where where = new Where(getCatalogColumn(CatalogRecord.BASIS_ID), basisId);
        ColumnExp cost = getCatalogColumn(CatalogRecord.COST);
        return updatePartial(getCatalogClass(), where, invalidator, cost, new Add(cost, change));
    }

    /**
     * Removes the listing for the specified item from the catalog. This does not address the rules
     * pertaining to basis items: the caller must deal with derivative listings.
     * 
     * @return true if the catalog master was deleted, false if it was left around because it had
     * been purchased one or more times.
     */
    public boolean removeListing (CatalogRecord listing)
    {
        // remove the catalog listing record
        delete(getCatalogKey(listing.catalogId));

        // clear out the listing mappings for the original item
        if (listing.originalItemId != 0) {
            noteListing(listing.originalItemId, 0);
        }
        if (listing.basisId > 0) {
            noteBasisAssigned(listing.basisId, false);
        }
        // if there are no clones of the master record, delete it as well
        boolean masterDeleted = false;
        if (loadCloneRecordCount(listing.listedItemId) == 0) {
            deleteItem(listing.listedItemId);
            masterDeleted = true;
        } else  {
            // otherwise disassociate it from the catalog record as that has gone away
            noteListing(listing.listedItemId, 0);
        }
        return masterDeleted;
    }

    /**
     * Inserts an item clone into the database with the given owner and purchase data. Also fills
     * (@link CloneRecord#itemId) with the next available ID and {@link CloneRecord#ownerId}
     * with the new owner. Finally, updates {@link CloneRecord#lastTouched} and
     * {@link CloneRecord#purchaseTime}.
     */
    public ItemRecord insertClone (
        ItemRecord parent, int newOwnerId, Currency currency, int amountPaid)
    {
        CloneRecord record;
        try {
            record = getCloneClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.initialize(parent, newOwnerId, currency, amountPaid);
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
            delete(getCloneKey(itemId));

        } else {
            // delete the item in question
            delete(getItemKey(itemId));

            // delete rating records for this item (and invalidate the cache properly)
            _ratingRepo.deleteRatings(itemId);

            // delete tag records relating to this item
            _tagRepo.deleteTags(itemId);

            // delete support flags on this item
            _itemFlagRepo.removeItemFlags(getItemType(), itemId);
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
        return _ratingRepo.getRating(itemId, memberId);
    }

    /**
     * Transfers rating records from one record to another. This is used when a catalog listing is
     * updated to migrate the players' individual rating records from the old master item to the
     * new one.
     *
     * <p> Note: this destabilizes the rating of the abandoned previous listing, but that rating is
     * meaningless anyway since the item is no longer in the catalog. Ratings should really be on
     * listings not items, but that's a giant fiasco we don't want to deal with.
     */
    public void reassignRatings (int oldItemId, int newItemId)
    {
        _ratingRepo.reassignRatings(oldItemId, newItemId);
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
            key = getCloneKey(item.itemId);
        } else {
            where = new Where(getItemColumn(ItemRecord.ITEM_ID), item.itemId,
                              getItemColumn(ItemRecord.OWNER_ID), item.ownerId);
            key = getItemKey(item.itemId);
        }
        int modifiedRows = updatePartial(
            key.getPersistentClass(), where, key,
            ItemRecord.OWNER_ID, newOwnerId,
            ItemRecord.LAST_TOUCHED, new Timestamp(System.currentTimeMillis()));
        if (modifiedRows == 0) {
            throw new DatabaseException("Failed to safely update ownerId [item=" + item +
                                        ", newOwnerId=" + newOwnerId + "]");
        }
    }

    /**
     * Increments the favorite count on the specified catalog record by the specified amount.
     */
    public void incrementFavoriteCount (int catalogId, int increment)
    {
        Map<ColumnExp, SQLExpression> updates = Maps.newHashMap();
        updates.put(CatalogRecord.FAVORITE_COUNT, new Add(
                        getCatalogColumn(CatalogRecord.FAVORITE_COUNT), increment));
        if (updateLiteral(getCatalogKey(catalogId), updates) == 0) {
            log.warning("Could not update favorite count on catalog record.",
                        "catalogId", catalogId, "increment", increment);
        }
    }

    /**
     * Loads up the catalog ids of all listings that specify the listing with the given catalog id
     * as a basis.
     * @param maximum maximum number of results to return, or 0 to return all available
     */
    public List<Integer> loadDerivativeIds (int catalogId, int maximum)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        SQLExpression derived = new Equals(getCatalogColumn(CatalogRecord.BASIS_ID), catalogId);
        SQLExpression visible = new Not(new Equals(
            getCatalogColumn(CatalogRecord.PRICING), CatalogListing.PRICING_HIDDEN));
        clauses.add(new Where(new And(derived, visible)));
        if (maximum > 0) {
            clauses.add(new Limit(0, maximum));
        }
        return Lists.transform(findAllKeys(getCatalogClass(), false, clauses),
            new Function<Key<? extends CatalogRecord>, Integer>() {
                public Integer apply (Key<? extends CatalogRecord> key) {
                    return CatalogRecord.getCatalogId(key);
                }
            });
    }

    /**
     * Notes that the specified original item is now associated with the specified catalog listed
     * item (which may be zero to clear out a listing link).
     */
    protected void noteListing (int itemId, int catalogId)
    {
        updatePartial(getItemKey(itemId), ItemRecord.CATALOG_ID, catalogId);
    }

    /**
     * Increments or decrements the derivation count of the given listing.
     */
    protected void noteBasisAssigned (int catalogId, boolean add)
    {
        ColumnExp count = getCatalogColumn(CatalogRecord.DERIVATION_COUNT);
        updatePartial(getCatalogKey(catalogId), count, new Add(count, add ? 1 : -1));
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
        clauseList.add(new Join(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID),
                                getItemColumn(ItemRecord.ITEM_ID)));
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
     * Adds a full-text match on item name and description to the supplied list.
     */
    protected void addTextMatchClause (List<SQLOperator> matches, WordSearch search)
    {
        // search item name and description
        matches.add(search._fts.match());
    }

    /**
     * Adds a match on the name of the creator to the supplied list.
     */
    protected void addCreatorMatchClause (List<SQLOperator> matches, WordSearch search)
    {
        SQLOperator op = search.madeByExpression();
        if (op != null) {
            matches.add(op);
        }
    }

    /**
     * Creates a word search record for the supplied query. This is expensive and involves database
     * lookups, so beware. Returns null if the query is blank.
     */
    public WordSearch buildWordSearch (String query)
    {
        if (query != null && query.trim().length() > 0) {
            return new WordSearch(query);
        }
        return null;
    }

    /**
     * Searches for any tags that match the search string and matches all catalog master items that
     * are tagged with those tags.
     */
    protected void addTagMatchClause (
        List<SQLOperator> matches, ColumnExp itemColumn, WordSearch search)
    {
        // build a query to check tags if one or more tags exists
        SQLOperator op = search.tagExistsExpression(itemColumn);
        if (op != null) {
            matches.add(op);
        }
    }

    /**
     * Composes the supplied list of search match clauses into a single operator.
     */
    protected SQLOperator makeSearchClause (List<SQLOperator> matches)
    {
        // if we ended up with multiple match clauses, OR them all together
        return (matches.size() == 1) ? matches.get(0) : new Or(matches);
    }

    /**
     * Builds a search clause that matches item text, creator name and tags (against listed catalog
     * items).
     */
    protected SQLOperator buildSearchClause (WordSearch queryContext)
    {
        List<SQLOperator> matches = Lists.newArrayList();

        addTextMatchClause(matches, queryContext);
        addCreatorMatchClause(matches, queryContext);
        addTagMatchClause(matches, getCatalogColumn(CatalogRecord.LISTED_ITEM_ID), queryContext);

        return makeSearchClause(matches);
    }

    /**
     * Helper function for {@link #countListings} and {@link #loadCatalog}. Returns true if
     * sufficient clauses were added that we can heuristically claim that the query will not
     * match enormous numbers of rows.
     */
    protected boolean addSearchClause (
        List<QueryClause> clauses, List<SQLOperator> whereBits, boolean mature,
        WordSearch queryContext, int tag, int creator, Float minRating, int gameId)
    {
        boolean significantlyConstrained = false;

        // add our search clauses if we have a search string
        if (queryContext != null) {
            whereBits.add(buildSearchClause(queryContext));
            significantlyConstrained = true;
        }

        if (tag > 0) {
            // join against TagRecord
            clauses.add(new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                                 getTagRepository().getTagColumn(TagRecord.TARGET_ID)));
            // and add a condition
            whereBits.add(new Equals(getTagColumn(TagRecord.TAG_ID), tag));
            significantlyConstrained = true;
        }

        if (creator > 0) {
            whereBits.add(new Equals(getItemColumn(ItemRecord.CREATOR_ID), creator));
            significantlyConstrained = true;
        }

        if (!mature) {
            // add a check to make sure ItemRecord.FLAG_MATURE is not set on any returned items
            whereBits.add(new Equals(getItemColumn(ItemRecord.MATURE), false));
        }

        if (minRating != null) {
            whereBits.add(new GreaterThanEquals(getRatingExpression(), minRating));
        }

        if (gameId != 0 && GameItemRecord.class.isAssignableFrom(getItemClass())) {
            whereBits.add(new Equals(getItemColumn(GameItemRecord.GAME_ID), gameId));
            significantlyConstrained = true;
        }

        whereBits.add(new Not(new Equals(getCatalogColumn(CatalogRecord.PRICING),
                                         CatalogListing.PRICING_HIDDEN)));

        clauses.add(new Where(new And(whereBits)));
        return significantlyConstrained;
    }

    protected void addOrderByListDate (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getCatalogColumn(CatalogRecord.LISTED_DATE));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByRating (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getRatingExpression());
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByPrice (List<SQLExpression> exprs, List<OrderBy.Order> orders,
                                    OrderBy.Order order)
    {
        // Multiply bar prices by the current exchange rate.
        //     adjustedCost = cost * Math.max(1, currencyByteVal * exchangeRate)
        //
        // This depends on two things:
        // - We know that Currency.COINS=0, Currency.BARS=1
        // - if the exchange rate was less than 1, this would value coins and bars equally
        //   instead of making bars worth less... that shouldn't happen though.
        exprs.add(new Mul(getCatalogColumn(CatalogRecord.COST),
                          new FunctionExp("GREATEST", new ValueExp(1),
                                          new Mul(getCatalogColumn(CatalogRecord.CURRENCY),
                                                  new ValueExp(_exchange.getRate())))));
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
        exprs.add(new Add(getRatingExpression(),
                          new Div(new EpochSeconds(getCatalogColumn(CatalogRecord.LISTED_DATE)),
                                  HotnessConfig.DROPOFF_SECONDS)));
        orders.add(OrderBy.Order.DESC);
    }

    // Construct a relevance ordering for item searches
    protected void addOrderByRelevance (
        List<SQLExpression> exprs, List<OrderBy.Order> orders, WordSearch context)
    {
        // The relevance of a catalog entry is a product of several factors, each chosen
        // to have a tunable impact. The actual value is not important, only the relative
        // sizes.
        SQLOperator[] ops = new SQLOperator[] {
            // The base value is just the Full Text Search rank value, the scale of which is
            // entirely unknown. We only give it a tiny linear shift so that the creator and
            // tag factors below have something non-zero to work with when there is no full
            // text hit at all
            new Add(new ValueExp(0.1), context.fullTextRank()),

            // adjust the FTS rank by (5 + rating), which means a 5-star item is rated
            // (approximately) twice as high rated as a 1-star item
            new Add(new ValueExp(1.0), getRatingExpression()),

            // then boost by (3 + log10(purchases)), thus an item that's sold 1,000 copies
            // is rated twice as high as something that's sold 1 copy
            new Add(new ValueExp(3.0),
                    new FunctionExp("LOG", new Add(new ValueExp(1.0),
                                                   getCatalogColumn(CatalogRecord.PURCHASES)))),
        };

        SQLOperator tagExistsExp =
            context.tagExistsExpression(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID));
        if (tagExistsExp != null) {
            // if there is a tag match, immediately boost relevance by 25%
            ops = ArrayUtil.append(ops,
                new Case(tagExistsExp, new ValueExp(1.25), new ValueExp(1.0)));
        }

        SQLOperator madeByExp = context.madeByExpression();
        if (madeByExp != null) {
            // if the item was made by a creator who matches the description, also boost by 50%
            ops = ArrayUtil.append(ops,
                new Case(madeByExp, new ValueExp(1.5), new ValueExp(1.0)));
        }

        exprs.add(new Mul(ops));
        orders.add(OrderBy.Order.DESC);

        exprs.add(getRatingExpression());
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByFavorites (List<SQLExpression> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getCatalogColumn(CatalogRecord.FAVORITE_COUNT));
        orders.add(OrderBy.Order.DESC);
    }

    /**
     * Extracts the ids of original items from the supplied set of mixed ids.
     */
    protected Set<Integer> getOriginalIds (Collection<Integer> itemIds)
    {
        return Sets.newHashSet(Iterables.filter(itemIds, IS_ORIGINAL_ID));
    }

    /**
     * Extracts the ids of cloned items from the supplied set of mixed ids.
     */
    protected Set<Integer> getCloneIds (Collection<Integer> itemIds)
    {
        return Sets.newHashSet(Iterables.filter(itemIds, IS_CLONE_ID));
    }

    protected ColumnExp getItemColumn (ColumnExp pcol)
    {
        return new ColumnExp(getItemClass(), pcol.name);
    }

    protected ColumnExp getCatalogColumn (ColumnExp pcol)
    {
        return new ColumnExp(getCatalogClass(), pcol.name);
    }

    protected ColumnExp getCloneColumn (ColumnExp pcol)
    {
        return new ColumnExp(getCloneClass(), pcol.name);
    }

//    protected ColumnExp getRatingColumn (ColumnExp pcol)
//    {
//        return new ColumnExp(getRatingClass(), pcol.name);
//    }

    protected ColumnExp getTagColumn (ColumnExp pcol)
    {
        return new ColumnExp(getTagRepository().getTagClass(), pcol.name);
    }

    protected SQLExpression getRatingExpression ()
    {
        return new Div(getItemColumn(ItemRecord.RATING_SUM),
                       new FunctionExp("GREATEST", getItemColumn(ItemRecord.RATING_COUNT),
                                       new ValueExp(1.0)));
    }

    protected Key<T> getItemKey (int itemId)
    {
        return Key.newKey(getItemClass(), getItemColumn(ItemRecord.ITEM_ID), itemId);
    }

    protected Key<CloneRecord> getCloneKey (int itemId)
    {
        return Key.newKey(getCloneClass(), getCloneColumn(CloneRecord.ITEM_ID), itemId);
    }

    protected Key<CatalogRecord> getCatalogKey (int catalogId)
    {
        return Key.newKey(getCatalogClass(), getCatalogColumn(CatalogRecord.CATALOG_ID), catalogId);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(getItemClass());
        classes.add(getCloneClass());
        classes.add(getCatalogClass());
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

    /** A predicate that returns true for original item ids. */
    protected static final Predicate<Integer> IS_ORIGINAL_ID = new Predicate<Integer>() {
        public boolean apply (Integer itemId) {
            return itemId > 0;
        }
    };

    /** A predicate that returns true for cloned item ids. */
    protected static final Predicate<Integer> IS_CLONE_ID = new Predicate<Integer>() {
        public boolean apply (Integer itemId) {
            return itemId < 0;
        }
    };

    /** The byte type of our item. */
    protected byte _itemType;

    /** Used to manage our item tags. */
    protected TagRepository _tagRepo;

    /** Used to manage item ratings. */
    protected RatingRepository _ratingRepo;

    // our dependencies
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected MoneyExchange _exchange;

    /** The minimum number of purchases before we'll start attenuating price based on returns. */
    protected static final int MIN_ATTEN_PURCHASES = 5;

    /** The minimum number of ratings required to qualify a rating as "solid" */
    protected static final int MIN_SOLID_RATINGS = 20;

    /** How many catalog records to actually request from the database when using our fancy
     * chunking algorithm. Larger values reduce DB load; excessively high ones will over-fill
     * the cache heap with unrequested result sets. */
    protected static final int FIND_ALL_CHUNK = 100;
}
