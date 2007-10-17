//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntSet;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals;

import com.threerings.msoy.person.util.FeedMessageType;

/**
 * Maintains persistent data for feeds.
 */
public class FeedRepository extends DepotRepository
{
    public FeedRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all applicable feed messages for the specified member.
     *
     * @param a timestamp before which not to load messages or null if all available messages
     * should be loaded.
     */
    public List<FeedMessageRecord> loadMemberFeed (
        int memberId, IntSet friendIds, IntSet groupIds, Timestamp since)
        throws PersistenceException
    {
        ArrayList<FeedMessageRecord> messages = new ArrayList<FeedMessageRecord>();
        messages.addAll(findAll(GlobalFeedMessageRecord.class));
        messages.addAll(findAll(FriendFeedMessageRecord.class,
                                new Where(new Conditionals.In(FriendFeedMessageRecord.ACTOR_ID_C,
                                                              friendIds))));
        messages.addAll(findAll(GroupFeedMessageRecord.class,
                                new Where(new Conditionals.In(GroupFeedMessageRecord.GROUP_ID_C,
                                                              groupIds))));
        return messages;
    }

    /**
     * Publishes a global message which will show up in all users' feeds. Note: global messages are
     * never throttled.
     */
    public void publishGlobalMessage (FeedMessageType type, String data)
        throws PersistenceException
    {
        // TODO: store the message
        throw new PersistenceException("stub");
    }

    /**
     * Publishes a feed message to the specified actor's friends.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, String data)
        throws PersistenceException
    {
        // TODO: check that the message does not exceed its throttle
        // TODO: store the message
        throw new PersistenceException("stub");
    }

    /**
     * Publishes a feed message to the specified group's members.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, String data)
        throws PersistenceException
    {
        // TODO: check that the message does not exceed its throttle
        // TODO: store the message
        throw new PersistenceException("stub");
    }

    // TODO: call this method periodically on the server... I'm not immediately thinking of a good
    // way to do this; having the repository register an Interval at construct time seems sketchy
    /**
     * Prunes feed messages of all types that have expired.
     */
    public void pruneFeeds ()
        throws PersistenceException
    {
        // TODO: delete records older than FEED_EXPIRATION_PERIOD in all tables
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FriendFeedMessageRecord.class);
        classes.add(GroupFeedMessageRecord.class);
        classes.add(GlobalFeedMessageRecord.class);
    }

    /** Feed messages expire after two weeks. */
    protected static final long FEED_EXPIRATION_PERIOD = 14 * 24 * 60 * 60 * 1000L;
}
