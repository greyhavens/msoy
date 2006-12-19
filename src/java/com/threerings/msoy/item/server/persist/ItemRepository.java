//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.samskivert.Log;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.Modifier;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.Query;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.SQLOperator;
import com.samskivert.jdbc.depot.operator.Arithmetic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.expression.FunctionExp;
import com.samskivert.jdbc.depot.expression.LiteralExp;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.IntListUtil;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.TagHistory;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<
        T extends ItemRecord,
        CLT extends CloneRecord<T>,
        CAT extends CatalogRecord<T>,
        TT extends TagRecord<T>,
        THT extends TagHistoryRecord<T>,
        RT extends RatingRecord<T>>
    extends DepotRepository
{
    @Computed @Entity
    public static class RatingAverageRecord {
        public int count;
        public int sum;
    }

    public ItemRepository (ConnectionProvider provider)
    {
        // we'll be using one persistence context per ItemRecord type
        super(new PersistenceContext("itemdb", provider));
    }

    /**
     * Load an item, or a clone.
     */
    public T loadItem (int itemId)
        throws PersistenceException
    {
        // TODO: This will only work for the first two billion clones.
        return itemId > 0 ? loadOriginalItem(itemId) : loadClone(itemId);
    }

    /**
     * Loads an item with the specified identifier. Returns null if no item
     * exists with that identifier.
     */
    public T loadOriginalItem (int itemId)
        throws PersistenceException
    {
        return load(getItemClass(), itemId);
    }

    /**
     * Loads the clone with the given identifier. Returns null if no clone
     * exists with that identifier.
     */
    public T loadClone (int cloneId) throws PersistenceException
    {
        CLT cloneRecord = load(getCloneClass(), cloneId);
        if (cloneRecord == null) {
            return null;
        }

        T clone = loadOriginalItem(cloneRecord.originalItemId);
        if (clone == null) {
            throw new PersistenceException(
                "Clone's original does not exist [cloneId=" + cloneId +
                ", originalItemId=" + cloneRecord.originalItemId + "]");
        }
        clone.parentId = clone.itemId;
        clone.itemId = cloneRecord.itemId;
        clone.ownerId = cloneRecord.ownerId;
        clone.used = cloneRecord.used;
        clone.location = cloneRecord.location;
        return clone;
    }

    /**
     * Loads all original items owned by the specified member.
     */
    public Collection<T> loadOriginalItems (int ownerId)
        throws PersistenceException
    {
        return findAll(getItemClass(), new Key(ItemRecord.OWNER_ID, ownerId),
                       OrderBy.descending(ItemRecord.RATING));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public Collection<T> loadClonedItems (int ownerId)
        throws PersistenceException
    {
        ColumnExp cloneOwner = new ColumnExp(getCloneClass(), CloneRecord.OWNER_ID);
        return findAll(
            getItemClass(),
            new Key(cloneOwner, ownerId),
            new Join(getItemClass(), ItemRecord.ITEM_ID,
                     getCloneClass(), CloneRecord.ORIGINAL_ITEM_ID),
            new FieldOverride(ItemRecord.ITEM_ID, getCloneClass(), CloneRecord.ITEM_ID),
            new FieldOverride(ItemRecord.PARENT_ID, getItemClass(), ItemRecord.ITEM_ID),
            new FieldOverride(ItemRecord.OWNER_ID, cloneOwner),
            new FieldOverride(ItemRecord.LOCATION, getItemClass(), ItemRecord.ITEM_ID),
            new FieldOverride(ItemRecord.USED, getItemClass(), ItemRecord.ITEM_ID));
    }

    /**
     * Loads the specified items. Omits missing items from results.
     */
    public Collection<T> loadItems (int[] itemIds)
        throws PersistenceException
    {
        if (itemIds.length == 0) {
            return Collections.emptyList();
        }
        Comparable[] idArr = IntListUtil.box(itemIds);
        Where inClause = new Where(new In(getItemClass(), ItemRecord.ITEM_ID, idArr));
        Collection<T> items = findAll(
            getItemClass(), inClause,
            new Join(getItemClass(), ItemRecord.ITEM_ID,
                     getCloneClass(), CloneRecord.ORIGINAL_ITEM_ID),
            new FieldOverride(ItemRecord.ITEM_ID, getCloneClass(), CloneRecord.ITEM_ID),
            new FieldOverride(ItemRecord.PARENT_ID, getItemClass(), ItemRecord.ITEM_ID),
            new FieldOverride(ItemRecord.OWNER_ID, getCloneClass(), CloneRecord.OWNER_ID));

        items.addAll(findAll(getItemClass(), inClause));
        return items;
    }

    /**
     * Mark the specified items as being used in the specified way.
     */
    public void markItemUsage (int[] itemIds, byte usageType, int location)
        throws PersistenceException
    {
        Class<T> iclass = getItemClass();
        Class<CLT> cclass = getCloneClass();
        Byte utype = Byte.valueOf(usageType);
        Integer loc = Integer.valueOf(location);

        for (int itemId : itemIds) {
            int result;
            if (itemId > 0) {
                result = updatePartial(iclass, itemId, ItemRecord.USED, utype,
                    ItemRecord.LOCATION, loc);

            } else {
                result = updatePartial(cclass, itemId, ItemRecord.USED, utype,
                    ItemRecord.LOCATION, loc);
            }
            // if the item didn't update, freak out.
            if (0 == result) {
                Log.warning("Unable to find item to mark usage " +
                            "[itemId=" + itemId + ", usageType=" + usageType +
                            ", location=" + location + "].");
            }
        }
    }

    /**
     * Loads all items in the catalog.
     *
     * TODO: This method currently fetches CatalogRecords through a join against ItemRecord,
     *       and then executes a second query against ItemRecord only. This really really has
     *       to be a single join in a sane universe, but it makes significant demands on the
     *       Depot code that we don't know how to handle yet (or possibly some fiddling with
     *       the Item vs Catalog class hierarchies). 
     */
    public Collection<CAT> loadCatalog (byte sortBy, String search, int offset, int rows)
        throws PersistenceException
    {
        SQLExpression sortExp;

        switch(sortBy) {
        case CatalogListing.SORT_BY_LIST_DATE:
            sortExp = new ColumnExp(getCatalogClass(), CatalogRecord.LISTED_DATE);
            break;
        case CatalogListing.SORT_BY_RATING:
            ColumnExp ratingCol = new ColumnExp(getItemClass(), ItemRecord.RATING);
            sortExp = new Div(new FunctionExp("floor", ratingCol), 2);
            break;
        default:
            throw new IllegalArgumentException(
                "Sort method not implemented [sortBy=" + sortBy + "]");
        }

        QueryClause[] clauses = new QueryClause[] {
            new Join(getCatalogClass(), CatalogRecord.ITEM_ID,
                getItemClass(), ItemRecord.ITEM_ID),
            OrderBy.descending(sortExp),
            new Limit(offset, rows)
        };
        
        if (search != null && search.length() > 0) {
            // TODO: We should have a Like() operator in Depot.
            SQLOperator searchExp = new SQLOperator.BinaryOperator(
                new ColumnExp(getItemClass(), ItemRecord.NAME),"%" + search + "%")
            {
                @Override
                protected String operator ()
                {
                    return " like ";
                }
            };
            clauses = ArrayUtil.append(clauses, new Where(searchExp));
        };

        // fetch all the catalog records of interest
        Collection<CAT> records = findAll(getCatalogClass(), clauses);

        if (records.size() == 0) {
            return records;
        }

        // construct an array of item ids we need to load
        Comparable[] idArr = new Integer[records.size()];
        int ii = 0;
        for (CatalogRecord record : records) {
            idArr[ii ++] = record.itemId;
        }

        // load those items and map item ID's to items
        Collection<T> items = findAll(
            getItemClass(), new Where(new In(getItemClass(), ItemRecord.ITEM_ID, idArr)));
        Map<Integer, T> map = new HashMap<Integer, T>();
        for (T iRec : items) {
            map.put(iRec.itemId, iRec);
        }

        // finally populate the catalog records
        for (CatalogRecord<T> record : records) {
            record.item = map.get(record.itemId);
        }
        return records;
    }

    /**
     * Inserts the supplied item into the database. {@link Item#itemId} will be filled in as a
     * result of this call.
     */
    public void insertOriginalItem (T item)
        throws PersistenceException
    {
        if (item.itemId != 0) {
            throw new PersistenceException("Can't insert item with existing key: " + item);
        }
        insert(item);
    }

    /**
     * Updates the supplied item in the database.
     */
    public void updateOriginalItem (T item)
        throws PersistenceException
    {
        update(item);
    }

    /**
     * Perform the low level operations involved with listing an item in the catalog: create a row
     * in the item table for a new, immutable version of the item, then an associated row in the
     * catalog table.
     *
     * TODO: this method modifies the input value, totally unintuitive!
     */
    public CatalogRecord insertListing (ItemRecord listItem, long listingTime)
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
        record.listedDate = new Timestamp(listingTime);
        // and insert it - done!
        insert(record);
        return record;
    }

    /**
     * Removes the listing for the specified item from the catalog, returns true if a listing was
     * found and removed, false otherwise.
     */
    public boolean removeListing  (int itemId)
        throws PersistenceException
    {
        return delete(getCatalogClass(), itemId) > 0;
    }

    /**
     * Insert an item clone into the database with the given owner.  This fills itemId with the
     * next available unique ID and ownerId with the supplied value for the new owner.
     */
    public int insertClone (int itemId, int newOwnerId)
        throws PersistenceException
    {
        CLT record;
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
    public void deleteClone (int cloneId)
        throws PersistenceException
    {
        delete(getCloneClass(), cloneId);
    }

    public RatingRecord<T> getRating (int itemId, int memberId)
        throws PersistenceException
    {
        return load(getRatingClass(), new Key(RatingRecord.ITEM_ID, itemId,
                                              RatingRecord.MEMBER_ID, memberId));
    }

    /**
     * Insert/update a rating row, calculate the new rating and finally update the item's rating.
     */
    public float rateItem (final int itemId, int memberId, byte rating)
        throws PersistenceException
    {
        // first create a new rating record
        RatingRecord<T> record;
        try {
            record = getRatingClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(
                "Failed to create a new item rating record " +
                "[itemId=" + itemId + ", memberId=" + memberId + "]", e);
        }
        // populate and insert it
        record.itemId = itemId;
        record.memberId = memberId;
        record.rating = rating;
        store(record);

        RatingAverageRecord average =
            load(RatingAverageRecord.class,
                 new FieldOverride("count", "count(*)"),
                 new FieldOverride("sum", "sum(" + RatingRecord.RATING + ")"),
                 new FromOverride(getRatingClass()),
                 new Where(new Equals(new ColumnExp(getRatingClass(), RatingRecord.ITEM_ID),
                                      itemId)));
        float newRating = (float) ((average.count == 0) ? 0.0 : average.sum/average.count);
        // and then smack the new value into the item using yummy depot code
        updatePartial(getItemClass(), itemId, new Object[] { ItemRecord.RATING, newRating });
        return newRating;
    }

    /**
     * Join TagNameRecord and TagRecord, group by tag, and count how many items
     * reference each such tag.
     */
    public Collection<TagPopularityRecord> getPopularTags (int rows)
        throws PersistenceException
    {
        return findAll(TagPopularityRecord.class,
                       new FromOverride(getTagClass()),
                       new Join(new ColumnExp(getTagClass(), TagRecord.TAG_ID),
                                TagNameRecord.TAG_ID_C),
                       new FieldOverride(TagPopularityRecord.TAG_ID, TagNameRecord.TAG_ID_C),
                       new FieldOverride(TagPopularityRecord.TAG, TagNameRecord.TAG_C),
                       new FieldOverride(TagPopularityRecord.COUNT, "count(*)"),
                       new GroupBy(TagNameRecord.TAG_ID_C));
    }

    /**
     * Loads all tag records for the given item, translated to tag names.
     */
    public Iterable<TagNameRecord> getTags (int itemId)
        throws PersistenceException
    {
        return findAll(TagNameRecord.class,
                       new Key(TagRecord.ITEM_ID, itemId),
                       new Join(TagNameRecord.TAG_ID_C,
                                new ColumnExp(getTagClass(), TagRecord.TAG_ID)));
    }

    /**
     * Loads all the tag history records for a given item.
     */
    public Iterable<THT> getTagHistoryByItem (int itemId)
        throws PersistenceException
    {
        return findAll(getTagHistoryClass(), new Key(TagHistoryRecord.ITEM_ID, itemId));
    }

    /**
     * Loads all the tag history records for a given member.
     */
    public Iterable<THT> getTagHistoryByMember (int memberId)
        throws PersistenceException
    {
        return findAll(getTagHistoryClass(), new Key(TagHistoryRecord.MEMBER_ID, memberId));
    }

    /**
     * Finds the tag record for a certain tag, or create it.
     */
    public TagNameRecord getTag (String tagName)
        throws PersistenceException
    {
        // load the tag, if it exists
        TagNameRecord record = load(TagNameRecord.class, new Key(TagNameRecord.TAG, tagName));
        if (record == null) {
            // if it doesn't, create it on the fly
            record = new TagNameRecord();
            record.tag = tagName;
            insert(record);
        }
        return record;
    }

    /**
     * Find the tag record for a certain tag id, or create it.
     */
    public TagNameRecord getTag (int tagId)
        throws PersistenceException
    {
        return load(TagNameRecord.class, new Key(TagNameRecord.TAG_ID, tagId));
    }

    /**
     * Add a tag to an item. If the tag already exists, return false and do nothing else. If it did
     * not, create the tag and add a record in the history table.
     */
    public TagHistoryRecord<T> tagItem (int itemId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord<T> tag = load(
            getTagClass(),
            new Key(TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId));
        if (tag != null) {
            return null;
        }

        try {
            tag = getTagClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException("Failed to create a new item tag record " +
                                           "[itemId=" + itemId + ", tagId=" + tagId + "]", e);
        }
        tag.itemId = itemId;
        tag.tagId = tagId;
        insert(tag);

        TagHistoryRecord<T> history;
        try {
            history = getTagHistoryClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException("Failed to create a new item tag history tag record " +
                                           "[itemId=" + itemId + ", tagId=" + tagId + "]", e);
        }
        history.itemId = itemId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_ADDED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Remove a tag from an item. If the tag didn't exist, return false and do nothing else. If it
     * did, remove the tag and add a record in the history table.
     */
    public TagHistoryRecord<T> untagItem (int itemId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord<T> tag = load(
            getTagClass(),
            new Key(TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId));
        if (tag == null) {
            return null;
        }
        delete(tag);

        TagHistoryRecord<T> history;
        try {
            history = getTagHistoryClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(
                "Failed to create a new item tag history tag record " +
                "[itemId=" + itemId + ", tagId=" + tagId + "]", e);
        }
        history.itemId = itemId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_REMOVED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Copy all tags from one item to another. We have to resort to JDBC here, because we want to
     * do the rather non-generic:
     *
     *   INSERT INTO PhotoTagRecord (itemId, tagId)
     *        SELECT 153567, tagId
     *          FROM PhotoTagRecord
     *         WHERE itemId = 89736;
     */
    public int copyTags (final int fromItemId, final int toItemId, int ownerId, long now)
        throws PersistenceException
    {
        final String tagTable = _ctx.getMarshaller(getTagClass()).getTableName();
        int rows = _ctx.invoke(new Modifier(null) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        " INSERT INTO " + tagTable +
                        " (" + TagRecord.ITEM_ID + ", " + TagRecord.TAG_ID + ")" +
                        "      SELECT ?, " + TagRecord.TAG_ID +
                        "        FROM " + tagTable +
                        "       WHERE " + TagRecord.ITEM_ID + " = ?");
                    stmt.setInt(1, toItemId);
                    stmt.setInt(2, fromItemId);
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        // add a single row to history for the copy
        TagHistoryRecord<T> history;
        try {
            history = getTagHistoryClass().newInstance();
        } catch (Exception e) {
            throw new PersistenceException(
                "Failed to create a new item tag history tag record " +
                "[itemId=" + toItemId + "]", e);
        }
        history.itemId = toItemId;
        history.tagId = -1;
        history.memberId = ownerId;
        history.action = TagHistory.ACTION_COPIED;
        history.time = new Timestamp(now);
        insert(history);
        return rows;
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
    protected abstract Class<CLT> getCloneClass ();

    /**
     * Specific item repositories override this method and indicate their item's catalog persistent
     * record class.
     */
    protected abstract Class<CAT> getCatalogClass ();

    /**
     * Specific item repositories override this method and indicate their item's tag persistent
     * record class.
     */
    protected abstract Class<TT> getTagClass ();

    /**
     * Specific item repositories override this method and indicate their item's tag history
     * persistent record class.
     */
    protected abstract Class<THT> getTagHistoryClass ();

    /**
     * Specific item repositories override this method and indicate their item's rating persistent
     * record class.
     */
    protected abstract Class<RT> getRatingClass ();
}
