//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.samskivert.Log;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.util.Tuple;
import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.Modifier;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.Query;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.QueryClause;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.msoy.item.web.TagHistory;

/**
 * Manages a repository of digital items of a particular type.
 */
public abstract class ItemRepository<T extends ItemRecord>
    extends DepotRepository
{
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
        CloneRecord<?> cloneRecord = load(getCloneClass(), cloneId);
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
                       new OrderBy(new ColumnExp(ItemRecord.RATING)));
    }

    /**
     * Loads all cloned items owned by the specified member.
     */
    public Collection<T> loadClonedItems (int ownerId)
        throws PersistenceException
    {
        ColumnExp cloneOwner =
            new ColumnExp(getCloneClass(), CloneRecord.OWNER_ID);
        return findAll(
            getItemClass(),
            new Key(cloneOwner, ownerId),
            new Join(
                getItemClass(),
                ItemRecord.ITEM_ID,
                getCloneClass(),
                CloneRecord.ORIGINAL_ITEM_ID),
            new FieldOverride(
                ItemRecord.ITEM_ID,
                new ColumnExp(getCloneClass(), CloneRecord.ITEM_ID)),
            new FieldOverride(
                ItemRecord.PARENT_ID,
                new ColumnExp(getItemClass(), ItemRecord.ITEM_ID)),
            new FieldOverride(ItemRecord.OWNER_ID, cloneOwner));
    }

    /**
     * Loads the specified items. Omits missing items from results.
     */
    public ArrayList<T> loadItems (int[] itemIds)
        throws PersistenceException
    {
        // TODO: Support added to Depot to load a bunch of things at once?
        ArrayList<T> list = new ArrayList<T>();
        for (int id : itemIds) {
            T result = loadItem(id);
            if (result != null) {
                list.add(result);
            }
        }

        return list;
    }

    /**
     * Mark the specified items as being used in the specified way.
     */
    public void markItemUsage (int[] itemIds, byte usageType, int location)
        throws PersistenceException
    {
        Class<T> iclass = getItemClass();
        Class<? extends CloneRecord<T>> cclass = getCloneClass();
        Byte utype = Byte.valueOf(usageType);
        Integer loc = Integer.valueOf(location);

        for (int itemId : itemIds) {
            if (0 == updatePartial(iclass, itemId,
                    ItemRecord.USED, utype, ItemRecord.LOCATION, loc)) {
                // if the item didn't update, try the clone
                if (0 == updatePartial(cclass, itemId,
                        ItemRecord.USED, utype, ItemRecord.LOCATION, loc)) {
                    Log.warning("Unable to find item to mark usage " +
                        "[itemId=" + itemId + ", usageType=" + usageType +
                        ", location=" + location + "].");
                }
            }
        }
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
            record.item = loadOriginalItem(record.itemId);
        }
        return records;
    }


    /**
     * Inserts the supplied item into the database. {@link Item#itemId} will be
     * filled in as a result of this call.
     */
    public void insertOriginalItem (T item)
        throws PersistenceException
    {
        if (item.itemId != 0) {
            throw new PersistenceException(
                "Can't insert item with existing key " + item);
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
    public void deleteClone (int cloneId)
            throws PersistenceException
    {
        delete(getCloneClass(), cloneId);
    }

    public RatingRecord<T> getRating (int itemId, int memberId)
            throws PersistenceException
    {
        return load(getRatingClass(),
                    new Key(RatingRecord.ITEM_ID, itemId,
                            RatingRecord.MEMBER_ID, memberId));

    }

    /** Insert/update a rating row, calculate the new rating and finally
     *  update the item's rating. */
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

        // then calculate the new average rating for this item from scratch
        final String ratingTable =
            _ctx.getMarshaller(getRatingClass()).getTableName();
        float newRating = _ctx.invoke(new Query<Float>(_ctx, getRatingClass(), (QueryClause) null) {
            public Float invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        " SELECT COUNT(*), SUM(" + RatingRecord.RATING + ")" +
                        "   FROM " + ratingTable +
                        "  WHERE " + RatingRecord.ITEM_ID + " = ?");
                    stmt.setInt(1, itemId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) {
                        throw new SQLException(
                            "Failed to calculate new rating for item [itemId=" +
                            itemId + "]");
                    }
                    int count = rs.getInt(1);
                    float sum = rs.getInt(2);
                    return (float) ((count == 0) ? 0.0 : sum/count);
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        // and then smack the new value into the item using yummy depot code
        updatePartial(getItemClass(), itemId,
                      new Object[] { ItemRecord.RATING, newRating });
        return newRating;
    }

    /**
     * Join TagNameRecord and TagRecord, group by tag, and count how many items
     * reference each such tag. We should be able to do this in Depot shortly. 
     */
    public Iterable<Tuple<TagNameRecord, Integer>> getPopularTags (int rows)
        throws PersistenceException
    {
        final String tnTable =
            _ctx.getMarshaller(TagNameRecord.class).getTableName(); 
        final String tTable =
            _ctx.getMarshaller(getTagClass()).getTableName();

        final String query =
            "  select t.tagId, tn.tag, count(*) " +
            "    from " + tTable + " t, " +
            "         " + tnTable + " tn " +
            "    where t.tagId = tn.tagId " +
            " group by t.tagId " +
            "    limit " + rows;

        return _ctx.invoke(
            new Query<List<Tuple<TagNameRecord, Integer>>>(_ctx, null, (QueryClause) null) {
                public List<Tuple<TagNameRecord, Integer>> invoke (
                    Connection conn) throws SQLException {
                    PreparedStatement stmt = conn.prepareStatement(query);
                    try {
                        ArrayList<Tuple<TagNameRecord, Integer>> results =
                            new ArrayList<Tuple<TagNameRecord, Integer>>();
                        ResultSet rs = stmt.executeQuery(query);
                        while (rs.next()) {
                            TagNameRecord record = new TagNameRecord();
                            record.tagId = rs.getInt(1);
                            record.tag = rs.getString(2);
                            int count = rs.getInt(3);
                            results.add(
                                new Tuple<TagNameRecord, Integer>(record, count));
                        }
                        return results;

                    } finally {
                        stmt.close();
                    }
                }
            });
    }
    
    /** Load all tag records for the given item, translated to tag names. */
    public Iterable<TagNameRecord> getTags (int itemId)
            throws PersistenceException
    {
        return findAll(TagNameRecord.class,
                       new Key(TagRecord.ITEM_ID, itemId),
                       new Join(
                           TagNameRecord.class,
                           TagNameRecord.TAG_ID,
                           getTagClass(),
                           TagRecord.TAG_ID));
    }

    /** Load all the tag history records for a given item. */
    public Iterable<? extends TagHistoryRecord<T>> getTagHistoryByItem (
        int itemId)
            throws PersistenceException
    {
        return findAll(getTagHistoryClass(),
                       new Key(TagHistoryRecord.ITEM_ID, itemId));
    }

    /** Load all the tag history records for a given member. */
    public Iterable<? extends TagHistoryRecord<T>> getTagHistoryByMember (
        int memberId)
            throws PersistenceException
    {
        return findAll(getTagHistoryClass(),
                       new Key(TagHistoryRecord.MEMBER_ID, memberId));
    }

    /** Find the tag record for a certain tag, or create it. */
    public TagNameRecord getTag (String tagName)
            throws PersistenceException
    {
        // load the tag, if it exists
        TagNameRecord record = load(TagNameRecord.class,
                                    new Key(TagNameRecord.TAG, tagName));
        if (record == null) {
            // if it doesn't, create it on the fly
            record = new TagNameRecord();
            record.tag = tagName;
            insert(record);
        }
        return record;
    }

    /** Find the tag record for a certain tag id, or create it. */
    public TagNameRecord getTag (int tagId)
            throws PersistenceException
    {
        return load(TagNameRecord.class, new Key(TagNameRecord.TAG_ID, tagId));
    }


    /**
     * Add a tag to an item. If the tag already exists, return false and do
     * nothing else. If it did not, create the tag and add a record in the
     * history table.
     */
    public TagHistoryRecord<T> tagItem (
        int itemId, int tagId, int taggerId, long now)
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
            throw new PersistenceException(
                "Failed to create a new item tag record " +
                "[itemId=" + itemId + ", tagId=" + tagId + "]", e);
        }
        tag.itemId = itemId;
        tag.tagId = tagId;
        insert(tag);

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
        history.action = TagHistory.ACTION_ADDED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Remove a tag from an item. If the tag didn't exist, return false and
     * do nothing else. If it did, remove the tag and add a record in the
     * history table.
     */
    public TagHistoryRecord<T> untagItem (
        int itemId, int tagId, int taggerId, long now)
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
     * Copy all tags from one item to another. We have to resort to JDBC
     * here, because we want to do the rather non-generic:
     *
     *   INSERT INTO PhotoTagRecord (itemId, tagId)
     *        SELECT 153567, tagId
     *          FROM PhotoTagRecord
     *         WHERE itemId = 89736;
     */
    public int copyTags (final int fromItemId, final int toItemId,
                         int ownerId, long now)
            throws PersistenceException
    {
        final String tagTable =
            _ctx.getMarshaller(getTagClass()).getTableName();
        int rows = _ctx.invoke(new Modifier(null) {
            public int invoke (Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        " INSERT INTO " + tagTable +
                        " (" + TagRecord.ITEM_ID + ", " +
                               TagRecord.TAG_ID + ")" +
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

    protected abstract Class<T> getItemClass ();
    protected abstract Class<? extends CloneRecord<T>> getCloneClass ();
    protected abstract Class<? extends CatalogRecord<T>> getCatalogClass ();

    protected abstract Class<? extends TagRecord<T>> getTagClass ();
    protected abstract Class<? extends TagHistoryRecord<T>> getTagHistoryClass ();
    protected abstract Class<? extends RatingRecord<T>> getRatingClass ();
}
