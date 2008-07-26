//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.expression.ValueExp;
import com.samskivert.jdbc.depot.operator.Arithmetic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;

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
    }

    /**
     * Loads up to the specified maximum number of threads from the supplied set of groups that
     * have messages that are unread by the specified member. The results are ordered from newest
     * to oldest.
     */
    public List<ForumThreadRecord> loadUnreadThreads (
        int memberId, Set<Integer> groupIds, int maximum)
        throws PersistenceException
    {
        SQLExpression join = new And(
            new Equals(ForumThreadRecord.THREAD_ID_C, ReadTrackingRecord.THREAD_ID_C),
            new Equals(ReadTrackingRecord.MEMBER_ID_C, memberId)
        );
        SQLExpression where = new And(
            new In(ForumThreadRecord.GROUP_ID_C, groupIds),
            new Or(new IsNull(ReadTrackingRecord.THREAD_ID_C),
                   new And(new Equals(ReadTrackingRecord.MEMBER_ID_C, memberId),
                           new GreaterThan(ForumThreadRecord.MOST_RECENT_POST_ID_C,
                                           ReadTrackingRecord.LAST_READ_POST_ID_C))));
        return findAll(ForumThreadRecord.class,
                       new Join(ReadTrackingRecord.class, join).setType(Join.Type.LEFT_OUTER),
                       new Where(where),
                       new Limit(0, maximum),
                       OrderBy.descending(ForumThreadRecord.MOST_RECENT_POST_ID_C));
    }

    /**
     * Loads the latest threads for the specified group. Ordered by threadId (ie: creation time).
     */
    public List<ForumThreadRecord> loadRecentThreads (int groupId, int count)
        throws PersistenceException
    {
        return findAll(ForumThreadRecord.class,
                       new Where(ForumThreadRecord.GROUP_ID_C, groupId),
                       new Limit(0, count),
                       OrderBy.descending(ForumThreadRecord.THREAD_ID_C));
    }

    /**
     * Loads the total number of threads in the specified group.
     */
    public int loadThreadCount (int groupId)
        throws PersistenceException
    {
        return load(CountRecord.class,
                    new FromOverride(ForumThreadRecord.class),
                    new Where(ForumThreadRecord.GROUP_ID_C, groupId)).count;
    }

    /**
     * Loads the total number of threads in the specified group.
     */
    public int loadMessageCount (int groupId)
        throws PersistenceException
    {
        return load(CountRecord.class,
            new FromOverride(ForumThreadRecord.class, ForumMessageRecord.class),
            new Where(new And(
                new Equals(ForumThreadRecord.GROUP_ID_C, groupId),
                new Equals(ForumThreadRecord.THREAD_ID_C, ForumMessageRecord.THREAD_ID_C)))
            ).count;
    }

    /**
     * Loads the specified range of forum threads for the specified group. Ordered first stickyness
     * and then by most recently updated (newer first).
     */
    public List<ForumThreadRecord> loadThreads (int groupId, int offset, int count)
        throws PersistenceException
    {
        return findAll(ForumThreadRecord.class,
                       new Where(ForumThreadRecord.GROUP_ID_C, groupId),
                       new Limit(offset, count),
                       new OrderBy(
                           new SQLExpression[] { ForumThreadRecord.STICKY_C,
                                                 ForumThreadRecord.MOST_RECENT_POST_ID_C },
                           new OrderBy.Order[] { OrderBy.Order.DESC, OrderBy.Order.DESC }));
    }

    /**
     * Finds all threads that match the specified search in their subject or for which one or more
     * of their messages matches the supplied search.
     */
    public List<ForumThreadRecord> findThreads (int groupId, String search, int limit)
        throws PersistenceException
    {
        And where = new And(new Equals(ForumThreadRecord.GROUP_ID_C, groupId),
                            new Or(new FullTextMatch(ForumThreadRecord.class,
                                                     ForumThreadRecord.FTS_SUBJECT, search),
                                   new FullTextMatch(ForumMessageRecord.class,
                                                     ForumMessageRecord.FTS_MESSAGE, search)));
        return findAll(ForumThreadRecord.class,
                       new Join(ForumThreadRecord.THREAD_ID_C, ForumMessageRecord.THREAD_ID_C),
                       new Where(where), new Limit(0, limit));
    }

    /**
     * Loads the specified range of forum messages for the specified thread. Ordered by posting
     * date.
     */
    public List<ForumMessageRecord> loadMessages (int threadId, int offset, int count)
        throws PersistenceException
    {
        return findAll(ForumMessageRecord.class,
                       new Where(ForumMessageRecord.THREAD_ID_C, threadId),
                       new Limit(offset, count),
                       OrderBy.ascending(ForumMessageRecord.CREATED_C));
    }

    /**
     * Searches the messages in a particular thread for a search string.
     */
    public List<ForumMessageRecord> findMessages (int threadId, String search, int limit)
        throws PersistenceException
    {
        And where = new And(new Equals(ForumMessageRecord.THREAD_ID_C, threadId),
                            new FullTextMatch(ForumMessageRecord.class,
                                              ForumMessageRecord.FTS_MESSAGE, search));
        return findAll(ForumMessageRecord.class, new Where(where), new Limit(0, limit));
    }

    /**
     * Loads the specified forum thread record. Returns null if no record exists for that id.
     */
    public ForumThreadRecord loadThread (int threadId)
        throws PersistenceException
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
        throws PersistenceException
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

        } catch (PersistenceException pe) {
            try {
                delete(ftr);
            } catch (PersistenceException pe2) {
                log.warning("Failed to roll back thread insert " + ftr + ": " + pe2 + ".");
            }
            throw pe;
        }
    }

    /**
     * Updates the flags of the specified thread.
     */
    public void updateThreadFlags (int threadId, int flags)
        throws PersistenceException
    {
        updatePartial(ForumThreadRecord.class, threadId,
                      ForumThreadRecord.FLAGS, flags,
                      ForumThreadRecord.STICKY, (flags & ForumThread.FLAG_STICKY) != 0);
    }

    /**
     * Posts a message to the specified thread.
     */
    public ForumMessageRecord postMessage (ForumThreadRecord thread, int posterId, int inReplyTo, String message)
        throws PersistenceException
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

        Map<String, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Add(ForumThreadRecord.POSTS_C, 1));
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
        throws PersistenceException
    {
        return load(ForumMessageRecord.class, messageId);
    }

    /**
     * Loads all the message records that are associated with this issueId.
     */
    public List<ForumMessageRecord> loadIssueMessages (int issueId)
        throws PersistenceException
    {
        return findAll(ForumMessageRecord.class,
                       new Where(ForumMessageRecord.ISSUE_ID_C, issueId));
    }

    /**
     * Updates the text of the supplied message.
     */
    public void updateMessage (int messageId, String message)
        throws PersistenceException
    {
        updatePartial(ForumMessageRecord.class, messageId,
                      ForumMessageRecord.LAST_EDITED, new Timestamp(System.currentTimeMillis()),
                      ForumMessageRecord.MESSAGE, message);
    }

    /**
     * Updates the issueid of the supplied message.
     */
    public void updateMessageIssue (int messageId, int issueId)
        throws PersistenceException
    {
        updatePartial(ForumMessageRecord.class, messageId,
                      ForumMessageRecord.ISSUE_ID, issueId);
    }

    /**
     * Deletes the specified message.
     */
    public void deleteMessage (int messageId)
        throws PersistenceException
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
        Map<String, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Sub(ForumThreadRecord.POSTS_C, 1));
        // and update the last post/poster/etc. if we just deleted the last post
        if (ftr.mostRecentPostId == fmr.messageId) {
            List<ForumMessageRecord> lastMsg = findAll(
                ForumMessageRecord.class,
                new Where(ForumMessageRecord.THREAD_ID_C, ftr.threadId),
                new Limit(0, 1),
                OrderBy.descending(ForumMessageRecord.CREATED_C));
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
        throws PersistenceException
    {
        return findAll(ReadTrackingRecord.class,
                       new Where(new And(new Equals(ReadTrackingRecord.MEMBER_ID_C, memberId),
                                         new In(ReadTrackingRecord.THREAD_ID_C, threadIds))));
    }

    /**
     * Notes this member's most recently read post for the specified thread.
     */
    public void noteLastReadPostId (int memberId, int threadId,
                                    int lastReadPostId, int lastReadPostIndex)
        throws PersistenceException
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
