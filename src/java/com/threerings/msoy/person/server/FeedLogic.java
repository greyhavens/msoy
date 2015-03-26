//
// $Id$

package com.threerings.msoy.person.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.CollectionUtil;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.orth.data.MediaDesc;

import com.threerings.gwt.util.ExpanderResult;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.comment.server.persist.CommentRecord;
import com.threerings.msoy.comment.server.persist.CommentRepository.CommentThread;
import com.threerings.msoy.comment.server.persist.CommentRepository;
// import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard;

import static com.threerings.msoy.Log.log;

/**
 * Provides new feed related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class FeedLogic
{
    /**
     * Loads all news stream activity for the specified member.
     */
    public ExpanderResult<Activity> loadStreamActivity (int memberId, long beforeTime, int count)
    {
        List<FeedMessageRecord> feedRecords = Lists.newArrayList();
        List<Integer> friends = _memberRepo.loadActiveFriendIds(memberId);
        _feedRepo.loadPersonalFeed(memberId, feedRecords, friends, beforeTime, count + 1);

        Set<Integer> groups = _groupLogic.getMemberGroupIds(memberId);
        _feedRepo.loadGroupFeeds(feedRecords, groups, beforeTime, count + 1);

        List<CommentThread> threads = _commentRepo.loadStreamComments(
            memberId, friends, beforeTime, count + 1, 2);

        List<Activity> activities = Lists.newArrayList(Iterables.concat(threads, feedRecords));

        ExpanderResult<Activity> result = new ExpanderResult<Activity>();
        result.hasMore = (activities.size() > count);

        Collections.sort(activities, Activity.MOST_RECENT_FIRST);
        CollectionUtil.limit(activities, count);
        result.page = resolveActivities(activities);

        return result;
    }

    /**
     * Loads the general activity (feed and comments) for the specified member.
     */
    public ExpanderResult<Activity> loadMemberActivity (int memberId, long beforeTime, int count)
    {
        List<FeedMessageRecord> feedRecords =
            _feedRepo.loadMemberFeed(memberId, beforeTime, count + 1);

        List<CommentThread> threads = _commentRepo.loadComments(
            CommentType.PROFILE_WALL.toByte(), memberId, beforeTime, count + 1, 2);

        List<Activity> activities = Lists.newArrayList(Iterables.concat(threads, feedRecords));

        ExpanderResult<Activity> result = new ExpanderResult<Activity>();
        result.hasMore = (activities.size() > count);

        Collections.sort(activities, Activity.MOST_RECENT_FIRST);
        CollectionUtil.limit(activities, count);
        result.page = resolveActivities(activities);

        return result;
    }

    public ExpanderResult<Activity> loadComments (
        CommentType etype, int eid, long beforeTime, int count)
    {
        List<CommentThread> threads = _commentRepo.loadComments(
            etype.toByte(), eid, beforeTime, count + 1, 2);

        ExpanderResult<Activity> result = new ExpanderResult<Activity>();
        result.hasMore = (threads.size() > count);

        Collections.sort(threads, Activity.MOST_RECENT_FIRST);
        CollectionUtil.limit(threads, count);
        result.page = resolveActivities(threads);

        return result;
    }

    public ExpanderResult<Activity> loadReplies (
        CommentType etype, int eid, long replyTo, long beforeTime, int count)
    {
        CommentThread thread = _commentRepo.loadReplies(
            etype.toByte(), eid, replyTo, beforeTime, count);

        ExpanderResult<Activity> result = new ExpanderResult<Activity>();
        result.hasMore = thread.hasMoreReplies;
        result.page = resolveActivities(Collections.singletonList(thread));
        return result;
    }

    protected List<Activity> resolveActivities (List<? extends Activity> activities)
    {
        Set<Integer> commentMembers = Sets.newHashSet(),
                     messageMembers = Sets.newHashSet(),
                     messageGroups = Sets.newHashSet();

        // Run through and collect all the extra info we need to lookup
        for (Activity activity : activities) {
            if (activity instanceof CommentThread) {
                CommentThread thread = (CommentThread) activity;
                if (thread.comment != null) {
                    commentMembers.add(thread.comment.memberId);
                }
                for (CommentRecord reply : thread.replies) {
                    commentMembers.add(reply.memberId);
                }

            } else if (activity instanceof FeedMessageRecord) {
                FeedMessageRecord record = (FeedMessageRecord) activity;
                record.addReferences(messageMembers, messageGroups);

            } else {
                throw new IllegalArgumentException();
            }
        }

        // Lookup member cards for comments
        Map<Integer, MemberCard> commentCards = MemberCardRecord.toMap(
            _memberRepo.loadMemberCards(commentMembers));

        // Lookup member names for feeds
        Map<Integer, MemberName> memberNames = _memberRepo.loadMemberNames(messageMembers);

        // Lookup group names for feeds
        Map<Integer, GroupName> groupNames = Maps.newHashMap();
        for (GroupRecord group : _groupRepo.loadGroups(messageGroups)) {
            groupNames.put(group.groupId, group.toGroupName());
        }

        // Convert the whole thing into a list that can be sent to the client
        List<Activity> resolved = Lists.newArrayList();
        for (Activity activity : activities) {
            if (activity instanceof CommentThread) {
                CommentThread thread = (CommentThread) activity;
                if (thread.comment != null) {
                    Comment comment = thread.comment.toComment(commentCards);
                    if (comment.commentor == null) {
                        continue; // this member was deleted, shouldn't usually happen
                    }
                    for (CommentRecord reply : thread.replies) {
                        comment.replies.add(reply.toComment(commentCards));
                    }
                    comment.hasMoreReplies = thread.hasMoreReplies;
                    resolved.add(comment);

                } else {
                    // If we're going through loadReplies()
                    for (CommentRecord reply : thread.replies) {
                        resolved.add(reply.toComment(commentCards));
                    }
                }

            } else if (activity instanceof FeedMessageRecord) {
                FeedMessageRecord record = (FeedMessageRecord) activity;
                FeedMessage message = record.toMessage(memberNames, groupNames);
                // signAllMedia(message);
                resolved.add(message);
            }
        }
        return resolved;
    }

    /**
     * Publishes a global message which will show up in all users' feeds. Note: global messages are
     * never throttled.
     */
    public void publishGlobalMessage (FeedMessageType type, Object... args)
    {
        _feedRepo.publishGlobalMessage(type, feedToString(args));
    }

    /**
     * Publishes a feed message to the specified actor's friends.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, Object... args)
    {
        return _feedRepo.publishMemberMessage(actorId, type, feedToString(args));
    }

    /**
     * Publishes a feed message to the specified group's members.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, Object... args)
    {
        return _feedRepo.publishGroupMessage(groupId, type, feedToString(args));
    }

    /**
     * Publishes a self feed message, that will show up on the target's profile.
     */
    public boolean publishSelfMessage (int targetId, int actorId,
        FeedMessageType type, Object...args)
    {
        return publishSelfMessage(targetId, actorId, false, type, args);
    }

    public boolean publishSelfMessage (int targetId, int actorId, boolean throttle,
        FeedMessageType type, Object...args)
    {
        return _feedRepo.publishSelfMessage(targetId, actorId, type, feedToString(args), throttle);
    }

    /**
     * Publishes a feed message to the specified member's friends indicating that they earned a
     * trophy. Handles the further publication of that trophy to Facebook or other external sources
     * as appropriate.
     */
    public void publishTrophyEarned (int memberId, String name, MediaDesc trophyMedia,
                                     int gameId, String gameName, String gameDesc)
    {
        // publish to our local Whirled feed
        publishMemberMessage(memberId, FeedMessageType.FRIEND_WON_TROPHY,
            name, gameId, trophyMedia);
    }

    /**
     * Publishes a feed message to the specified member's friends indicating that they played a
     * game. Handles the further publication of that message to Facebook or other external sources
     * as appropriate.
     */
    public void publishGamePlayed (
        GameInfoRecord game, int[] playerIds, int[] scores, int gameMode)
    {
        // TODO: do something with game mode? this would require some game editor ui for naming
        // them or something
        // TODO: use the scores too, but always replace previous feed items with the higher score
        if (playerIds.length == 1) {
            publishMemberMessage(playerIds[0], FeedMessageType.FRIEND_PLAYED_GAME,
                game.name, game.gameId, game.getThumbMedia());

        } else {
            // TODO: multiplayer message
        }
    }

    // /** Prepare message arguments for viewing on the client. */
    // protected void signAllMedia (FeedMessage message)
    // {
    //     // Hackily iterate over the arguments and sign anything that looks like media.
    //     for (int ii = message.data.length-1; ii >= 0; ii --) {
    //         String[] bits = message.data[ii].split(":");
    //         if (bits.length == 3 && bits[0].length() == 40) {
    //             HashMediaDesc hmd = HashMediaDesc.stringToHMD(message.data[ii]);
    //             if (hmd != null) {
    //                 // we have to sign the media descs.
    //                 message.data[ii] = CloudfrontMediaDesc.cfmdToString(
    //                     MediaDescFactory.createMediaDesc(hmd));
    //             }
    //         }
    //     }
    // }

    /** Prepare message arguments for persisting to database. */
    protected String feedToString (Object[] args)
    {
        for (int ii = args.length-1; ii >= 0; ii --) {
            if (args[ii] instanceof HashMediaDesc) {
                // note that we do not persist CloudfrontMediaDesc's expiration/signature!
                Object foo = args[ii];
                args[ii] = HashMediaDesc.hmdToString((HashMediaDesc) args[ii]);
                log.info("Unpacking bit", "desc", foo, "bit", args[ii]);
            } else if (args[ii] instanceof MediaDesc) {
                log.warning("Unknown media descriptor in feed", "desc", args[ii]);
                args[ii] = "";
            }
        }
        return StringUtil.join(args, "\t");
    }

    @Inject protected CommentRepository _commentRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;

    protected static final int MAX_PERSONAL_FEED_CUTOFF_DAYS = 7;
    protected static final int MIN_PERSONAL_FEED_CUTOFF_DAYS = 2;
    protected static final int FRIENDS_PER_DAY = 25;
    protected static final int GROUP_FEED_CUTOFF_DAYS = 7;
}
