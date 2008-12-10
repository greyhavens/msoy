//
// $Id$

package com.threerings.msoy.comment.server.persist;

import java.sql.Timestamp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.server.persist.CountRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages member comments on various and sundry things.
 */
@Singleton @BlockingThread
public class CommentRepository extends DepotRepository
{
    @Inject public CommentRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Loads the most recent comments for the specified entity type and identifier.
     *
     * @param start the offset into the comments (in reverse time order) to load.
     * @param count the number of comments to load.
     */
    public List<CommentRecord> loadComments (int entityType, int entityId, int start, int count)
    {
        // load up the specified comment set
        return findAll(CommentRecord.class,
                       new Where(CommentRecord.ENTITY_TYPE_C, entityType,
                                 CommentRecord.ENTITY_ID_C, entityId),
                       OrderBy.descending(CommentRecord.POSTED_C),
                       new Limit(start, count));
    }

    /**
     * Loads the given member's ratings of the comments for the given entity.
     */
    public List<CommentRatingRecord> loadRatings (int entityType, int entityId, int memberId)
    {
        return findAll(CommentRatingRecord.class,
                       new Where(CommentRatingRecord.ENTITY_TYPE_C, entityType,
                                 CommentRatingRecord.ENTITY_ID_C, entityId,
                                 CommentRatingRecord.MEMBER_ID_C, memberId));
    }

    /**
     * Loads the given member's ratings of the comments for the given entity.
     */
    public CommentRatingRecord loadRating (
        int entityType, int entityId, long posted, int memberId)
    {
        return load(CommentRatingRecord.class, CommentRatingRecord.getKey(
                        entityType, entityId, memberId, new Timestamp(posted)));
    }

    /**
     * Loads a specific comment record.
     */
    public CommentRecord loadComment (int entityType, int entityId, long posted)
    {
        return load(CommentRecord.class,
                    CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    /**
     * Loads the total number of comments posted to the specified entity.
     */
    public int loadCommentCount (int entityType, int entityId)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(CommentRecord.class));
        clauses.add(new Where(CommentRecord.ENTITY_TYPE_C, entityType,
                              CommentRecord.ENTITY_ID_C, entityId));
        return load(CountRecord.class, clauses).count;
    }

    /**
     * Posts a new comment on the specified entity by the specified member.
     */
    public CommentRecord postComment (int entityType, int entityId, int memberId, String text)
    {
        if (text.length() > Comment.MAX_TEXT_LENGTH) { // sanity check
            throw new DatabaseException(
                "Rejecting overlong comment [type=" + entityType + ", id=" + entityId +
                ", who=" + memberId + ", length=" + text.length() + "]");
        }

        CommentRecord record = new CommentRecord();
        record.entityType = entityType;
        record.entityId = entityId;
        record.posted = new Timestamp(System.currentTimeMillis());
        record.memberId = memberId;
        record.text = text;
        insert(record);

        return record;
    }

    /**
     * Inserts a new rating for a comment by a given member.
     * @return true for success, false if the member had already rated the comment
     */
    public boolean rateComment (
        int entityType, int entityId, long posted, int memberId, boolean rating)
    {
        Timestamp postedStamp = new Timestamp(posted);
        try {
            // insert a new rating record for us
            insert(new CommentRatingRecord(entityType, entityId, postedStamp, memberId, rating));

            // then update the sums in the comment
            Key<CommentRecord> comment = CommentRecord.getKey(entityType, entityId, postedStamp);
            Map<String, SQLExpression> updates = Maps.newHashMap();
            updates.put(CommentRecord.CURRENT_RATING, new Arithmetic.Add(
                            CommentRecord.CURRENT_RATING_C, rating ? 1 : -1));
            updates.put(CommentRecord.TOTAL_RATINGS, new Arithmetic.Add(
                            CommentRecord.TOTAL_RATINGS_C, 1));
            updateLiteral(CommentRecord.class, comment, comment, updates);
            return true;

        } catch (DuplicateKeyException dke) {
            log.warning("Ignoring duplicate comment rating", "entityType", entityType,
                        "entityId", entityId, "posted", postedStamp, "memberId", memberId,
                        "rating", rating);
            return false;
        }
    }

    /**
     * Deletes the comment with the specified key.
     */
    public void deleteComment (int entityType, int entityId, long posted)
    {
        delete(CommentRecord.class,
               CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CommentRecord.class);
        classes.add(CommentRatingRecord.class);
    }
}
