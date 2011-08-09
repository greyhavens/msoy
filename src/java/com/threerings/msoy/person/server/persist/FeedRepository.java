//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.CollectionUtil;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.expression.SQLExpression;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.server.persist.RepositoryUtil;

import static com.threerings.msoy.Log.log;

/**
 * Maintains persistent data for feeds.
 */
@Singleton @BlockingThread
public class FeedRepository extends DepotRepository
{
    @Inject public FeedRepository (PersistenceContext perCtx)
    {
        super(perCtx);
    }

    /**
     * Loads all applicable personal feed messages for the specified member.
     */
    public void loadPersonalFeed (int memberId, List<FeedMessageRecord> messages,
        Collection<Integer> friendIds, long beforeTime, int count)
    {
        SQLExpression<Boolean> self = SelfFeedMessageRecord.TARGET_ID.eq(memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, beforeTime, count);
        if (!friendIds.isEmpty()) {
            SQLExpression<Boolean> actors = null;
            actors = FriendFeedMessageRecord.ACTOR_ID.in(friendIds);
            loadFeedMessages(messages, FriendFeedMessageRecord.class, actors, beforeTime, count);
        }

        // include actions the member has performed
        SQLExpression<Boolean> actor = FriendFeedMessageRecord.ACTOR_ID.eq(memberId);
        loadFeedMessages(messages, FriendFeedMessageRecord.class, actor, beforeTime, count);
    }

    /**
     * Loads all global and group feed messages for the given groups.
     */
    public void loadGroupFeeds (
        List<FeedMessageRecord> messages, Collection<Integer> groupIds, long beforeTime, int count)
    {
        loadFeedMessages(messages, GlobalFeedMessageRecord.class, null, beforeTime, count);
        if (!groupIds.isEmpty()) {
            SQLExpression<Boolean> groups = null;
            groups = GroupFeedMessageRecord.GROUP_ID.in(groupIds);
            loadFeedMessages(messages, GroupFeedMessageRecord.class, groups, beforeTime, count);
        }
    }

    /**
     * Loads feed messages by the specified member, using a count as a guideline. May return more
     * records than you asked for.
     */
    public List<FeedMessageRecord> loadMemberFeed (
        int memberId, long beforeTime, int count)
    {
        List<FeedMessageRecord> messages = Lists.newArrayList();

        List<SQLExpression<?>> conditions = Lists.newArrayList();
        conditions.add(FriendFeedMessageRecord.ACTOR_ID.eq(memberId));
        if (beforeTime < Long.MAX_VALUE) {
            conditions.add(FriendFeedMessageRecord.POSTED.lessThan(new Timestamp(beforeTime)));
        }
        messages.addAll(from(FriendFeedMessageRecord.class)
            .where(conditions)
            .descending(FriendFeedMessageRecord.POSTED)
            .limit(count)
            .select());

        conditions = Lists.newArrayList();
        conditions.add(SelfFeedMessageRecord.TARGET_ID.eq(memberId));
        if (beforeTime < Long.MAX_VALUE) {
            conditions.add(SelfFeedMessageRecord.POSTED.lessThan(new Timestamp(beforeTime)));
        }
        messages.addAll(from(SelfFeedMessageRecord.class)
            .where(conditions)
            .descending(SelfFeedMessageRecord.POSTED)
            .limit(count)
            .select());

        return messages;
    }

    /**
     * Use {@link FeedLogic#publishGlobalMessage}.
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
     * Use {@link FeedLogic#publishSelfMessage}.
     */
    public boolean publishSelfMessage (
        int targetId, int actorId, FeedMessageType type, String data, boolean throttle)
    {
        if (throttle && type.getThrottleCount() > 0) {
            Timestamp threshold = new Timestamp(
                System.currentTimeMillis() - type.getThrottlePeriod());
            int count = from(SelfFeedMessageRecord.class).where(
                SelfFeedMessageRecord.TARGET_ID.eq(targetId),
                SelfFeedMessageRecord.ACTOR_ID.eq(actorId),
                SelfFeedMessageRecord.TYPE.eq(type.getCode()),
                SelfFeedMessageRecord.POSTED.greaterThan(threshold)).selectCount();
            if (count >= type.getThrottleCount()) {
                return false;
            }
        }

        SelfFeedMessageRecord message = new SelfFeedMessageRecord();
        message.targetId = targetId;
        message.actorId = actorId;
        message.type = type.getCode();
        message.data = data;
        message.posted = new Timestamp(System.currentTimeMillis());
        insert(message);
        return true;
    }

    /**
     * Use {@link FeedLogic#publishMemberMessage}.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, String data)
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle = new Timestamp(
                System.currentTimeMillis() - type.getThrottlePeriod());
            int count = from(FriendFeedMessageRecord.class).where(
                FriendFeedMessageRecord.ACTOR_ID.eq(actorId),
                FriendFeedMessageRecord.TYPE.eq(type.getCode()),
                FriendFeedMessageRecord.POSTED.greaterThan(throttle)).selectCount();
            if (count >= type.getThrottleCount()) {
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
     * Use {@link FeedLogic#publishGroupMessage}.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, String data)
    {
        if (type.getThrottleCount() > 0) {
            Timestamp throttle = new Timestamp(
                System.currentTimeMillis() - type.getThrottlePeriod());
            int count = from(GroupFeedMessageRecord.class).where(
                GroupFeedMessageRecord.GROUP_ID.eq(groupId),
                GroupFeedMessageRecord.TYPE.eq(type.getCode()),
                GroupFeedMessageRecord.POSTED.greaterThan(throttle)).selectCount();
            if (count >= type.getThrottleCount()) {
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
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        from(FriendFeedMessageRecord.class).where(
            FriendFeedMessageRecord.ACTOR_ID.in(memberIds)).delete();
        from(SelfFeedMessageRecord.class).where(
            SelfFeedMessageRecord.TARGET_ID.in(memberIds)).delete();
    }

    protected void loadFeedMessages (List<FeedMessageRecord> messages,
        Class<? extends FeedMessageRecord> pClass,
        SQLExpression<?> condition, long beforeTime, int count)
    {
        List<SQLExpression<?>> whereBits = Lists.newArrayList();
        if (condition != null) {
            whereBits.add(condition);
        }
        whereBits.add(FeedMessageRecord.POSTED.as(pClass).lessThan(new Timestamp(beforeTime)));
        messages.addAll(from(pClass)
            .where(whereBits)
            .descending(FeedMessageRecord.POSTED.as(pClass))
            .limit(count)
            .select());
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
