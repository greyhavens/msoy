//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic;
import com.samskivert.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.util.FeedMessageType;

/**
 * Maintains persistent data for feeds.
 */
@Singleton @BlockingThread
public class FeedRepository extends DepotRepository
{
    @Computed @Entity
    public static class FeedMessageCount extends PersistentRecord {
        @Computed(fieldDefinition="count(*)")
        public int count;
    }

    @Inject public FeedRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all applicable personal feed messages for the specified member.
     *
     * @param since an approximate timestamp before which not to load messages.
     */
    public void loadPersonalFeed (int memberId, List<FeedMessageRecord> messages,
        Collection<Integer> friendIds, long since)
    {
        // round "since" to the nearest 2^18 milliseconds (~4 hours) for cachability
        since &= ~0x3FFFFL;
        SQLOperator self = new Conditionals.Equals(SelfFeedMessageRecord.TARGET_ID, memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, since);
        if (!friendIds.isEmpty()) {
            SQLOperator actors = null;
            actors = new Conditionals.In(FriendFeedMessageRecord.ACTOR_ID, friendIds);
            loadFeedMessages(messages, FriendFeedMessageRecord.class, actors, since);
        }

        // include actions the member has performed
        SQLOperator actor = new Conditionals.Equals(FriendFeedMessageRecord.ACTOR_ID, memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, since);
    }

    /**
     * Loads all global and group feed messages for the given groups.
     *
     * @param since an approximate timestamp before which not to load messages.
     */
    public void loadGroupFeeds (List<FeedMessageRecord> messages,
        Collection<Integer> groupIds, long since)
    {
        // round "since" to the nearest 2^18 milliseconds (~4 hours) for cachability
        since &= ~0x3FFFFL;
        loadFeedMessages(messages, GlobalFeedMessageRecord.class, null, since);
        if (!groupIds.isEmpty()) {
            SQLOperator groups = null;
            groups = new Conditionals.In(GroupFeedMessageRecord.GROUP_ID, groupIds);
            loadFeedMessages(messages, GroupFeedMessageRecord.class, groups, since);
        }
    }

    /**
     * Loads all applicable feed messages by the specified member.
     *
     * @param since an approximate timestamp before which not to load messages.
     */
    public List <FeedMessageRecord> loadMemberFeed (int memberId, long since)
    {
        // round "since" to the nearest 2^16 milliseconds (~1 hour) for cachability
        since &= ~0xFFFFL;
        List<FeedMessageRecord> messages = Lists.newArrayList();
        SQLOperator actor = new Conditionals.Equals(FriendFeedMessageRecord.ACTOR_ID, memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, since);
        SQLOperator self = new Conditionals.Equals(SelfFeedMessageRecord.TARGET_ID, memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, since);
        return messages;
    }

    /**
     * Publishes a global message which will show up in all users' feeds. Note: global messages are
     * never throttled.
     */
    public void publishGlobalMessage (FeedMessageType type, String data)
    {
        GlobalFeedMessageRecord message = new GlobalFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
    }

    /**
     * Publishes a self feed message, that will show up on the target's profile.  These are
     * currently not throttled.
     */
    public void publishSelfMessage (int targetId, int actorId, FeedMessageType type, String data)
    {
        SelfFeedMessageRecord message = new SelfFeedMessageRecord();
        message.targetId = targetId;
        message.actorId = actorId;
        message.type = type.getCode();
        message.data = data;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
    }

    /**
     * Publishes a feed message to the specified actor's friends.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, String data)
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle =
                new Timestamp(System.currentTimeMillis() - type.getThrottlePeriod());
            List<SQLOperator> bits = Lists.newArrayList();
            bits.add(new Conditionals.Equals(FriendFeedMessageRecord.ACTOR_ID, actorId));
            bits.add(new Conditionals.Equals(FriendFeedMessageRecord.TYPE, type.getCode()));
            bits.add(new Conditionals.GreaterThan(FriendFeedMessageRecord.POSTED, throttle));

            FeedMessageCount count = load(FeedMessageCount.class,
                    new FromOverride(FriendFeedMessageRecord.class),
                    new Where(new Logic.And(bits)));

            if (count.count >= type.getThrottleCount()) {
                return false;
            }
        }

        FriendFeedMessageRecord message = new FriendFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.actorId = actorId;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
        return true;
    }

    /**
     * Publishes a feed message to the specified group's members.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, String data)
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle =
                new Timestamp(System.currentTimeMillis() - type.getThrottlePeriod());
            List<SQLOperator> bits = Lists.newArrayList();
            bits.add(new Conditionals.Equals(GroupFeedMessageRecord.GROUP_ID, groupId));
            bits.add(new Conditionals.Equals(GroupFeedMessageRecord.TYPE, type.getCode()));
            bits.add(new Conditionals.GreaterThan(GroupFeedMessageRecord.POSTED, throttle));

            FeedMessageCount count = load(FeedMessageCount.class,
                    new FromOverride(GroupFeedMessageRecord.class),
                    new Where(new Logic.And(bits)));

            if (count.count >= type.getThrottleCount()) {
                return false;
            }
        }

        GroupFeedMessageRecord message = new GroupFeedMessageRecord();
        message.type = type.getCode();
        message.data = data;
        message.groupId = groupId;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
        return true;
    }

    // TODO: call this method periodically on the server... I'm not immediately thinking of a good
    // way to do this; having the repository register an Interval at construct time seems sketchy
    /**
     * Prunes feed messages of all types that have expired.
     */
    public void pruneFeeds ()
    {
        final Timestamp cutoff = new Timestamp(System.currentTimeMillis() - FEED_EXPIRATION_PERIOD);
        deleteAll(GlobalFeedMessageRecord.class,
                  new Where(new Conditionals.LessThan(GlobalFeedMessageRecord.POSTED, cutoff)));
        deleteAll(FriendFeedMessageRecord.class,
                  new Where(new Conditionals.LessThan(FriendFeedMessageRecord.POSTED, cutoff)));
        deleteAll(GroupFeedMessageRecord.class,
                  new Where(new Conditionals.LessThan(GroupFeedMessageRecord.POSTED, cutoff)));
        deleteAll(SelfFeedMessageRecord.class,
                  new Where(new Conditionals.LessThan(SelfFeedMessageRecord.POSTED, cutoff)));
    }

    /**
     * Helper function for {@link #loadMemberFeed}.
     */
    protected void loadFeedMessages (List<FeedMessageRecord> messages,
                                     Class<? extends FeedMessageRecord> pClass,
                                     SQLOperator main, long sinceMillis)
    {
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (main != null) {
            whereBits.add(main);
        }
        Timestamp since = new Timestamp(sinceMillis);
        whereBits.add(new Conditionals.GreaterThanEquals(
                          new ColumnExp(pClass, FeedMessageRecord.POSTED.name), since));
        messages.addAll(findAll(pClass, new Where(new Logic.And(whereBits))));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(FriendFeedMessageRecord.class);
        classes.add(GroupFeedMessageRecord.class);
        classes.add(GlobalFeedMessageRecord.class);
        classes.add(SelfFeedMessageRecord.class);
    }

    /** Feed messages expire after two weeks. */
    protected static final long FEED_EXPIRATION_PERIOD = 14 * 24 * 60 * 60 * 1000L;
}
