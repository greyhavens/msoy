//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

/**
 * Manages forum threads and messages.
 */
public class ForumRepository extends DepotRepository
{
    public ForumRepository (PersistenceContext ctx)
    {
        super(ctx);
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
    public ForumThreadRecord createThread (int creatorId, int groupId, int flags,
                                           String subject, String message)
        throws PersistenceException
    {
        // create a record for the thread
        ForumThreadRecord ftr = new ForumThreadRecord();
        ftr.groupId = groupId;
        ftr.flags = flags;
        ftr.subject = subject;
        insert(ftr);

        // post the first message to the thread (this will update the thread's last posted info)
        ForumMessageRecord fmr = postMessage(ftr.threadId, creatorId, message);

        // fill the last post values into the thread record by hand so that we can return it
        ftr.mostRecentPostId = fmr.messageId;
        ftr.mostRecentPostTime = fmr.created;
        ftr.mostRecentPosterId = creatorId;

        return ftr;
    }

    /**
     * Posts a message to the specified thread.
     */
    public ForumMessageRecord postMessage (int threadId, int posterId, String message)
        throws PersistenceException
    {
        // insert a record in the database for the message
        ForumMessageRecord fmr = new ForumMessageRecord();
        fmr.threadId = threadId;
        fmr.posterId = posterId;
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
