//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.SQLExpression;
import com.samskivert.jdbc.depot.operator.Arithmetic.*;
import com.samskivert.jdbc.depot.operator.Conditionals.*;
import com.samskivert.jdbc.depot.operator.Logic.*;

import com.threerings.msoy.fora.data.ForumThread;

/**
 * Manages forum threads and messages.
 */
public class ForumRepository extends DepotRepository
{
    /** Used by {@link #loadThreadCount}. */
    @Entity @Computed
    public static class ThreadCountRecord extends PersistentRecord
    {
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    public ForumRepository (PersistenceContext ctx)
    {
        super(ctx);
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
     * Loads the total number of threads in the specified group.
     */
    public int loadThreadCount (int groupId)
        throws PersistenceException
    {
        return load(ThreadCountRecord.class,
                    new FromOverride(ForumThreadRecord.class),
                    new Where(ForumThreadRecord.GROUP_ID_C, groupId)).count;
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

        // post the first message to the thread (this will update the thread's last posted info)
        ForumMessageRecord fmr = postMessage(ftr.threadId, creatorId, 0, message);

        // fill the last post values into the thread record by hand so that we can return it
        ftr.mostRecentPostId = fmr.messageId;
        ftr.mostRecentPostTime = fmr.created;
        ftr.mostRecentPosterId = creatorId;
        ftr.posts = 1;

        return ftr;
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
    public ForumMessageRecord postMessage (int threadId, int posterId, int inReplyTo, String message)
        throws PersistenceException
    {
        // insert a record in the database for the message
        ForumMessageRecord fmr = new ForumMessageRecord();
        fmr.threadId = threadId;
        fmr.posterId = posterId;
        fmr.inReplyTo = inReplyTo;
        fmr.created = fmr.lastEdited = new Timestamp(System.currentTimeMillis());
        fmr.message = message;
        insert(fmr);

        // update the post count and last post information for the thread
        updatePartial(ForumThreadRecord.class, threadId,
                      ForumThreadRecord.MOST_RECENT_POST_ID, fmr.messageId,
                      ForumThreadRecord.MOST_RECENT_POST_TIME, fmr.created,
                      ForumThreadRecord.MOST_RECENT_POSTER_ID, posterId);

        Map<String, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Add(ForumThreadRecord.POSTS_C, 1));
        updateLiteral(ForumThreadRecord.class, threadId, updates);

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
     * Deletes the specified message.
     */
    public void deleteMessage (int messageId)
        throws PersistenceException
    {
        ForumMessageRecord fmr = loadMessage(messageId);
        if (fmr == null || delete(ForumMessageRecord.class, messageId) == 0) {
            return;
        }

        Map<String, SQLExpression> updates = Maps.newHashMap();
        updates.put(ForumThreadRecord.POSTS, new Sub(ForumThreadRecord.POSTS_C, 1));
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
