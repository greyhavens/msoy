//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntIntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.samskivert.depot.CountRecord;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Exps;
import com.samskivert.depot.Key;
import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.Query;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.FullText;

import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.server.persist.RepositoryUtil;

import static com.threerings.msoy.Log.log;

/**
 * Manages forum threads and messages.
 */
@Singleton @BlockingThread
public class ForumRepository extends DepotRepository
{
    @Inject public ForumRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(ForumMessageRecord.class,
            new SchemaMigration.Retype(6, ForumMessageRecord.MESSAGE));
    }

    /**
     * Loads up to the specified maximum number of threads from the supplied set of groups that
     * have messages that are unread by the specified member. The results are ordered from newest
     * to oldest.
     */
    public List<ForumThreadRecord> loadUnreadThreads (
        int memberId, Set<Integer> groupIds, int start, int count)
    {
        return forUnreadThreads(from(ForumThreadRecord.class), memberId, groupIds)
            .limit(start, count)
            .descending(ForumThreadRecord.MOST_RECENT_POST_ID)
            .cache(CacheStrategy.RECORDS)
        .select();
    }

    /**
     * Counts how many threads there are posted to the given set of groups that have not been read
     * by the given member.
     */
    public int countUnreadThreads (int memberId, Set<Integer> groupIds)
    {
        return forUnreadThreads(from(ForumThreadRecord.class), memberId, groupIds).selectCount();
    }

    /**
     * Loads recent threads containing posts by the given posters that are unread by the given
     * member, up to a maximum. Threads for the given hidden groups are not returned.
     */
    public List<ForumThreadRecord> loadUnreadFriendThreads (
        int memberId, Set<Integer> posterIds, Set<Integer> hiddenGroupIds, int offset, int count)
    {
        if (posterIds.isEmpty()) {
            return Collections.emptyList();
        }
        return forUnreadFriendThreads(
            from(ForumThreadRecord.class), memberId, posterIds, hiddenGroupIds)
            .limit(offset, count)
            .descending(ForumThreadRecord.MOST_RECENT_POST_ID)
            .select();
    }

    /**
     * Counts the number of recent threads containing posts by the given posters that are unread by
     * the given member. Threads for the given hidden groups are not counted.
     */
    public int countUnreadFriendThreads (
        int memberId, Set<Integer> posterIds, Set<Integer> hiddenGroupIds)
    {
        if (posterIds.isEmpty()) {
            return 0;
        }
        return forUnreadFriendThreads(
            from(ForumThreadRecord.class), memberId, posterIds, hiddenGroupIds)
            .select(ForumThreadRecord.THREAD_ID).size();
    }

    /**
     * Loads the latest threads for the specified group. Ordered by threadId (ie: creation time).
     */
    public List<ForumThreadRecord> loadRecentThreads (int groupId, int count)
    {
        return from(ForumThreadRecord.class).where(ForumThreadRecord.GROUP_ID, groupId)
            .limit(count).descending(ForumThreadRecord.THREAD_ID).select();
    }

    /**
     * Loads the total number of threads in the specified group.
     */
    public int loadThreadCount (int groupId)
    {
        return from(ForumThreadRecord.class).where(ForumThreadRecord.GROUP_ID, groupId)
            .selectCount();
    }

    /**
     * Loads the total number of messages in all threads in the specified group.
     */
    public int loadMessageCount (int groupId)
    {
        return from(ForumThreadRecord.class).where(
            ForumThreadRecord.GROUP_ID.eq(groupId),
            ForumThreadRecord.THREAD_ID.eq(ForumMessageRecord.THREAD_ID))
            .selectCount();
    }

    /**
     * Loads the specified range of forum threads for the specified group. Ordered first stickyness
     * and then by most recently updated (newer first).
     */
    public List<ForumThreadRecord> loadThreads (int groupId, int offset, int count)
    {
        return from(ForumThreadRecord.class).cacheRecords()
            .where(ForumThreadRecord.GROUP_ID, groupId)
            .limit(offset, count)
            .orderBy(new OrderBy(
                new SQLExpression<?>[]{ForumThreadRecord.STICKY,
                        ForumThreadRecord.MOST_RECENT_POST_ID},
                    new OrderBy.Order[]{OrderBy.Order.DESC, OrderBy.Order.DESC}))
            .select();
    }

    /**
     * Load the list of threads with ids in the given set, in no particular order.
     */
    public List<ForumThreadRecord> loadThreads (Collection<Integer> threadIds)
    {
        return from(ForumThreadRecord.class).
            where(ForumThreadRecord.THREAD_ID.in(threadIds))
            .select();
    }

    /**
     * Finds all threads that match the specified search in their subject or for which one or more
     * of their messages matches the supplied search.
     */
    public List<ForumThreadRecord> findThreads (int groupId, String search, int offset, int count)
    {
        FullText ftsSubject = new FullText(ForumThreadRecord.class,
            ForumThreadRecord.FTS_SUBJECT, search);
        FullText ftsMessage = new FullText(ForumMessageRecord.class,
            ForumMessageRecord.FTS_MESSAGE, search);
        return from(ForumThreadRecord.class)
            .join(ForumThreadRecord.THREAD_ID, ForumMessageRecord.THREAD_ID)
            .where(ForumThreadRecord.GROUP_ID.eq(groupId),
                   Ops.or(ftsSubject.match(), ftsMessage.match()))
            .descending(ftsSubject.rank().times(5).plus(ftsMessage.rank()))
            .limit(offset, count)
            .select();
    }

    /**
     * Finds all threads in a list of groups that match the specified search in their subject or
     * for which one or more of their messages matches the supplied search.
     */
    public List<ForumThreadRecord> findThreadsIn (
        int memberId, Set<Integer> groupIds, String search, int offset, int count)
    {
        if (groupIds.isEmpty()) {
            return Collections.emptyList();
        }

        FullText ftsSubject = new FullText(ForumThreadRecord.class,
            ForumThreadRecord.FTS_SUBJECT, search);
        FullText ftsMessage = new FullText(ForumMessageRecord.class,
            ForumMessageRecord.FTS_MESSAGE, search);

        return from(ForumThreadRecord.class).cacheRecords()
            .join(ForumThreadRecord.THREAD_ID, ForumMessageRecord.THREAD_ID)
            .where(ForumThreadRecord.GROUP_ID.in(groupIds),
                Ops.or(ftsSubject.match(), ftsMessage.match()))
            .limit(offset, count)
            .descending(ftsSubject.rank().times(5).plus(ftsMessage.rank()))
            .select();
    }

    /**
     * Loads the specified range of forum messages for the specified thread. Ordered by posting
     * date.
     */
    public List<ForumMessageRecord> loadMessages (int threadId, int offset, int count)
    {
        return from(ForumMessageRecord.class)
            .where(ForumMessageRecord.THREAD_ID, threadId)
            .limit(offset, count)
            .ascending(ForumMessageRecord.CREATED)
            .select();
    }

    /**
     * Searches the messages in a particular thread for a search string.
     */
    public List<ForumMessageRecord> findMessages (
        int threadId, String search, int offset, int count)
    {
        FullText ftsMessage = new FullText(ForumMessageRecord.class,
            ForumMessageRecord.FTS_MESSAGE, search);
        return from(ForumMessageRecord.class)
            .where(ForumMessageRecord.THREAD_ID.eq(threadId), ftsMessage.match())
            .descending(ftsMessage.rank())
            .limit(offset, count)
            .select();
    }

    /**
     * Loads up a map of message id to the index of the message within the containing thread.
     */
    public IntIntMap loadMessageIds (int threadId)
    {
        List<Key<ForumMessageRecord>> keys = from(ForumMessageRecord.class)
            .where(ForumMessageRecord.THREAD_ID.eq(threadId))
            .ascending(ForumMessageRecord.CREATED)
            .selectKeys(false);

        IntIntMap idToIndex = new IntIntMap();
        int index = 0;
        for (Key<ForumMessageRecord> key : keys) {
            idToIndex.put(ForumMessageRecord.extractMessageId(key), index++);
        }
        return idToIndex;
    }

    /**
     * Loads the specified forum thread record. Returns null if no record exists for that id.
     */
    public ForumThreadRecord loadThread (int threadId)
    {
        return load(ForumThreadRecord.getKey(threadId));
    }

    /**
     * Creates a thread for the specified group by the specified author with the specified subject
     * and first message content.
     *
     * @return the newly created thread's record.
     */
    public ForumThreadRecord createThread (int groupId, int creatorId, int flags,
                                           String subject, String message)
    {
        // create a record for the thread
        ForumThreadRecord ftr = new ForumThreadRecord();
        ftr.groupId = groupId;
        ftr.flags = flags;
        ftr.subject = subject;
        ftr.sticky = (flags & ForumThread.FLAG_STICKY) != 0;
        ftr.mostRecentPostTime = new Timestamp(System.currentTimeMillis()); // must be non-null
        insert(ftr);

        try {
            // post the first message to the thread (this will update the thread's last posted info)
            postMessage(ftr, creatorId, 0, message);
            return ftr;

        } catch (DatabaseException e) {
            try {
                delete(ftr);
            } catch (DatabaseException e2) {
                log.warning("Failed to roll back thread insert " + ftr + ": " + e2 + ".");
            }
            throw e;
        }
    }

    /**
     * Updates the flags and subject of the specified thread.
     */
    public void updateThread (int threadId, int flags, String subject)
    {
        updatePartial(ForumThreadRecord.getKey(threadId),
                      ForumThreadRecord.SUBJECT, subject,
                      ForumThreadRecord.FLAGS, flags,
                      ForumThreadRecord.STICKY, (flags & ForumThread.FLAG_STICKY) != 0);
    }

    /**
     * Posts a message to the specified thread.
     */
    public ForumMessageRecord postMessage (
        ForumThreadRecord thread, int posterId, int inReplyTo, String message)
    {
        // insert a record in the database for the message
        ForumMessageRecord fmr = new ForumMessageRecord();
        fmr.threadId = thread.threadId;
        fmr.posterId = posterId;
        fmr.inReplyTo = inReplyTo;
        fmr.created = fmr.lastEdited = new Timestamp(System.currentTimeMillis());
        fmr.message = message;
        insert(fmr);

        // update the post count and last post information for the thread
        updatePartial(ForumThreadRecord.getKey(thread.threadId),
                      ForumThreadRecord.MOST_RECENT_POST_ID, fmr.messageId,
                      ForumThreadRecord.MOST_RECENT_POST_TIME, fmr.created,
                      ForumThreadRecord.MOST_RECENT_POSTER_ID, posterId);

        updatePartial(ForumThreadRecord.getKey(thread.threadId),
                      ForumThreadRecord.POSTS, ForumThreadRecord.POSTS.plus(1));

        // update thread object to match its persistent record
        thread.posts++;
        thread.mostRecentPostId = fmr.messageId;
        thread.mostRecentPostTime = fmr.created;
        thread.mostRecentPosterId = posterId;

        return fmr;
    }

    /**
     * Loads the specified message record. Returns null if no record exists for that id.
     */
    public ForumMessageRecord loadMessage (int messageId)
    {
        return load(ForumMessageRecord.getKey(messageId));
    }

    /**
     * Loads all the message records that are associated with this issueId.
     */
    public List<ForumMessageRecord> loadIssueMessages (int issueId)
    {
        return findAll(ForumMessageRecord.class,
                       new Where(ForumMessageRecord.ISSUE_ID, issueId));
    }

    /**
     * Updates the text of the supplied message.
     *
     * @return the newly assigned last edited timestamp.
     */
    public Timestamp updateMessage (int messageId, String message)
    {
        Timestamp lastEdited = new Timestamp(System.currentTimeMillis());
        updatePartial(ForumMessageRecord.getKey(messageId),
                      ForumMessageRecord.LAST_EDITED, lastEdited,
                      ForumMessageRecord.MESSAGE, message);
        return lastEdited;
    }

    /**
     * Updates the issueid of the supplied message.
     */
    public void updateMessageIssue (int messageId, int issueId)
    {
        updatePartial(ForumMessageRecord.getKey(messageId), ForumMessageRecord.ISSUE_ID, issueId);
    }

    /**
     * Deletes the specified message.
     */
    public void deleteMessage (int messageId)
    {
        ForumMessageRecord fmr = loadMessage(messageId);
        if (fmr == null || delete(ForumMessageRecord.getKey(messageId)) == 0) {
            return;
        }

        ForumThreadRecord ftr = loadThread(fmr.threadId);
        if (ftr == null) {
            return; // oh well, nothing to worry about
        }

        // if this was the only post in the thread, delete the thread
        if (ftr.posts == 1) {
            delete(ForumThreadRecord.getKey(ftr.threadId));
            return;
        }

        // otherwise decrement the post count
        Map<ColumnExp<?>, SQLExpression<?>> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, ForumThreadRecord.POSTS.minus(1));
        // and update the last post/poster/etc. if we just deleted the last post
        if (ftr.mostRecentPostId == fmr.messageId) {
            List<ForumMessageRecord> lastMsg = findAll(
                ForumMessageRecord.class,
                new Where(ForumMessageRecord.THREAD_ID, ftr.threadId),
                new Limit(0, 1),
                OrderBy.descending(ForumMessageRecord.CREATED));
            if (lastMsg.size() > 0) {
                ForumMessageRecord last = lastMsg.get(0);
                updates.put(ForumThreadRecord.MOST_RECENT_POST_ID, Exps.value(last.messageId));
                updates.put(ForumThreadRecord.MOST_RECENT_POSTER_ID, Exps.value(last.posterId));
                updates.put(ForumThreadRecord.MOST_RECENT_POST_TIME, Exps.value(last.created));
            }
        }
        updatePartial(ForumThreadRecord.getKey(fmr.threadId), updates);
    }

    /**
     * Loads up the last read post information for the specified member and threads.
     */
    public List<ReadTrackingRecord> loadLastReadPostInfo (int memberId, Set<Integer> threadIds)
    {
        return findAll(ReadTrackingRecord.class,
                       new Where(Ops.and(ReadTrackingRecord.MEMBER_ID.eq(memberId),
                                         ReadTrackingRecord.THREAD_ID.in(threadIds))));
    }

    /**
     * Notes this member's most recently read post for the specified thread.
     */
    public void noteLastReadPostId (int memberId, int threadId,
                                    int lastReadPostId, int lastReadPostIndex)
    {
        ReadTrackingRecord record = new ReadTrackingRecord();
        record.memberId = memberId;
        record.threadId = threadId;
        record.lastReadPostId = lastReadPostId;
        record.lastReadPostIndex = lastReadPostIndex;
        store(record);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts. NOTE: members' posts and threads are not deleted. Those live on into
     * glorious eternity.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(ReadTrackingRecord.class,
                  new Where(ReadTrackingRecord.MEMBER_ID.in(memberIds)));
    }

    protected Query<ForumThreadRecord> forUnreadThreads (
        Query<ForumThreadRecord> builder, int memberId, Set<Integer> groupIds)
    {
        Join join = new Join(ReadTrackingRecord.class, Ops.and(
            ForumThreadRecord.THREAD_ID.eq(ReadTrackingRecord.THREAD_ID),
            ReadTrackingRecord.MEMBER_ID.eq(memberId)));
        join.setType(Join.Type.LEFT_OUTER);

        return builder.join(join)
            .where(Ops.and(
                ForumThreadRecord.GROUP_ID.in(groupIds),
                ForumThreadRecord.MOST_RECENT_POST_TIME.greaterThan(
                    RepositoryUtil.getCutoff(UNREAD_POSTS_CUTOFF)),
                Ops.or(ReadTrackingRecord.THREAD_ID.isNull(),
                    Ops.and(ReadTrackingRecord.MEMBER_ID.eq(memberId),
                        ForumThreadRecord.MOST_RECENT_POST_ID.greaterThan(
                            ReadTrackingRecord.LAST_READ_POST_ID)))));
    }

    protected Query<ForumThreadRecord> forUnreadFriendThreads (
        Query<ForumThreadRecord> builder, int memberId, Set<Integer> authorIds,
        Set<Integer> hiddenGroupIds)
    {
        Join joinRead = new Join(ReadTrackingRecord.class, Ops.and(
            ForumThreadRecord.THREAD_ID.eq(ReadTrackingRecord.THREAD_ID),
            ReadTrackingRecord.MEMBER_ID.eq(memberId)));
        joinRead.setType(Join.Type.LEFT_OUTER);

        Join joinThread = new Join(ForumMessageRecord.class,
            ForumThreadRecord.THREAD_ID.eq(ForumMessageRecord.THREAD_ID));
        joinThread.setType(Join.Type.INNER);

        builder.join(joinRead).join(joinThread);

        // filtering expressions
        List<SQLExpression<?>> conditions = Lists.newArrayListWithCapacity(4);
        conditions.add(ForumMessageRecord.POSTER_ID.in(authorIds));
        conditions.add(Ops.or(ReadTrackingRecord.THREAD_ID.isNull(),
                              Ops.and(ReadTrackingRecord.MEMBER_ID.eq(memberId),
                                      ForumMessageRecord.MESSAGE_ID.greaterThan(
                                          ReadTrackingRecord.LAST_READ_POST_ID))));
        if (hiddenGroupIds.size() > 0) { // no empty In's
            conditions.add(Ops.not(ForumThreadRecord.GROUP_ID.in(hiddenGroupIds)));
        }
        conditions.add(ForumMessageRecord.CREATED.greaterThan(
                           RepositoryUtil.getCutoff(UNREAD_POSTS_CUTOFF)));

        return builder.where(conditions)
            .groupBy(ForumThreadRecord.THREAD_ID, ForumThreadRecord.MOST_RECENT_POST_ID);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ForumThreadRecord.class);
        classes.add(ForumMessageRecord.class);
        classes.add(ReadTrackingRecord.class);
    }

    /** We don't consider unread posts older than this many days. */
    protected static final int UNREAD_POSTS_CUTOFF = 21;
}
