//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.StringFuncs;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.GroupBy;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.impl.Modifier;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.web.gwt.TagHistory;

/**
 * Manages the persistent side of tagging of things in the MetaSOY system (right now items and
 * groups).
 */
@BlockingThread
public abstract class TagRepository extends DepotRepository
{
    /**
     * Creates a tag repository for the supplied tag and tag history record classes.
     */
    public TagRepository (PersistenceContext ctx)
    {
        super(ctx);

        @SuppressWarnings("unchecked") Class<TagRecord> tagClass = (Class<TagRecord>)
            createTagRecord().getClass();
        _tagClass = tagClass;

        @SuppressWarnings("unchecked") Class<TagHistoryRecord> thClass = (Class<TagHistoryRecord>)
            createTagHistoryRecord().getClass();
        _tagHistoryClass = thClass;
    }

    /**
     * Creates a {@link TagRecord} derived instance the class for which defines a custom table name
     * for our tag record class. For example:
     * <pre>
     * @Entity(name="PhotoTagRecord")
     * public class PhotoTagRecord extends TagRecord {}
     * </pre>
     */
    protected abstract TagRecord createTagRecord ();

    /**
     * Creates a {@link TagRecord} derived instance the class for which defines a custom table name
     * for our tag record class. See {@link #createTagRecord}.
     */
    protected abstract TagHistoryRecord createTagHistoryRecord ();

    /**
     * Exports the specific tag class used by this repository, for joining purposes.
     */
    public Class<TagRecord> getTagClass ()
    {
        return _tagClass;
    }

    /**
     * Converts a column expression from {@link TagRecord} to one of the appropriate derived type
     * for the records handled by this repository.
     */
    public <T> ColumnExp<T> getTagColumn (ColumnExp<T> col)
    {
        return col.as(getTagClass());
    }

    /**
     * Exports the specific tag history class used by this repository, for joining purposes.
     */
    public Class<TagHistoryRecord> getTagHistoryClass ()
    {
        return _tagHistoryClass;
    }

    /**
     * Converts a column expression from {@link TagHistoryRecord} to one of the appropriate derived
     * type for the records handled by this repository.
     */
    public <T> ColumnExp<T> getTagHistoryColumn (ColumnExp<T> col)
    {
        return col.as(getTagHistoryClass());
    }

    /**
     * Join TagNameRecord and TagRecord, group by tag, and count how many targets reference each
     * such tag. The query is relatively expensive and the result is unlikely to change rapidly
     * in a production environment, so we cache for POPULAR_TAG_EXPIRATION seconds.
     */
    public List<TagPopularityRecord> getPopularTags (int rows)
    {
        int now = (int) (System.currentTimeMillis() / 1000);
        if (rows > _popularTags.size() || now >= _popularTagExpiration) {
            _popularTags = findAll(TagPopularityRecord.class,
                new FromOverride(getTagClass()),
                new Limit(0, rows),
                getTagColumn(TagRecord.TAG_ID).join(TagNameRecord.TAG_ID),
                OrderBy.descending(TagPopularityRecord.COUNT),
                new GroupBy(TagNameRecord.TAG_ID, TagNameRecord.TAG));

            _popularTagExpiration = DeploymentConfig.devDeployment ?
                now : now + POPULAR_TAG_EXPIRATION;
        }
        return _popularTags.subList(0, Math.min(rows, _popularTags.size()));
    }

    /**
     * Loads all tag records for the given target, translated to tag names.
     */
    public List<TagNameRecord> getTags (int targetId)
    {
        return findAll(TagNameRecord.class,
                       new Where(getTagColumn(TagRecord.TARGET_ID), targetId),
                       TagNameRecord.TAG_ID.join(getTagColumn(TagRecord.TAG_ID)));
    }

    /**
     * Loads all the tag history records for a given target.
     */
    public List<TagHistoryRecord> getTagHistoryByTarget (int targetId, int offset, int rows)
    {
        List<QueryClause> clauses = Lists.newArrayList(
            new Where(getTagHistoryColumn(TagHistoryRecord.TARGET_ID), targetId),
            OrderBy.descending(getTagHistoryColumn(TagHistoryRecord.TIME)));

        if (rows != 0) {
            clauses.add(new Limit(offset, rows));
        }

        return findAll(getTagHistoryClass(), clauses);

    }

    /**
     * Gets the most recent addition of the given tag.
     */
    public TagHistoryRecord getLastAddition (int targetId, int tagId)
    {
        List<TagHistoryRecord> hist = findAll(getTagHistoryClass(),
            new Where(getTagHistoryColumn(TagHistoryRecord.TARGET_ID), targetId,
                getTagHistoryColumn(TagHistoryRecord.TAG_ID), tagId,
                getTagHistoryColumn(TagHistoryRecord.ACTION), TagHistory.ACTION_ADDED),
            new Limit(0, 1), OrderBy.descending(getTagHistoryColumn(TagHistoryRecord.TIME)));

        return hist.isEmpty() ? null : hist.get(0);
    }

    /**
     * Loads all the tag history records for a given member.
     */
    public List<TagHistoryRecord> getTagHistoryByMember (int memberId)
    {
        return findAll(getTagHistoryClass(),
                       new Where(getTagHistoryColumn(TagHistoryRecord.MEMBER_ID), memberId));
    }

    /**
     * Loads all tag records for the specified tags.
     */
    public List<TagNameRecord> getTags (String[] tags)
    {
        String[] newTags = new String[tags.length];
        for (int ii = 0; ii < tags.length; ii ++) {
            newTags[ii] = tags[ii].toLowerCase();
        }
        return findAll(TagNameRecord.class, new Where(
            StringFuncs.lower(TagNameRecord.TAG).in(newTags)));
    }

    /**
     * Returns the id for the specified tag or 0 if no such tag exists.
     */
    public int getTagId (String tagName)
    {
        TagNameRecord tnr = getTag(tagName);
        return (tnr == null) ? 0 : tnr.tagId;
    }

    /**
     * Finds the tag record for a certain tag.
     */
    public TagNameRecord getTag (String tagName)
    {
        return load(TagNameRecord.class, new Where(
            StringFuncs.lower(TagNameRecord.TAG).eq(tagName.toLowerCase())));
    }

    /**
     * Finds the tag record for a certain tag, or create it.
     */
    public TagNameRecord getOrCreateTag (String tagName)
    {
        // load the tag, if it exists
        TagNameRecord record = getTag(tagName);
        if (record == null) {
            // if it doesn't, create it on the fly
            record = new TagNameRecord();
            record.tag = tagName;
            insert(record);
        }
        return record;
    }

    /**
     * Find the tag record for a certain tag id.
     */
    public TagNameRecord getTag (int tagId)
    {
        return load(TagNameRecord.getKey(tagId));
    }

    /**
     * Find the tag records for a certain set of tag id's.
     */
    public Map<Integer, TagNameRecord> getTags (Set<Integer> tagIds)
    {
        Map<Integer, TagNameRecord> result = Maps.newHashMap();
        for (TagNameRecord rec : loadAll(TagNameRecord.class, tagIds)) {
            result.put(rec.tagId, rec);
        }
        return result;
    }

    /**
     * Adds a tag to a target. If the tag already exists, returns false and do nothing else. If it
     * did not, creates the tag and adds a record in the history table.
     */
    public TagHistoryRecord tag (int targetId, int tagId, int taggerId, long now)
    {
        TagRecord tag = load(getTagKey(tagId, targetId));
        if (tag != null) {
            return null;
        }

        tag = createTagRecord();
        tag.targetId = targetId;
        tag.tagId = tagId;
        insert(tag);

        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = targetId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_ADDED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Removes a tag from a target. If the tag didn't exist, returns false and do nothing else. If
     * it did, removes the tag and adds a record in the history table.
     */
    public TagHistoryRecord untag (int targetId, int tagId, int taggerId, long now)
    {
        TagRecord tag = load(getTagKey(tagId, targetId));
        if (tag == null) {
            return null;
        }
        delete(tag);

        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = targetId;
        history.tagId = tagId;
        history.memberId = taggerId;
        history.action = TagHistory.ACTION_REMOVED;
        history.time = new Timestamp(now);
        insert(history);
        return history;
    }

    /**
     * Copy all tags from one target to another. We have to resort to JDBC here, because we want to
     * do the rather non-generic:
     *
     *   INSERT INTO PhotoTagRecord (targetId, tagId)
     *        SELECT 153567, tagId
     *          FROM PhotoTagRecord
     *         WHERE targetId = 89736;
     *
     * TODO: Depot is very very close to being able to handle this.
     */
    public int copyTags (final int fromTargetId, final int toTargetId, int ownerId, long now)
    {
        final String tagTable = _ctx.getMarshaller(getTagClass()).getTableName();
        int rows = _ctx.invoke(new Modifier() {
            protected int invoke (Connection conn, DatabaseLiaison liaison) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(
                        " INSERT INTO " + liaison.tableSQL(tagTable) + " (" +
                        liaison.columnSQL(TagRecord.TARGET_ID.name) + ", " +
                        liaison.columnSQL(TagRecord.TAG_ID.name) + ")" +
                        "      SELECT ?, " + liaison.columnSQL(TagRecord.TAG_ID.name) +
                        "        FROM " + liaison.tableSQL(tagTable) +
                        "       WHERE " + liaison.columnSQL(TagRecord.TARGET_ID.name) + " = ?");
                    stmt.setInt(1, toTargetId);
                    stmt.setInt(2, fromTargetId);
                    return stmt.executeUpdate();
                } finally {
                    JDBCUtil.close(stmt);
                }
            }
        });

        // add a single row to history for the copy
        TagHistoryRecord history = createTagHistoryRecord();
        history.targetId = toTargetId;
        history.tagId = -1;
        history.memberId = ownerId;
        history.action = TagHistory.ACTION_COPIED;
        history.time = new Timestamp(now);
        insert(history);
        return rows;
    }

    /**
     * Deletes all tag and history records associated with the specified target.
     */
    public void deleteTags (final int targetId)
    {
        // invalidate and delete tag records for this target
        deleteAll(getTagClass(), new Where(getTagColumn(TagRecord.TARGET_ID), targetId));

        // invalidate and delete tag history records for this target
        deleteAll(getTagHistoryClass(),
                  new Where(getTagHistoryColumn(TagHistoryRecord.TARGET_ID), targetId));
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // note: this is a full table scan, but currently probably ok
        deleteAll(getTagHistoryClass(),
                  new Where(getTagHistoryColumn(TagHistoryRecord.MEMBER_ID).in(memberIds)));
    }

    protected Key<TagRecord> getTagKey (int tagId, int targetId)
    {
        return new Key<TagRecord>(getTagClass(),
                                  new ColumnExp<?>[] { getTagColumn(TagRecord.TAG_ID),
                                                    getTagColumn(TagRecord.TARGET_ID) },
                                  new Comparable<?>[] { tagId, targetId });
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(createTagRecord().getClass());
        classes.add(createTagHistoryRecord().getClass());
        classes.add(TagNameRecord.class);
    }

    protected Class<TagRecord> _tagClass;
    protected Class<TagHistoryRecord> _tagHistoryClass;
    protected List<TagPopularityRecord> _popularTags = Lists.newArrayList();
    protected int _popularTagExpiration;

    /** How long we cache the results of the popular tags query, in seconds. */
    protected static int POPULAR_TAG_EXPIRATION = 30 * 60;  // half an hour
}
