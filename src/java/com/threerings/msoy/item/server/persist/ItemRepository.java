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

import com.samskivert.Log;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
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
        // TODO: This will be a single join once Depot can handle it. It's a
        // TODO: special case because itemId is ambiguous between the tables.
        // TODO: We will still probably need to iterate afterwards, but not
        // TODO: hit the database further.
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
        if (cloneRecord == null) {
            throw new PersistenceException(
                "Clone does not exist [cloneId=" + cloneId + "]");
        }
        ItemRecord clone = loadItem(cloneRecord.originalItemId);
        if (clone == null) {
            throw new PersistenceException(
                "Clone's original does not exist [cloneId=" + cloneId + 
                ", originalItemId=" + cloneRecord.originalItemId + "]");
        }
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
    public void deleteClone (int cloneId)
            throws PersistenceException
    {
        delete(getCloneClass(), cloneId);
    }

    public RatingRecord<T> getRating (int itemId, int memberId)
            throws PersistenceException
    {
        return load(getRatingClass(), new Key2(
            RatingRecord.ITEM_ID, itemId, RatingRecord.MEMBER_ID, memberId));
            
    }

    /** Insert/update a rating row, calculate the new rating and finally
     *  update the item's rating. */
    public float rateItem (int itemId, int memberId, byte rating)
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
        float newRating;
        String ratingTable = getMarshaller(getRatingClass()).getTableName();
        Connection conn = _conprov.getConnection(getIdent(), false);
        try {
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement(
                    " SELECT COUNT(*), SUM(" + RatingRecord.RATING + ")" +
                    "   FROM " + ratingTable +
                    "  WHERE " + RatingRecord.ITEM_ID + " = ?");
                stmt.setInt(1, itemId);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new PersistenceException(
                        "Failed to calculate new rating for item [itemId=" +
                        itemId + "]"); 
                }
                int count = rs.getInt(1);
                double sum = rs.getInt(2);
                newRating = (float) ((count == 0) ? 0.0 : sum/count);
            } finally {
                JDBCUtil.close(stmt);
            }
        } catch (SQLException sqle) {
            throw new PersistenceException(
                "Failed to calculate new rating for item [itemId=" +
                itemId + "]");
        } finally {
            _conprov.releaseConnection(getIdent(), false, conn);
        }
        
        // and then smack the new value into the item using yummy depot code
        updatePartial(
            getItemClass(), itemId, new Object[] {
                ItemRecord.RATING, newRating });
        return newRating;
    }
    
    /** Load all tag records for the given item, translated to tag names. */
    public Iterable<TagNameRecord> getTags (int itemId)
            throws PersistenceException
    {
        ArrayList<TagNameRecord> result = new ArrayList<TagNameRecord>();
        // TODO: This will obviously be a join as soon as Depot can do one.
        for (TagRecord tagRecord :
                findAll(getTagClass(), new Key(TagRecord.ITEM_ID, itemId))) {
            result.add(getTag(tagRecord.tagId));
        }
        return result;
    }

    /** Load all the tag history records for a given item. */
    public Iterable<? extends TagHistoryRecord<T>> getTagHistoryByItem (
        int itemId)
            throws PersistenceException
    {
        return findAll(
            getTagHistoryClass(),
            new Key(TagHistoryRecord.ITEM_ID, itemId));
    }
    
    /** Load all the tag history records for a given member. */
    public Iterable<? extends TagHistoryRecord<T>> getTagHistoryByMember (
        int memberId)
            throws PersistenceException
    {
        return findAll(
            getTagHistoryClass(),
            new Key(TagHistoryRecord.MEMBER_ID, memberId));
    }

    /** Find the tag record for a certain tag, or create it. */
    public TagNameRecord getTag (String tagName)
            throws PersistenceException
    {
        // load the tag, if it exists
        TagNameRecord record = load(
            TagNameRecord.class, new Key(TagNameRecord.TAG, tagName));
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
        TagRecord<T> tag = load(getTagClass(), new Key2(
            TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId));
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
        TagRecord<T> tag = load(getTagClass(), new Key2(
            TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId));
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
    public int copyTags (int fromItemId, int toItemId, int ownerId, long now)
            throws PersistenceException
    {
        int rows;
        String tagTable = getMarshaller(getTagClass()).getTableName();
        Connection conn = _conprov.getConnection(getIdent(), false);
        try {
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
                rows = stmt.executeUpdate();
            } finally {
                JDBCUtil.close(stmt);
            }
        } catch (SQLException sqle) {
            throw new PersistenceException(
                "Failed to copy tags to remixed item [fromItemId=" + 
                fromItemId + ", toItemId=" + toItemId + "]", sqle);
        } finally {
            _conprov.releaseConnection(getIdent(), false, conn);
        }
        
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

    protected abstract Class<? extends TagRecord<T>> getTagClass ();
    protected abstract Class<? extends TagHistoryRecord<T>> getTagHistoryClass ();
    protected abstract Class<? extends RatingRecord<T>> getRatingClass ();
}
