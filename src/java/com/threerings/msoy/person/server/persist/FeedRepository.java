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
import com.samskivert.depot.operator.And;
import com.samskivert.depot.operator.Equals;
import com.samskivert.depot.operator.GreaterThan;
import com.samskivert.depot.operator.GreaterThanEquals;
import com.samskivert.depot.operator.In;
import com.samskivert.depot.operator.LessThan;
import com.samskivert.depot.operator.SQLOperator;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.server.persist.RepositoryUtil;

import static com.threerings.msoy.Log.log;

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
     * @param cutoffDays the number of days in the past before which not to load messages.
     */
    public void loadPersonalFeed (int memberId, List<FeedMessageRecord> messages,
                                  Collection<Integer> friendIds, int cutoffDays)
    {
        SQLOperator self = new Equals(SelfFeedMessageRecord.TARGET_ID, memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, cutoffDays);
        if (!friendIds.isEmpty()) {
            SQLOperator actors = null;
            actors = new In(FriendFeedMessageRecord.ACTOR_ID, friendIds);
            loadFeedMessages(messages, FriendFeedMessageRecord.class, actors, cutoffDays);
        }

        // include actions the member has performed
        SQLOperator actor = new Equals(FriendFeedMessageRecord.ACTOR_ID, memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, cutoffDays);
    }

    /**
     * Loads all global and group feed messages for the given groups.
     *
     * @param cutoffDays the number of days in the past before which not to load messages.
     */
    public void loadGroupFeeds (
        List<FeedMessageRecord> messages, Collection<Integer> groupIds, int cutoffDays)
    {
        loadFeedMessages(messages, GlobalFeedMessageRecord.class, null, cutoffDays);
        if (!groupIds.isEmpty()) {
            SQLOperator groups = null;
            groups = new In(GroupFeedMessageRecord.GROUP_ID, groupIds);
            loadFeedMessages(messages, GroupFeedMessageRecord.class, groups, cutoffDays);
        }
    }

    /**
     * Loads all applicable feed messages by the specified member.
     *
     * @param cutoffDays the number of days in the past before which not to load messages.
     */
    public List <FeedMessageRecord> loadMemberFeed (int memberId, int cutoffDays)
    {
        List<FeedMessageRecord> messages = Lists.newArrayList();
        SQLOperator actor = new Equals(FriendFeedMessageRecord.ACTOR_ID, memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, cutoffDays);
        SQLOperator self = new Equals(SelfFeedMessageRecord.TARGET_ID, memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, cutoffDays);
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
            bits.add(new Equals(FriendFeedMessageRecord.ACTOR_ID, actorId));
            bits.add(new Equals(FriendFeedMessageRecord.TYPE, type.getCode()));
            bits.add(new GreaterThan(FriendFeedMessageRecord.POSTED, throttle));

            FeedMessageCount count = load(
                FeedMessageCount.class, new FromOverride(FriendFeedMessageRecord.class),
                new Where(new And(bits)));
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
            bits.add(new Equals(GroupFeedMessageRecord.GROUP_ID, groupId));
            bits.add(new Equals(GroupFeedMessageRecord.TYPE, type.getCode()));
            bits.add(new GreaterThan(GroupFeedMessageRecord.POSTED, throttle));

            FeedMessageCount count = load(
                FeedMessageCount.class, new FromOverride(GroupFeedMessageRecord.class),
                new Where(new And(bits)));
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

    /**
     * Prunes feed messages of all types that have expired.
     */
    public void pruneFeeds ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - FEED_EXPIRATION_PERIOD);
        int global = deleteAll(GlobalFeedMessageRecord.class,
            new Where(new LessThan(GlobalFeedMessageRecord.POSTED, cutoff)), null);
        int friend = deleteAll(FriendFeedMessageRecord.class,
            new Where(new LessThan(FriendFeedMessageRecord.POSTED, cutoff)), null);
        int group = deleteAll(GroupFeedMessageRecord.class,
            new Where(new LessThan(GroupFeedMessageRecord.POSTED, cutoff)), null);
        int self = deleteAll(SelfFeedMessageRecord.class,
            new Where(new LessThan(SelfFeedMessageRecord.POSTED, cutoff)), null);
        log.info("Feeds pruned", "global", global, "friend", friend, "group", group, "self", self);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(FriendFeedMessageRecord.class,
                  new Where(new In(FriendFeedMessageRecord.ACTOR_ID, memberIds)));
        deleteAll(SelfFeedMessageRecord.class,
                  new Where(new In(SelfFeedMessageRecord.TARGET_ID, memberIds)));
    }

    protected void loadFeedMessages (List<FeedMessageRecord> messages,
                                     Class<? extends FeedMessageRecord> pClass,
                                     SQLOperator main, int cutoffDays)
    {
        List<SQLOperator> whereBits = Lists.newArrayList();
        if (main != null) {
            whereBits.add(main);
        }
        whereBits.add(new GreaterThanEquals(FeedMessageRecord.POSTED.as(pClass),
                                            RepositoryUtil.getCutoff(cutoffDays)));
        messages.addAll(findAll(pClass, new Where(new And(whereBits))));
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
