//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

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

    /** Used by {@link #loadMessageCount}. */
    @Entity @Computed
    public static class MessageCountRecord extends PersistentRecord
    {
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    public ForumRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP
        _ctx.registerMigration(ForumThreadRecord.class, new EntityMigration.Retype(2, "threadId"));
        _ctx.registerMigration(ForumMessageRecord.class, new EntityMigration.Retype(2, "messageId"));
        // END TEMP
    }

    /**
     * Loads the specified range of forum threads for the specified group. Ordered most recently
     * updated first.
     */
    public List<ForumThreadRecord> loadThreads (int groupId, int offset, int count)
        throws PersistenceException
    {
        return findAll(ForumThreadRecord.class,
                       new Where(ForumThreadRecord.GROUP_ID_C, groupId),
                       new Limit(offset, count),
                       OrderBy.descending(ForumThreadRecord.MOST_RECENT_POST_TIME_C));
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
     * Loads the total number of messages in the specified thread.
     */
    public int loadMessageCount (int threadId)
        throws PersistenceException
    {
        return load(MessageCountRecord.class,
                    new FromOverride(ForumMessageRecord.class),
                    new Where(ForumMessageRecord.THREAD_ID_C, threadId)).count;
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
        ftr.mostRecentPostTime = new Timestamp(System.currentTimeMillis()); // must be non-null
        insert(ftr);

        // post the first message to the thread (this will update the thread's last posted info)
        ForumMessageRecord fmr = postMessage(ftr.threadId, creatorId, 0, message);

        // fill the last post values into the thread record by hand so that we can return it
        ftr.mostRecentPostId = fmr.messageId;
        ftr.mostRecentPostTime = fmr.created;
        ftr.mostRecentPosterId = creatorId;

        return ftr;
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

        // update the last post information for the thread
        updatePartial(ForumThreadRecord.class, threadId,
                      ForumThreadRecord.MOST_RECENT_POST_ID, fmr.messageId,
                      ForumThreadRecord.MOST_RECENT_POST_TIME, fmr.created,
                      ForumThreadRecord.MOST_RECENT_POSTER_ID, posterId);

        return fmr;
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(ForumThreadRecord.class);
        classes.add(ForumMessageRecord.class);
        classes.add(ReadTrackingRecord.class);
    }
}
