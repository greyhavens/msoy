//
// $Id$

package com.threerings.msoy.comment.server.persist;

import java.sql.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.comment.data.all.Comment;

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
     * @param beforeTime the time offset into the comments to load.
     * @param count the number of comments to load.
     */
    public ExpanderResult<CommentThread> loadComments (
        int entityType, int entityId, long beforeTime, int count, int repliesPerComment)
    {
        List<SQLExpression<?>> conditions = Lists.newArrayList();
        conditions.add(CommentRecord.ENTITY_TYPE.eq(entityType));
        conditions.add(CommentRecord.ENTITY_ID.eq(entityId));
        conditions.add(CommentRecord.REPLY_TO.isNull());
        if (beforeTime < Long.MAX_VALUE) {
            conditions.add(CommentRecord.POSTED.lessThan(new Timestamp(beforeTime)));
        }

        // Fetch the non-reply comments for this page
        List<CommentRecord> comments = from(CommentRecord._R)
            .where(conditions)
            .descending(CommentRecord.POSTED)
            .limit(count + 1)
            .select();

        ExpanderResult<CommentThread> result = new ExpanderResult<CommentThread>();
        if (comments.size() > count) {
            comments = comments.subList(0, count);
            result.hasMore = true;
        }

        // Assemble this page's threads
        Map<Timestamp, CommentThread> threads = Maps.newTreeMap();
        for (CommentRecord comment : comments) {
            threads.put(comment.posted, new CommentThread(comment));
        }

        // Load the replies for each thread
        Set<Timestamp> postIds = Sets.newHashSet(Lists.transform(comments, TO_POSTED));
        while (!postIds.isEmpty()) {
            // Load up a block of replies
            List<CommentRecord> replies = from(CommentRecord._R)
                .where(CommentRecord.ENTITY_TYPE.eq(entityType),
                    CommentRecord.ENTITY_ID.eq(entityId),
                    CommentRecord.REPLY_TO.in(postIds))
                .limit(count*repliesPerComment)
                .descending(CommentRecord.POSTED)
                .select();

            for (CommentRecord reply : replies) {
                CommentThread thread = threads.get(reply.replyTo);
                if (!thread.replies.contains(reply)) {
                    if (thread.replies.size() < repliesPerComment) {
                        thread.replies.add(reply);
                    } else {
                        thread.hasMoreReplies = true;
                        postIds.remove(reply.replyTo);
                    }
                }
            }

            if (replies.size() < count*repliesPerComment) {
                // Stop if we're definitely out of remaining replies
                break;
            }
        }

        result.page = ImmutableList.copyOf(threads.values());
        return result;
    }

    /**
     * Loads count replies that were made before a timestamp.
     */
    public CommentThread loadReplies (
        int entityType, int entityId, long replyTo, long beforeTime, int count)
    {
        List<CommentRecord> replies = from(CommentRecord._R)
            .where(CommentRecord.ENTITY_TYPE.eq(entityType),
               CommentRecord.ENTITY_ID.eq(entityId),
               CommentRecord.REPLY_TO.eq(new Timestamp(replyTo)),
               CommentRecord.POSTED.lessThan(new Timestamp(beforeTime)))
            .limit(count + 1) // Request one extra
            .descending(CommentRecord.POSTED)
            .select();

        CommentThread thread = new CommentThread(null);
        if (replies.size() > count) {
            replies = replies.subList(0, count);
            thread.hasMoreReplies = true;
        }
        thread.replies = Sets.newTreeSet(replies);

        return thread;
    }

    /**
     * Loads the given member's ratings of the comments for the given entity.
     */
    public List<CommentRatingRecord> loadRatings (int entityType, int entityId, int memberId)
    {
        return findAll(CommentRatingRecord.class,
                       new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                                 CommentRatingRecord.ENTITY_ID, entityId,
                                 CommentRatingRecord.MEMBER_ID, memberId));
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
     * Loads the total number of non-reply comments posted to the specified entity.
     */
    public int loadCommentCount (int entityType, int entityId)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new FromOverride(CommentRecord.class));
        clauses.add(new Where(CommentRecord.ENTITY_TYPE, entityType,
                              CommentRecord.ENTITY_ID, entityId,
                              CommentRecord.REPLY_TO, null));
        return load(CountRecord.class, clauses.toArray(new QueryClause[clauses.size()])).count;
    }

    /**
     * Posts a new comment on the specified entity by the specified member.
     */
    public CommentRecord postComment (int entityType, int entityId, long replyTo,
        int memberId, String text)
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
        record.currentRating = 1;
        record.text = text;
        if (replyTo > 0) {
            record.replyTo = new Timestamp(replyTo);
        }
        insert(record);

        return record;
    }

    /**
     * Inserts a new rating for a comment by a given member.
     * @return true if the comment's rating changed
     */
    public int rateComment (
        int entityType, int entityId, long posted, int memberId, boolean rating)
    {
        Timestamp postedStamp = new Timestamp(posted);
        try {
            // see if this person has rated this record before
            CommentRatingRecord record = load(CommentRatingRecord.class,
                CommentRatingRecord.getKey(entityType, entityId, memberId, postedStamp));

            int adjustment;
            if (record != null) {
                if (record.rating == rating) {
                    // re-rated precisely as previously; we're done
                    return 0;
                }
                // previously rated and user changed their mind; comment gains or loses 2 votes
                adjustment = rating ? 2 : -2;
            } else {
                // previously unrated; the comment gains or loses 1 vote
                adjustment = rating ? 1 : -1;
            }

            // create a new record with the new rating
            CommentRatingRecord newRecord =
                new CommentRatingRecord(entityType, entityId, postedStamp, memberId, rating);

            // insert or update depending on what we already had
            if (record == null) {
                insert(newRecord);
            } else {
                update(newRecord);
            }

            // then update the sums in the comment
            Key<CommentRecord> comment = CommentRecord.getKey(entityType, entityId, postedStamp);
            Map<ColumnExp<?>, SQLExpression<?>> updates = Maps.newHashMap();
            updates.put(CommentRecord.CURRENT_RATING,
                        CommentRecord.CURRENT_RATING.plus(adjustment));
            if (record != null) {
                updates.put(CommentRecord.TOTAL_RATINGS, CommentRecord.TOTAL_RATINGS.plus(1));
            }
            updatePartial(CommentRecord.class, comment, comment, updates);
            return adjustment;

        } catch (DuplicateKeyException dke) {
            log.warning("Ignoring duplicate comment rating", "entityType", entityType,
                        "entityId", entityId, "posted", postedStamp, "memberId", memberId,
                        "rating", rating);
            return 0;
        }
    }

    /**
     * Deletes the comment with the specified key.
     */
    public void deleteComment (int entityType, int entityId, long posted)
    {
        Timestamp postedStamp = new Timestamp(posted);

        // delete the comment
        delete(CommentRecord.getKey(entityType, entityId, postedStamp));

        // delete all its ratings
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                            CommentRatingRecord.ENTITY_ID, entityId,
                            CommentRatingRecord.POSTED, postedStamp));
    }

    /**
     * Deletes all comments for the specified entity.
     */
    public void deleteComments (int entityType, int entityId)
    {
        // delete the comments
        deleteAll(CommentRecord.class, new Where(CommentRecord.ENTITY_TYPE, entityType,
                                                 CommentRecord.ENTITY_ID, entityId), null);

        // delete the comment ratings
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.ENTITY_TYPE, entityType,
                            CommentRatingRecord.ENTITY_ID, entityId), null);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging *
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // delete all ratings made by these members
        deleteAll(CommentRatingRecord.class,
                  new Where(CommentRatingRecord.MEMBER_ID.in(memberIds)));

        // load up the ids of all comments made by these members
        List<Key<CommentRecord>> keys = findAllKeys(
            CommentRecord.class, false, new Where(CommentRecord.MEMBER_ID.in(memberIds)));

        // delete those comments
        deleteAll(CommentRecord.class, KeySet.newKeySet(CommentRecord.class, keys));

        // TODO: delete all rating records made on the above comments
    }

    // Just used as a return structure for loadComments()
    public static class CommentThread
    {
        public CommentRecord comment;
        public Set<CommentRecord> replies = Sets.newTreeSet();
        public boolean hasMoreReplies;

        public CommentThread (CommentRecord comment)
        {
            this.comment = comment;
        }
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CommentRecord.class);
        classes.add(CommentRatingRecord.class);
    }

    public static Function<CommentRecord, Timestamp> TO_POSTED =
        new Function<CommentRecord, Timestamp>() {
            public Timestamp apply (CommentRecord comment) {
                return comment.posted;
            }
        };
}
