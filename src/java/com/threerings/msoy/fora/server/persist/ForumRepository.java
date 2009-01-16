//
// $Id$

package com.threerings.msoy.fora.server.persist;

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
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.expression.ValueExp;
import com.samskivert.depot.operator.Arithmetic.*;
import com.samskivert.depot.operator.Conditionals.*;
import com.samskivert.depot.operator.Logic.*;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.server.persist.CountRecord;

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
    public List<ForumThreadRecord> loadUnreadThreads (int memberId, Set<Integer> groupIds, int max)
    {
        SQLExpression join = new And(
            new Equals(ForumThreadRecord.THREAD_ID, ReadTrackingRecord.THREAD_ID),
            new Equals(ReadTrackingRecord.MEMBER_ID, memberId)
        );
        SQLExpression where = new And(
            new In(ForumThreadRecord.GROUP_ID, groupIds),
            new Or(new IsNull(ReadTrackingRecord.THREAD_ID),
                   new And(new Equals(ReadTrackingRecord.MEMBER_ID, memberId),
                           new GreaterThan(ForumThreadRecord.MOST_RECENT_POST_ID,
                                           ReadTrackingRecord.LAST_READ_POST_ID))));

        // consult the cache for records, but not for the keyset
        return findAll(ForumThreadRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
            new Join(ReadTrackingRecord.class, join).setType(Join.Type.LEFT_OUTER),
            new Where(where),
            new Limit(0, max),
            OrderBy.descending(ForumThreadRecord.MOST_RECENT_POST_ID)));
    }

    /**
     * Loads the latest threads for the specified group. Ordered by threadId (ie: creation time).
     */
    public List<ForumThreadRecord> loadRecentThreads (int groupId, int count)
    {
        return findAll(ForumThreadRecord.class,
                       new Where(ForumThreadRecord.GROUP_ID, groupId),
                       new Limit(0, count),
                       OrderBy.descending(ForumThreadRecord.THREAD_ID));
    }

    /**
     * Loads the total number of threads in the specified group.
     */
    public int loadThreadCount (int groupId)
    {
        return load(CountRecord.class,
                    new FromOverride(ForumThreadRecord.class),
                    new Where(ForumThreadRecord.GROUP_ID, groupId)).count;
    }

    /**
     * Loads the total number of threads in the specified group.
     */
    public int loadMessageCount (int groupId)
    {
        return load(CountRecord.class,
            new FromOverride(ForumThreadRecord.class, ForumMessageRecord.class),
            new Where(new And(
                new Equals(ForumThreadRecord.GROUP_ID, groupId),
                new Equals(ForumThreadRecord.THREAD_ID, ForumMessageRecord.THREAD_ID)))
            ).count;
    }

    /**
     * Loads the specified range of forum threads for the specified group. Ordered first stickyness
     * and then by most recently updated (newer first).
     */
    public List<ForumThreadRecord> loadThreads (int groupId, int offset, int count)
    {
        return findAll(ForumThreadRecord.class,
                       new Where(ForumThreadRecord.GROUP_ID, groupId),
                       new Limit(offset, count),
                       new OrderBy(
                           new SQLExpression[] { ForumThreadRecord.STICKY,
                                                 ForumThreadRecord.MOST_RECENT_POST_ID },
                           new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.DESC }));
    }

    /**
     * Finds all threads that match the specified search in their subject or for which one or more
     * of their messages matches the supplied search.
     */
    public List<ForumThreadRecord> findThreads (int groupId, String search, int limit)
    {
        And where = new And(new Equals(ForumThreadRecord.GROUP_ID, groupId),
                            new Or(new FullTextMatch(ForumThreadRecord.class,
                                                     ForumThreadRecord.FTS_SUBJECT, search),
                                   new FullTextMatch(ForumMessageRecord.class,
                                                     ForumMessageRecord.FTS_MESSAGE, search)));
        return findAll(ForumThreadRecord.class,
                       new Join(ForumThreadRecord.THREAD_ID, ForumMessageRecord.THREAD_ID),
                       new Where(where), new Limit(0, limit));
    }

    /**
     * Finds all unread threads in a list of groups that match the specified search in their
     * subject or for which one or more of their messages matches the supplied search.
     */
    public List<ForumThreadRecord> findUnreadThreads (int memberId, Set<Integer> groupIds,
        String search, int limit)
    {
        SQLExpression cacheJoinAnd = new And(
            new Equals(ForumThreadRecord.THREAD_ID, ReadTrackingRecord.THREAD_ID),
            new Equals(ReadTrackingRecord.MEMBER_ID, memberId)
        );
        SQLExpression where = new And(
            new In(ForumThreadRecord.GROUP_ID, groupIds),
            new Or(new FullTextMatch(ForumThreadRecord.class,
                    ForumThreadRecord.FTS_SUBJECT, search),
                new FullTextMatch(ForumMessageRecord.class,
                    ForumMessageRecord.FTS_MESSAGE, search)),
            new Or(new IsNull(ReadTrackingRecord.THREAD_ID),
                   new And(new Equals(ReadTrackingRecord.MEMBER_ID, memberId),
                           new GreaterThan(ForumThreadRecord.MOST_RECENT_POST_ID,
                                           ReadTrackingRecord.LAST_READ_POST_ID))));

        // consult the cache for records, but not for the keyset
        return findAll(ForumThreadRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
            new Join(ForumThreadRecord.THREAD_ID, ForumMessageRecord.THREAD_ID),
            new Join(ReadTrackingRecord.class, cacheJoinAnd).setType(Join.Type.LEFT_OUTER),
            new Where(where),
            new Limit(0, limit)));
    }

    /**
     * Loads the specified range of forum messages for the specified thread. Ordered by posting
     * date.
     */
    public List<ForumMessageRecord> loadMessages (int threadId, int offset, int count)
    {
        return findAll(ForumMessageRecord.class,
                       new Where(ForumMessageRecord.THREAD_ID, threadId),
                       new Limit(offset, count),
                       OrderBy.ascending(ForumMessageRecord.CREATED));
    }

    /**
     * Searches the messages in a particular thread for a search string.
     */
    public List<ForumMessageRecord> findMessages (int threadId, String search, int limit)
    {
        And where = new And(new Equals(ForumMessageRecord.THREAD_ID, threadId),
                            new FullTextMatch(ForumMessageRecord.class,
                                              ForumMessageRecord.FTS_MESSAGE, search));
        return findAll(ForumMessageRecord.class, new Where(where), new Limit(0, limit));
    }

    /**
     * Loads the specified forum thread record. Returns null if no record exists for that id.
     */
    public ForumThreadRecord loadThread (int threadId)
    {
        return load(ForumThreadRecord.class, threadId);
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
     * Updates the flags of the specified thread.
     */
    public void updateThreadFlags (int threadId, int flags)
    {
        updatePartial(ForumThreadRecord.class, threadId,
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
        updatePartial(ForumThreadRecord.class, thread.threadId,
                      ForumThreadRecord.MOST_RECENT_POST_ID, fmr.messageId,
                      ForumThreadRecord.MOST_RECENT_POST_TIME, fmr.created,
                      ForumThreadRecord.MOST_RECENT_POSTER_ID, posterId);

        Map<ColumnExp, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Add(ForumThreadRecord.POSTS, 1));
        updateLiteral(ForumThreadRecord.class, thread.threadId, updates);

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
        return load(ForumMessageRecord.class, messageId);
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
        updatePartial(ForumMessageRecord.class, messageId,
                      ForumMessageRecord.LAST_EDITED, lastEdited,
                      ForumMessageRecord.MESSAGE, message);
        return lastEdited;
    }

    /**
     * Updates the issueid of the supplied message.
     */
    public void updateMessageIssue (int messageId, int issueId)
    {
        updatePartial(ForumMessageRecord.class, messageId,
                      ForumMessageRecord.ISSUE_ID, issueId);
    }

    /**
     * Deletes the specified message.
     */
    public void deleteMessage (int messageId)
    {
        ForumMessageRecord fmr = loadMessage(messageId);
        if (fmr == null || delete(ForumMessageRecord.class, messageId) == 0) {
            return;
        }

        ForumThreadRecord ftr = loadThread(fmr.threadId);
        if (ftr == null) {
            return; // oh well, nothing to worry about
        }

        // if this was the only post in the thread, delete the thread
        if (ftr.posts == 1) {
            delete(ForumThreadRecord.class, ftr.threadId);
            return;
        }

        // otherwise decrement the post count
        Map<ColumnExp, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Sub(ForumThreadRecord.POSTS, 1));
        // and update the last post/poster/etc. if we just deleted the last post
        if (ftr.mostRecentPostId == fmr.messageId) {
            List<ForumMessageRecord> lastMsg = findAll(
                ForumMessageRecord.class,
                new Where(ForumMessageRecord.THREAD_ID, ftr.threadId),
                new Limit(0, 1),
                OrderBy.descending(ForumMessageRecord.CREATED));
            if (lastMsg.size() > 0) {
                ForumMessageRecord last = lastMsg.get(0);
                updates.put(ForumThreadRecord.MOST_RECENT_POST_ID, new ValueExp(last.messageId));
                updates.put(ForumThreadRecord.MOST_RECENT_POSTER_ID, new ValueExp(last.posterId));
                updates.put(ForumThreadRecord.MOST_RECENT_POST_TIME, new ValueExp(last.created));
            }
        }
        updateLiteral(ForumThreadRecord.class, fmr.threadId, updates);
    }

    /**
     * Loads up the last read post information for the specified member and threads.
     */
    public List<ReadTrackingRecord> loadLastReadPostInfo (int memberId, Set<Integer> threadIds)
    {
        return findAll(ReadTrackingRecord.class,
                       new Where(new And(new Equals(ReadTrackingRecord.MEMBER_ID, memberId),
                                         new In(ReadTrackingRecord.THREAD_ID, threadIds))));
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ForumThreadRecord.class);
        classes.add(ForumMessageRecord.class);
        classes.add(ReadTrackingRecord.class);
    }
}
