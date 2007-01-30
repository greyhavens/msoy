//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.Collection;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Modifier;
import com.samskivert.jdbc.depot.clause.FieldOverride;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.web.TagHistory;

/**
 * Manages the persistent side of tagging of things in the MetaSOY system (right now items and
 * groups).
 */
public class TagRepository<T extends TagRecord,HT extends TagHistoryRecord> extends DepotRepository
{
    /**
     * Creates a tag repository for the supplied tag and tag history record classes.
     */
    public TagRepository (ConnectionProvider conprov,
                          Class<T> tagClass, Class<HT> tagHistoryClass)
    {
        super(conprov);
        _tagClass = tagClass;
        _tagHistoryClass = tagHistoryClass;
    }

    /**
     * Join TagNameRecord and TagRecord, group by tag, and count how many items
     * reference each such tag.
     */
    public Collection<TagPopularityRecord> getPopularTags (int rows)
        throws PersistenceException
    {
        return findAll(TagPopularityRecord.class,
                       new FromOverride(_tagClass),
                       new Join(new ColumnExp(_tagClass, TagRecord.TAG_ID), TagNameRecord.TAG_ID_C),
                       new FieldOverride(TagPopularityRecord.TAG_ID, TagNameRecord.TAG_ID_C),
                       new FieldOverride(TagPopularityRecord.TAG, TagNameRecord.TAG_C),
                       new FieldOverride(TagPopularityRecord.COUNT, "count(*)"),
                       new GroupBy(TagNameRecord.TAG_ID_C));
    }

    /**
     * Loads all tag records for the given item, translated to tag names.
     */
    public Collection<TagNameRecord> getTags (int itemId)
        throws PersistenceException
    {
        return findAll(TagNameRecord.class,
                       new Where(TagRecord.ITEM_ID, itemId),
                       new Join(TagNameRecord.TAG_ID_C,
                                new ColumnExp(_tagClass, TagRecord.TAG_ID)));
    }

    /**
     * Loads all the tag history records for a given item.
     */
    public Collection<HT> getTagHistoryByItem (int itemId)
        throws PersistenceException
    {
        return findAll(_tagHistoryClass, new Where(TagHistoryRecord.ITEM_ID, itemId));
    }

    /**
     * Loads all the tag history records for a given member.
     */
    public Collection<HT> getTagHistoryByMember (int memberId)
        throws PersistenceException
    {
        return findAll(_tagHistoryClass, new Where(TagHistoryRecord.MEMBER_ID, memberId));
    }

    /**
     * Finds the tag record for a certain tag, or create it.
     */
    public TagNameRecord getTag (String tagName)
        throws PersistenceException
    {
        // load the tag, if it exists
        TagNameRecord record = load(TagNameRecord.class, TagNameRecord.TAG, tagName);
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
        return load(TagNameRecord.class, tagId);
    }

    /**
     * Add a tag to an item. If the tag already exists, return false and do nothing else. If it did
     * not, create the tag and add a record in the history table.
     */
    public TagHistoryRecord tagItem (int itemId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord tag = load(_tagClass, TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId);
        if (tag != null) {
            return null;
        }

        try {
            tag = _tagClass.newInstance();
        } catch (Exception e) {
            throw new PersistenceException("Failed to create a new item tag record " +
                                           "[itemId=" + itemId + ", tagId=" + tagId + "]", e);
        }
        tag.itemId = itemId;
        tag.tagId = tagId;
        insert(tag);

        TagHistoryRecord history;
        try {
            history = _tagHistoryClass.newInstance();
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
    public TagHistoryRecord untagItem (int itemId, int tagId, int taggerId, long now)
        throws PersistenceException
    {
        TagRecord tag = load(_tagClass, TagRecord.ITEM_ID, itemId, TagRecord.TAG_ID, tagId);
        if (tag == null) {
            return null;
        }
        delete(tag);

        TagHistoryRecord history;
        try {
            history = _tagHistoryClass.newInstance();
        } catch (Exception e) {
            throw new PersistenceException("Failed to create a new item tag history tag record " +
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
        final String tagTable = _ctx.getMarshaller(_tagClass).getTableName();
        int rows = _ctx.invoke(new Modifier() {
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
        TagHistoryRecord history;
        try {
            history = _tagHistoryClass.newInstance();
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

    protected Class<T> _tagClass;
    protected Class<HT> _tagHistoryClass;
}
