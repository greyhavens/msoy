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
     *
     * @param cutoffDays the number of days in the past before which not to load messages.
     */
    public void loadPersonalFeed (int memberId, List<FeedMessageRecord> messages,
                                  Collection<Integer> friendIds, int cutoffDays)
    {
        SQLExpression<Boolean> self = SelfFeedMessageRecord.TARGET_ID.eq(memberId);
        loadFeedMessages(messages, SelfFeedMessageRecord.class, self, cutoffDays);
        if (!friendIds.isEmpty()) {
            SQLExpression<Boolean> actors = null;
            actors = FriendFeedMessageRecord.ACTOR_ID.in(friendIds);
            loadFeedMessages(messages, FriendFeedMessageRecord.class, actors, cutoffDays);
        }

        // include actions the member has performed
        SQLExpression<Boolean> actor = FriendFeedMessageRecord.ACTOR_ID.eq(memberId);
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
            SQLExpression<Boolean> groups = null;
            groups = GroupFeedMessageRecord.GROUP_ID.in(groupIds);
            loadFeedMessages(messages, GroupFeedMessageRecord.class, groups, cutoffDays);
        }
    }

    /**
     * Loads feed messages by the specified member up to the specified limit. They are sorted from
     * most recently occurring to least.
     */
    public List<FeedMessageRecord> loadMemberFeed (int memberId, int limit)
    {
        List<FeedMessageRecord> messages = Lists.newArrayList();
        messages.addAll(from(FriendFeedMessageRecord.class).
                        where(FriendFeedMessageRecord.ACTOR_ID, memberId).
                        descending(FriendFeedMessageRecord.POSTED).limit(limit).select());
        messages.addAll(from(SelfFeedMessageRecord.class).
                        where(SelfFeedMessageRecord.TARGET_ID, memberId).
                        descending(SelfFeedMessageRecord.POSTED).limit(limit).select());
        Collections.sort(messages, FeedMessageRecord.BY_POSTED);
        CollectionUtil.limit(messages, limit);
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
     * Prunes feed messages of all types that have expired.
     */
    public void pruneFeeds ()
    {
        Timestamp cutoff = new Timestamp(System.currentTimeMillis() - FEED_EXPIRATION_PERIOD);
        int global = from(GlobalFeedMessageRecord.class).where(
            GlobalFeedMessageRecord.POSTED.lessThan(cutoff)).delete(null);
        int friend = from(FriendFeedMessageRecord.class).where(
            FriendFeedMessageRecord.POSTED.lessThan(cutoff)).delete(null);
        int group = from(GroupFeedMessageRecord.class).where(
            GroupFeedMessageRecord.POSTED.lessThan(cutoff)).delete(null);
        int self = from(SelfFeedMessageRecord.class).where(
            SelfFeedMessageRecord.POSTED.lessThan(cutoff)).delete(null);
        log.info("Feeds pruned", "global", global, "friend", friend, "group", group, "self", self);
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
                                     SQLExpression<?> main, int cutoffDays)
    {
        List<SQLExpression<?>> whereBits = Lists.newArrayList();
        if (main != null) {
            whereBits.add(main);
        }
        whereBits.add(FeedMessageRecord.POSTED.as(pClass).greaterEq(
                          RepositoryUtil.getCutoff(cutoffDays)));
        messages.addAll(from(pClass).where(whereBits).select());
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
