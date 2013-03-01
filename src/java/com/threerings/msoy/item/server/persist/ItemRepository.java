//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.io.Serializable;
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

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntIntMap;
import com.samskivert.util.QuickSort;
import com.samskivert.util.StringUtil;

import com.samskivert.depot.CacheInvalidator.TraverseWithFilter;
import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DateFuncs;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Funcs;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.MathFuncs;
import com.samskivert.depot.Ops;
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
import com.samskivert.depot.expression.FluentExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Case;
import com.samskivert.depot.operator.FullText;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.room.server.persist.MemoryRepository;
import com.threerings.msoy.server.persist.HotnessConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagRepository;

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
        public FullText.Match fullTextMatch ()
        {
            return _itemFts.match();
        }

        public FullText.Rank fullTextRank ()
        {
            return _itemFts.rank();
        }

        public FullText.Match cloneTextMatch ()
        {
            return _cloneFts.match();
        }

        public FullText.Rank cloneTextRank ()
        {
            return _cloneFts.rank();
        }

        public SQLExpression<?> tagExistsExpression (ColumnExp<?> itemColumn)
        {
            if (_tagIds.size() == 0) {
                return null;
            }
            Where where = new Where(
                Ops.and(getTagColumn(TagRecord.TARGET_ID).eq(itemColumn),
                        getTagColumn(TagRecord.TAG_ID).in(_tagIds)));
            return Ops.exists(new SelectClause(getTagRepository().getTagClass(),
                new ColumnExp<?>[] { getTagColumn(TagRecord.TAG_ID) }, where));
        }

        public SQLExpression<?> madeByExpression ()
        {
            if (_memberIds.size() == 0) {
                return null;
            }
            return getItemColumn(ItemRecord.CREATOR_ID).in(_memberIds);
        }

        protected WordSearch (String search)
        {
            // first split our search up into words
            String[] searchTerms = search.toLowerCase().split("\\W+");
            if (searchTerms.length > 0 && searchTerms[0].length() == 0) {
                searchTerms = ArrayUtil.splice(searchTerms, 0, 1);
            }

            // look up each word as a tag
            _tagIds = Sets.newHashSet();

            if (searchTerms.length > 0) {
                for (TagNameRecord tRec : getTagRepository().getTags(searchTerms)) {
                    _tagIds.add(tRec.tagId);
                }
            }

            _memberIds = Sets.newHashSet();

            // look up the first 100 members whose name matches each search term exactly
            for (String term : searchTerms) {
                _memberIds.addAll(_memberRepo.findMembersByExactDisplayName(term, 100));
            }

            _itemFts = new FullText(getItemClass(), ItemRecord.FTS_ND, search, true);
            _cloneFts = new FullText(getCloneClass(), CloneRecord.FTS_N, search, true);

        }

        protected Set<Integer> _tagIds;
        protected Set<Integer> _memberIds;
        protected FullText _itemFts, _cloneFts;
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

        _mogMarkClass = createMogMarkRecord().getClass();

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

        _ctx.registerMigration(getItemClass(), new SchemaMigration.Retype(
            23 * ItemRecord.BASE_MULTIPLIER, ItemRecord.FURNI_MEDIA_HASH));
    }

    /**
     * Configures this repository with its item type
     */
    public void init (MsoyItemType itemType)
    {
        _itemType = itemType;

        registerMigration(new DataMigration("2013-03 Shop bars removal, type=" + itemType) {
            @Override public void invoke ()
                throws DatabaseException {
                int coinsPerBar = 10000;
                Where barListings = new Where(getCatalogColumn(CatalogRecord.CURRENCY), Currency.BARS);

                updatePartial(getCatalogClass(), barListings, null,
                    getCatalogColumn(CatalogRecord.CURRENCY), Currency.COINS,
                    getCatalogColumn(CatalogRecord.COST),
                        getCatalogColumn(CatalogRecord.COST).times(coinsPerBar));
            }
        });
    }

    /**
     * Returns the item type constant for the type of item handled by this repository.
     */
    public MsoyItemType getItemType ()
    {
        return _itemType;
    }

    /**
     * Accesses the normally protected class of ItemRecord this repository is for.
     */
    public Class<T> exposeItemClass ()
    {
        return getItemClass();
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
     * Loads all the clones made from the given original item id that are also owned by
     * the given member.
     */
    public List<T> loadClones (int originalId, int ownerId)
    {
        return loadClonedItems(new Where(
            getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID), originalId,
            getCloneColumn(CloneRecord.OWNER_ID), ownerId));
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
     * query (if non-null) or the supplied theme (if non-zero).
     */
    public List<T> findItems (int ownerId, String query, int themeId)
    {
        if (ownerId == 0) {
            throw new IllegalArgumentException("Refusing to enumerate inventory of ownerId=0");
        }
        WordSearch queryContext = buildWordSearch(query);
        List<SQLExpression<?>> matches = Lists.newArrayList();

        // original items only match on the text and creator (they cannot be tagged)
        if (queryContext != null) {
            addTextMatchClause(matches, queryContext);
            addCreatorMatchClause(matches, queryContext);
        }

        QueryClause[] clauses = new QueryClause[0];
        SQLExpression<?>[] whereBits = new SQLExpression<?>[0];

        if (themeId != 0) {
            List<? extends MogMarkRecord> originalRecs = findAll(getMogMarkClass(),
                new Where(getMogMarkColumn(MogMarkRecord.GROUP_ID).eq(themeId)));
            whereBits = ArrayUtil.append(whereBits, getItemColumn(ItemRecord.ITEM_ID).in(
                Lists.transform(originalRecs, MogMarkRecord.TO_ITEM_ID)));
        }

        SQLExpression<?>[] originalBits = ArrayUtil.append(
            whereBits, getItemColumn(ItemRecord.OWNER_ID).eq(ownerId));
        if (matches.size() > 0) {
            originalBits = ArrayUtil.append(originalBits, makeSearchClause(matches));
        }

        // locate all matching original items
        List<T> results = loadAllWithWhere(getItemClass(), originalBits, clauses);

        if (queryContext != null) {
            // now add the tag match as cloned items can match tags
            addTagMatchClause(matches, getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID), queryContext);
            // and add renamed clones to the full text search
            matches.add(queryContext.cloneTextMatch());
        }

        SQLExpression<?>[] cloneBits = ArrayUtil.append(
            whereBits, getCloneColumn(CloneRecord.OWNER_ID).eq(ownerId));
        if (matches.size() > 0) {
            cloneBits = ArrayUtil.append(cloneBits, makeSearchClause(matches));
        }
        results.addAll(resolveClones(loadAllWithWhere(getCloneClass(), cloneBits,
            ArrayUtil.insert(clauses, new Join(
                getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID),
                getItemColumn(ItemRecord.ITEM_ID)), 0))));

        return results;
    }

    /**
     * Loads up to maxCount items from a user's inventory that were the most recently touched.
     * @param maxCount
     */
    public List<T> loadRecentlyTouched (int ownerId, int themeId, int maxCount)
    {
        SQLExpression<?>[] baseWhere = new SQLExpression<?>[0];
        QueryClause[] cloneClauses = new QueryClause[] {
            new Limit(0, maxCount),
            OrderBy.descending(getCloneColumn(CloneRecord.LAST_TOUCHED))
        };
        if (themeId != 0) {
            baseWhere = ArrayUtil.append(baseWhere,
                getMogMarkColumn(MogMarkRecord.GROUP_ID).eq(themeId));
            cloneClauses = ArrayUtil.append(cloneClauses,
                new Join(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID),
                         MogMarkRecord.ITEM_ID.as(getMogMarkClass())));
        }

        List<CloneRecord> cloneRecords = loadAllWithWhere(
            getCloneClass(),
            ArrayUtil.append(baseWhere, getCloneColumn(CloneRecord.OWNER_ID).eq(ownerId)),
            cloneClauses);

        List<T> items = resolveClones(cloneRecords);

        QueryClause[] originalClauses = new QueryClause[] {
            new Limit(0, maxCount),
            OrderBy.descending(getItemColumn(ItemRecord.LAST_TOUCHED))
        };
        if (themeId != 0) {
            originalClauses = ArrayUtil.append(originalClauses,
                new Join(getItemColumn(ItemRecord.ITEM_ID),
                         getMogMarkColumn(MogMarkRecord.ITEM_ID)));
        }

        items.addAll(loadAllWithWhere(
            getItemClass(),
            ArrayUtil.append(baseWhere, getItemColumn(ItemRecord.OWNER_ID).eq(ownerId)),
            originalClauses));

        // now, sort by their lastTouched time
        QuickSort.sort(items, new Comparator<T>() {
            public int compare (T o1, T o2) {
                return o2.lastTouched.compareTo(o1.lastTouched);
            }
        });

        // remove any items beyond maxCount
        CollectionUtil.limit(items, maxCount);
        return items;
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
        return loadCloneRecords(Collections.singleton(itemId));
    }

    /**
     * Loads all the raw clone records associated with any one of the given original item ids.
     * This is potentially an enormously large dataset.
     */
    public List<CloneRecord> loadCloneRecords (Collection<Integer> itemIds)
    {
        return findAll(getCloneClass(),
            new Where(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID).in(itemIds)));
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
                     new Where(getItemColumn(ItemRecord.ITEM_ID).in(origIds)))) {
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
                     new Where(getCloneColumn(CloneRecord.ITEM_ID).in(cloneIds)))) {
                ownerIds.put(oidrec.itemId, oidrec.ownerId);
            }
        }
        return ownerIds;
    }

    /**
     * Loads all owners of a given original item. Please note that this can be a large amount
     * of data for popular items. This method is intended for migration purposes.
     */
    public Set<Integer> loadOwnerIds (int originalItemId)
    {
        List<OwnerIdRecord> ownerRecords = findAll(
            OwnerIdRecord.class, new FromOverride(getCloneClass()),
            new FieldDefinition("itemId", getCloneColumn(CloneRecord.ITEM_ID)),
            new FieldDefinition("ownerId", getCloneColumn(CloneRecord.OWNER_ID)),
            new Where(getCloneColumn(CloneRecord.ITEM_ID).eq(originalItemId)));

        Set<Integer> result = Sets.newHashSet();





        for (OwnerIdRecord oidrec : ownerRecords) {
            result.add(oidrec.ownerId);
        }
        return result;
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
        Float minRating, int themeId, int gameId, int offset, int rows, float exchangeRate)
    {
        LinkedList<QueryClause> clauses = Lists.newLinkedList();
        clauses.add(new Join(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID),
                             getItemColumn(ItemRecord.ITEM_ID)));

        // sort out the primary and secondary order by clauses
        List<SQLExpression<?>> obExprs = Lists.newArrayList();
        List<OrderBy.Order> obOrders = Lists.newArrayList();
        // and keep track of additional constraints on the query
        List<SQLExpression<?>> whereBits = Lists.newArrayList();
        switch(sortBy) {
        case CatalogQuery.SORT_BY_LIST_DATE:
            addOrderByListDate(obExprs, obOrders);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_RATING:
            addOrderByRating(obExprs, obOrders);
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.ASC, exchangeRate);
            break;
        case CatalogQuery.SORT_BY_PRICE_ASC:
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.ASC, exchangeRate);
            addOrderByRating(obExprs, obOrders);
            break;
        case CatalogQuery.SORT_BY_PRICE_DESC:
            addOrderByPrice(obExprs, obOrders, OrderBy.Order.DESC, exchangeRate);
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
                obExprs.toArray(new SQLExpression<?>[obExprs.size()]),
                obOrders.toArray(new OrderBy.Order[obOrders.size()])).
                    thenAscending(getCatalogColumn(CatalogRecord.CATALOG_ID)));
        }

        // see if there's any where bits to turn into an actual where clause
        boolean significantlyConstrained = addSearchClause(
            clauses, whereBits, mature, context, tag, creator, minRating, themeId, gameId);

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
     * Load catalog records for a list of original (listed) item ID's.
     */
    public List<CatalogRecord> loadCatalogByListedItems (
        Collection<Integer> listedItemIds, boolean loadListedItems)
    {
        List<CatalogRecord> cRecs = findAll(getCatalogClass(),
            new Where(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID).in(listedItemIds)));
        if (loadListedItems) {
            // find out the id's of all the listed items
            Set<Integer> itemIds = Sets.newHashSet();





            for (CatalogRecord cRec : cRecs) {
                itemIds.add(cRec.listedItemId);
            }
            // load them and associate each one with its id
            Map<Integer, T> itemMap = Maps.newHashMap();
            for (T iRec : loadAll(getItemClass(), itemIds)) {
                itemMap.put(iRec.itemId, iRec);
            }
            // finally populate the catalog records
            for (CatalogRecord cRec : cRecs) {
                cRec.item = itemMap.get(cRec.listedItemId);
            }
        }
        return cRecs;
    }

    /**
     * Load markup records for items stamped with a given theme; order them chronologically
     * by stamping. This could be handled through loadCatalog(), but that thing is turning into
     * a monster as is.
     */
    public List<? extends MogMarkRecord> loadThemedCatalog (int themeId, int rows)
    {
        return findAll(getMogMarkClass(),
            new Join(getMogMarkColumn(MogMarkRecord.ITEM_ID),
                     getCatalogColumn(CatalogRecord.LISTED_ITEM_ID)),
            new Where(getMogMarkColumn(MogMarkRecord.GROUP_ID), themeId),
            OrderBy.descending(getMogMarkColumn(MogMarkRecord.LAST_STAMPED)),
            new Limit(0, rows));
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
            new Where(getCloneColumn(CloneRecord.OWNER_ID).in(memberIds)));
        deleteAll(getCloneClass(), KeySet.newKeySet(getCloneClass(), clones));
        deletedIds.addAll(Lists.transform(clones, Key.<CloneRecord>toInt()));

        // delete all original items that are not listed in the catalog; we could delete the
        // catalog originals but that would make repricing or otherwise fiddling with the catalog
        // listings on the part of the support staff more of a PITA, so we'll leave 'em for now
        List<Key<T>> origs = findAllKeys(
            getItemClass(), false,
            new Where(Ops.and(getItemColumn(ItemRecord.OWNER_ID).in(memberIds),
                              Ops.not(getItemColumn(ItemRecord.CATALOG_ID).eq(0)))));
        deleteAll(getItemClass(), KeySet.newKeySet(getItemClass(), origs));
        deletedIds.addAll(Lists.transform(origs, Key.<T>toInt()));

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
     * Figure out whether a given item is acceptable in a given theme.
     */
    public boolean isThemeStamped (int themeGroupId, int itemId)
    {
        if (itemId < 0) {
            // for clones we have to do a join
            return null != load(getMogMarkClass(),
                new Join(getMogMarkColumn(MogMarkRecord.ITEM_ID),
                         getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID)),
                new Where(getCloneColumn(CloneRecord.ITEM_ID), itemId,
                          getMogMarkColumn(MogMarkRecord.GROUP_ID), themeGroupId));

        }
        return null != load(getMogMarkClass(), new Where(
            getMogMarkColumn(MogMarkRecord.GROUP_ID), themeGroupId,
            getMogMarkColumn(MogMarkRecord.ITEM_ID), itemId));
    }

    /**
     * Return all the theme stamp records for a given item.
     */
    public List<? extends MogMarkRecord> loadItemStamps (int itemId)
    {
        return findAll(getMogMarkClass(),
            new Where(getMogMarkColumn(MogMarkRecord.ITEM_ID), itemId));
    }

    /**
     * Stamp the given item with the given theme, noting also who did it. This function
     * returns true if the item was previously unstamped by this theme.
     */
    public boolean stampItem (int itemId, int groupId, int stamperId)
    {
        return store(createMogMarkRecord(itemId, groupId, stamperId));
    }

    /**
     * Removes the given item's stamp for the given theme. This function returns true if the
     * stamp existed.
     */
    public boolean unstampItem (int itemId, int groupId)
    {
        return 1 == delete(createMogMarkRecord(itemId, groupId, -1));
    }

    /**
     * Update either the 'purchases' or the 'returns' field of a catalog listing, and figure out if
     * it's time to reprice it.
     * @return the newly assigned cost or the original cost if it did not change
     */
    public int nudgeListing (CatalogRecord record, boolean purchased)
    {
        int newCost = record.cost;
        Map<ColumnExp<?>, SQLExpression<?>> updates = Maps.newHashMap();
        if (purchased) {
            updates.put(CatalogRecord.PURCHASES,
                        getCatalogColumn(CatalogRecord.PURCHASES).plus(1));

            int purchases = record.purchases + 1; // for below calculations
            switch (record.pricing) {
            case CatalogListing.PRICING_LIMITED_EDITION:
                if (purchases >= record.salesTarget) {
                    updates.put(CatalogRecord.PRICING, Exps.value(CatalogListing.PRICING_HIDDEN));
                }
                break;

            case CatalogListing.PRICING_ESCALATE:
                if (purchases == record.salesTarget) {
                    newCost = CatalogListing.escalatePrice(record.cost);
                    updates.put(CatalogRecord.COST, Exps.value(newCost));
                }
                break;
            }

        } else {
            updates.put(CatalogRecord.RETURNS, getCatalogColumn(CatalogRecord.RETURNS).plus(1));
        }

        // finally update the columns we actually modified
        updatePartial(getCatalogKey(record.catalogId), updates);
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
                              Currency currency, int cost, long listingTime, int basisCatalogId,
                              int brandId)
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
        record.brandId = brandId;
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
                               Currency currency, int cost, int brandId, long updateTime)
    {
        updatePartial(getCatalogKey(catalogId),
                      // TODO?: CatalogRecord.LISTED_DATE, new Timestamp(updateTime),
                      CatalogRecord.PRICING, pricing,
                      CatalogRecord.SALES_TARGET, salesTarget,
                      CatalogRecord.CURRENCY, currency,
                      CatalogRecord.COST, cost,
                      CatalogRecord.BRAND_ID, brandId);
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
        ColumnExp<Integer> cost = getCatalogColumn(CatalogRecord.COST);
        return updatePartial(getCatalogClass(), where, invalidator, cost, cost.plus(change));
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
            // TODO RELISTING
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
    public T insertClone (T parent, int newOwnerId, Currency currency, int amountPaid)
    {
        CloneRecord record;
        try {
            record = getCloneClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.initialize(parent, newOwnerId, currency, amountPaid);
        insert(record);

        T newClone = getItemClass().cast(parent.clone());
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
            _itemFlagRepo.removeItemFlags(_itemType, itemId);
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
        SQLExpression<?> add = getCatalogColumn(CatalogRecord.FAVORITE_COUNT).plus(increment);
        if (updatePartial(getCatalogKey(catalogId), CatalogRecord.FAVORITE_COUNT, add) == 0) {
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
        SQLExpression<?> derived = getCatalogColumn(CatalogRecord.BASIS_ID).eq(catalogId);
        SQLExpression<?> visible = Ops.not(
            getCatalogColumn(CatalogRecord.PRICING).eq(CatalogListing.PRICING_HIDDEN));
        clauses.add(new Where(Ops.and(derived, visible)));
        if (maximum > 0) {
            clauses.add(new Limit(0, maximum));
        }
        return Lists.transform(findAllKeys(getCatalogClass(), false, clauses),
            Key.<CatalogRecord>toInt());
    }

    /**
     * A highly specialized method that returns true if one of the supplied itemIds (which may
     * correspond to clones or originals) corresponds to (is a clone of or master item for) the
     * given catalog listing.
     */
    public boolean containsListedItem (Collection<Integer> itemIds, int catalogId)
    {
        Set<Integer> originalIds = getOriginalIds(itemIds);
        if (!originalIds.isEmpty() &&
                load(CountRecord.class,
                    new FromOverride(getItemClass()),
                    new Where(Ops.and(
                        getItemColumn(ItemRecord.ITEM_ID).in(originalIds),
                        getItemColumn(ItemRecord.CATALOG_ID).eq(catalogId)))).count > 0) {
            return true;
        }

        Set<Integer> cloneIds = getCloneIds(itemIds);
        if (!cloneIds.isEmpty() &&
                load(CountRecord.class,
                    new FromOverride(getCloneClass()),
                    new Join(getCloneColumn(CloneRecord.ORIGINAL_ITEM_ID),
                        getItemColumn(ItemRecord.ITEM_ID)),
                        new Where(Ops.and(
                            getCloneColumn(CloneRecord.ITEM_ID).in(cloneIds),
                            getItemColumn(ItemRecord.CATALOG_ID).eq(catalogId)))).count > 0) {
            return true;
        }
        return false;
    }

    /**
     * Returns the column used to check for duplicate uploads.
     */
    protected ColumnExp<byte[]> getPrimaryMediaColumn ()
    {
        return getItemColumn(ItemRecord.FURNI_MEDIA_HASH);
    }

    /**
     * Returns true if this item's content already exists from a different player.
     */
    public ItemRecord loadConflictingItem (int creatorId, Item item)
    {
        byte[] hash = HashMediaDesc.unmakeHash(item.getPrimaryMedia());
        return load(getItemClass(),
            new Where(Ops.and(
                getPrimaryMediaColumn().eq(Exps.value(hash)),
                getItemColumn(ItemRecord.CREATOR_ID).notEq(creatorId))));
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
        ColumnExp<Integer> count = getCatalogColumn(CatalogRecord.DERIVATION_COUNT);
        updatePartial(getCatalogKey(catalogId), count, count.plus(add ? 1 : -1));
    }

    /**
     * Conveniently concatenates an array of expressions into a Where statements and appends
     * that to the supplied list of clauses, finally executing findAll() on the whole shebang.
     */
    protected <U extends PersistentRecord> List<U> loadAllWithWhere (
        Class<U> itemClass, SQLExpression<?>[] whereBits, QueryClause[] clauses)
    {
        return findAll(itemClass,
            ArrayUtil.append(clauses, new Where(Ops.and(whereBits))));
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
            return Lists.newArrayList();
        }

        // create a set of the corresponding original ids
        Set<Integer> origIds = Sets.newHashSet();
        for (CloneRecord clone : clones) {
            origIds.add(clone.originalItemId);
        }

        // find all the originals and insert them into a map
        List<T> originals = loadAll(getItemClass(), origIds);
        Map<Integer, T> records = Maps.newHashMap();
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
    protected void addTextMatchClause (List<SQLExpression<?>> matches, WordSearch search)
    {
        // search item name and description
        matches.add(search.fullTextMatch());
    }

    /**
     * Adds a match on the name of the creator to the supplied list.
     */
    protected void addCreatorMatchClause (List<SQLExpression<?>> matches, WordSearch search)
    {
        SQLExpression<?> op = search.madeByExpression();
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
        return StringUtil.isBlank(query) ? null : new WordSearch(query);
    }

    /**
     * Searches for any tags that match the search string and matches all catalog master items that
     * are tagged with those tags.
     */
    protected void addTagMatchClause (
        List<SQLExpression<?>> matches, ColumnExp<?> itemColumn, WordSearch search)
    {
        // build a query to check tags if one or more tags exists
        SQLExpression<?> op = search.tagExistsExpression(itemColumn);
        if (op != null) {
            matches.add(op);
        }
    }

    /**
     * Composes the supplied list of search match clauses into a single operator.
     */
    protected SQLExpression<?> makeSearchClause (List<SQLExpression<?>> matches)
    {
        switch (matches.size()) {
        case 0:
            return Exps.value(true);

        case 1:
            return matches.get(0);

        default:
            return Ops.or(matches);
        }
    }

    /**
     * Builds a search clause that matches item text, creator name and tags (against listed catalog
     * items).
     */
    protected SQLExpression<?> buildSearchClause (WordSearch queryContext)
    {
        List<SQLExpression<?>> matches = Lists.newArrayList();

        addTextMatchClause(matches, queryContext);
        addCreatorMatchClause(matches, queryContext);
        addTagMatchClause(matches, getCatalogColumn(CatalogRecord.LISTED_ITEM_ID), queryContext);

        return makeSearchClause(matches);
    }

    /**
     * Helper function for {@link #loadCatalog}. Returns true if sufficient clauses were added
     * that we can heuristically claim that the query will not match enormous numbers of rows.
     */
    protected boolean addSearchClause (
        List<QueryClause> clauses, List<SQLExpression<?>> whereBits, boolean mature,
        WordSearch queryContext, int tag, int creator, Float minRating, int themeId, int gameId)
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
            whereBits.add(getTagColumn(TagRecord.TAG_ID).eq(tag));
            significantlyConstrained = true;
        }

        if (creator > 0) {
            whereBits.add(getItemColumn(ItemRecord.CREATOR_ID).eq(creator));
            significantlyConstrained = true;
        }

        if (!mature) {
            // add a check to make sure ItemRecord.FLAG_MATURE is not set on any returned items
            whereBits.add(getItemColumn(ItemRecord.MATURE).eq(false));
        }

        if (minRating != null) {
            whereBits.add(getRatingExpression().greaterEq(minRating));
        }

        if (themeId != 0) {
            clauses.add(new Join(
                getItemColumn(ItemRecord.ITEM_ID), getMogMarkColumn(MogMarkRecord.ITEM_ID)));
            whereBits.add(getMogMarkColumn(MogMarkRecord.GROUP_ID).eq(themeId));
            significantlyConstrained = true;
        }

        if (gameId != 0 && GameItemRecord.class.isAssignableFrom(getItemClass())) {
            whereBits.add(getItemColumn(GameItemRecord.GAME_ID).eq(gameId));
            significantlyConstrained = true;
        }

        whereBits.add(Ops.not(getCatalogColumn(CatalogRecord.PRICING).eq(
                                  CatalogListing.PRICING_HIDDEN)));

        clauses.add(new Where(Ops.and(whereBits)));
        return significantlyConstrained;
    }

    protected void addOrderByListDate (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getCatalogColumn(CatalogRecord.LISTED_DATE));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByRating (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getRatingExpression());
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByPrice (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders,
                                    OrderBy.Order order, float exchangeRate)
    {
        // Multiply bar prices by the current exchange rate.
        //     adjustedCost = cost * Math.max(1, currencyByteVal * exchangeRate)
        //
        // This depends on two things:
        // - We know that Currency.COINS=0, Currency.BARS=1
        // - if the exchange rate was less than 1, this would value coins and bars equally
        //   instead of making bars worth less... that shouldn't happen though.
        exprs.add(getCatalogColumn(CatalogRecord.COST).times(Funcs.greatest(
            Exps.value(1), getCatalogColumn(CatalogRecord.CURRENCY).times(exchangeRate))));
        orders.add(order);
    }

    protected void addOrderByPurchases (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders)
    {
        // TODO: someday make an indexed column that represents (purchases-returns)
        exprs.add(getCatalogColumn(CatalogRecord.PURCHASES));
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByNewAndHot (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders)
    {
        exprs.add(getRatingExpression().plus(
            DateFuncs.epoch(getCatalogColumn(CatalogRecord.LISTED_DATE)).
                div(HotnessConfig.DROPOFF_SECONDS)));
        orders.add(OrderBy.Order.DESC);
    }

    // Construct a relevance ordering for item searches
    protected void addOrderByRelevance (
        List<SQLExpression<?>> exprs, List<OrderBy.Order> orders, WordSearch context)
    {
        // The relevance of a catalog entry is a product of several factors, each chosen to have a
        // tunable impact. The actual value is not important, only the relative sizes.
        List<SQLExpression<Double>> ops = Lists.newArrayList();
        // The base value is just the Full Text Search rank value, the scale of which is entirely
        // unknown. We give it a tiny linear shift so that the creator and tag factors below have
        // something non-zero to work with when there is no full text hit at all
        ops.add(context.fullTextRank().plus(0.1));
        // adjust the FTS rank by (rating + 5), which means a 5-star item is rated
        // (approximately) twice as high rated as a 1-star item
        ops.add(getRatingExpression().plus(1.0));
        // then boost by (log10(purchases+1) + 3), thus an item that's sold 1,000 copies is rated
        // twice as high as something that's sold 1 copy
        ops.add(MathFuncs.log10(getCatalogColumn(CatalogRecord.PURCHASES).plus(1.0)).plus(3.0));

        SQLExpression<?> tagExistsExp =
            context.tagExistsExpression(getCatalogColumn(CatalogRecord.LISTED_ITEM_ID));
        if (tagExistsExp != null) {
            // if there is a tag match, immediately boost relevance by 25%
            ops.add(new Case<Double>(tagExistsExp, Exps.value(1.25), Exps.value(1.0)));
        }

        SQLExpression<?> madeByExp = context.madeByExpression();
        if (madeByExp != null) {
            // if the item was made by a creator who matches the description, also boost by 50%
            ops.add(new Case<Double>(madeByExp, Exps.value(1.5), Exps.value(1.0)));
        }

        exprs.add(Ops.mul(ops));
        orders.add(OrderBy.Order.DESC);

        exprs.add(getRatingExpression());
        orders.add(OrderBy.Order.DESC);
    }

    protected void addOrderByFavorites (List<SQLExpression<?>> exprs, List<OrderBy.Order> orders)
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

    protected <T> ColumnExp<T> getItemColumn (ColumnExp<T> pcol)
    {
        return pcol.as(getItemClass());
    }

    protected <T> ColumnExp<T> getCatalogColumn (ColumnExp<T> pcol)
    {
        return pcol.as(getCatalogClass());
    }

    protected <T> ColumnExp<T> getCloneColumn (ColumnExp<T> pcol)
    {
        return pcol.as(getCloneClass());
    }

//    protected <T> ColumnExp<T> getRatingColumn (ColumnExp<T> pcol)
//    {
//        return pcol.as(getRatingClass());
//    }

    protected <T> ColumnExp<T> getMogMarkColumn (ColumnExp<T> pcol)
    {
        return pcol.as(getMogMarkClass());
    }

    protected <T> ColumnExp<T> getTagColumn (ColumnExp<T> pcol)
    {
        return pcol.as(getTagRepository().getTagClass());
    }

    protected FluentExp<? extends Number> getRatingExpression ()
    {
        return getItemColumn(ItemRecord.RATING_SUM).div(
            Funcs.greatest(getItemColumn(ItemRecord.RATING_COUNT), Exps.value(1.0)));
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
        classes.add(getMogMarkClass());
    }

    /**
     * Create and populate an appropriate {@link MogMarkRecord} subclass.
     */
    protected MogMarkRecord createMogMarkRecord (int itemId, int groupId, int stamperId)
    {
        MogMarkRecord rec = createMogMarkRecord();
        rec.itemId = itemId;
        rec.groupId = groupId;
        rec.stamperId = stamperId;
        rec.lastStamped = new Timestamp(System.currentTimeMillis());
        return rec;
    }

    protected Class<? extends MogMarkRecord> getMogMarkClass ()
    {
        return _mogMarkClass;
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
     * Specific item repositories override this method and indicate their item's mog mark persistent
     * record class.
     */
    protected abstract MogMarkRecord createMogMarkRecord ();

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
    protected MsoyItemType _itemType;

    /** This item type's concrete MogMarkRecord class. */
    protected Class<? extends MogMarkRecord> _mogMarkClass;

    /** Used to manage our item tags. */
    protected TagRepository _tagRepo;

    /** Used to manage item ratings. */
    protected RatingRepository _ratingRepo;

    // our dependencies
    @Inject protected ItemFlagRepository _itemFlagRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;

    /** The minimum number of purchases before we'll start attenuating price based on returns. */
    protected static final int MIN_ATTEN_PURCHASES = 5;

    /** The minimum number of ratings required to qualify a rating as "solid" */
    protected static final int MIN_SOLID_RATINGS = 20;

    /** How many catalog records to actually request from the database when using our fancy
     * chunking algorithm. Larger values reduce DB load; excessively high ones will over-fill
     * the cache heap with unrequested result sets. */
    protected static final int FIND_ALL_CHUNK = 100;
}
