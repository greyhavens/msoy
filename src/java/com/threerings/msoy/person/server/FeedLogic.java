//
// $Id$

package com.threerings.msoy.person.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.game.server.persist.GameInfoRecord;

import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType.Category;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;

/**
 * Provides new feed related services to servlets and other blocking thread entities.
 */
@Singleton @BlockingThread
public class FeedLogic
{
    /**
     * Pull up a list of news feed events for the current member, grouped by category. Only
     * itemsPerCategory items will be returned, or in the case of aggregation only items from the
     * first itemsPerCategory actors.
     *
     * @param onlyCategory If null, load all categories, otherwise only load that one.
     */
    public List<FeedCategory> loadFeedCategories (
        MemberRecord mrec, Set<Integer> friendIds, int itemsPerCategory, Category onlyCategory)
    {
        int feedDays = MAX_PERSONAL_FEED_CUTOFF_DAYS;

        // if we're loading all categories, adjust the number of days of data we load based on how
        // many friends this member has
        if (onlyCategory == null) {
            feedDays = Math.max(
                MIN_PERSONAL_FEED_CUTOFF_DAYS, feedDays - friendIds.size() / FRIENDS_PER_DAY);
        }

        // fetch all messages for the member's friends & groups from the past feedDays days
        Set<Integer> groups = _groupLogic.getMemberGroupIds(mrec.memberId);
        List<FeedMessageRecord> allRecords = Lists.newArrayList();
        _feedRepo.loadPersonalFeed(mrec.memberId, allRecords, friendIds, feedDays);
        // TODO: use different cutoffs for different groups.size()?
        _feedRepo.loadGroupFeeds(allRecords, groups, GROUP_FEED_CUTOFF_DAYS);

        // sort all the records by date
        Collections.sort(allRecords, FeedMessageRecord.BY_POSTED);

        List<FeedMessageRecord> allChosenRecords = Lists.newArrayList();
        Map<Category, List<String>> keysByCategory = Maps.newHashMap();

        // limit the feed messages to itemsPerCategory per category
        for (FeedMessageRecord record : allRecords) {
            FeedMessageType type = FeedMessageType.fromCode(record.type);
            Category category = type.getCategory();

            // skip all categories except the one we care about
            if (onlyCategory != null && category != onlyCategory) {
                continue;
            }

            List<String> typeKeys = keysByCategory.get(category);
            if (typeKeys == null) {
                typeKeys = Lists.newArrayList();
                keysByCategory.put(category, typeKeys);
            }

            String key;
            if (type == FeedMessageType.FRIEND_GAINED_LEVEL) {
                // all levelling records are returned, they get aggregated into a single item
                key = "";
            } else if (type == FeedMessageType.GROUP_UPDATED_ROOM) {
                // include room updates from the first itemsPerCategory groups
                key = "group_" + ((GroupFeedMessageRecord)record).groupId + "";
            } else if (type == FeedMessageType.SELF_ROOM_COMMENT) {
                // include comments on the first itemsPerCategory rooms
                key = "room_" + record.data.split("\t")[0];
            } else if (type == FeedMessageType.SELF_ITEM_COMMENT) {
                // include comments on the first itemsPerCategory items
                key = "item_" + record.data.split("\t")[1];
            } else if (type == FeedMessageType.SELF_GAME_COMMENT) {
                // include comments on the first itemsPerCategory games
                key = "game_" + record.data.split("\t")[1];
            } else if (record instanceof FriendFeedMessageRecord) {
                // include friend activities from the first itemsPerCategory friends
                key = "member_" + ((FriendFeedMessageRecord)record).actorId + "";
            } else {
                // include the first itemsPerCategory non-friend messages in each category
                key = typeKeys.size() + "";
            }

            if (typeKeys.contains(key)) {
                allChosenRecords.add(record);
            } else if (typeKeys.size() < itemsPerCategory) {
                allChosenRecords.add(record);
                typeKeys.add(key);
            }
        }

        // resolve all the chosen messages at the same time
        List<FeedMessage> allChosenMessages = resolveFeedMessages(allChosenRecords);

        // group up the resolved messages by category
        List<FeedCategory> feed = Lists.newArrayList();
        for (Category category : FeedMessageType.Category.values()) {

            // pull out messages of the right category (combine global & group announcements)
            List<FeedMessage> typeMessages = Lists.newArrayList();
            for (FeedMessage message : allChosenMessages) {
                if (message.type.getCategory() == category) {
                    typeMessages.add(message);
                }
            }
            allChosenMessages.removeAll(typeMessages);

            if (typeMessages.size() == 0) {
                continue;
            }

            FeedCategory feedCategory = new FeedCategory();
            feedCategory.category = category;
            feedCategory.messages = typeMessages.toArray(new FeedMessage[typeMessages.size()]);
            feed.add(feedCategory);
        }
        return feed;
    }

    /**
     * Loads the time-sorted feed events <em>generated by</em> the specified member.
     */
    public List<FeedMessage> loadMemberFeed (int memberId, int limit)
    {
        return resolveFeedMessages(_feedRepo.loadMemberFeed(memberId, limit));
    }

    /**
     * Publishes a global message which will show up in all users' feeds. Note: global messages are
     * never throttled.
     */
    public void publishGlobalMessage (FeedMessageType type, Object... args)
    {
        _feedRepo.publishGlobalMessage(type, StringUtil.join(args, "\t"));
    }

    /**
     * Publishes a feed message to the specified actor's friends.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishMemberMessage (int actorId, FeedMessageType type, Object... args)
    {
        return _feedRepo.publishMemberMessage(actorId, type, StringUtil.join(args, "\t"));
    }

    /**
     * Publishes a feed message to the specified group's members.
     *
     * @return true if the message was published, false if it was throttled because it would cause
     * messages of the specified type to exceed their throttle period.
     */
    public boolean publishGroupMessage (int groupId, FeedMessageType type, Object... args)
    {
        return _feedRepo.publishGroupMessage(groupId, type, StringUtil.join(args, "\t"));
    }

    /**
     * Publishes a self feed message, that will show up on the target's profile. These are
     * currently not throttled.
     */
    public void publishSelfMessage (int targetId, int actorId, FeedMessageType type, Object...args)
    {
        _feedRepo.publishSelfMessage(targetId, actorId, type, StringUtil.join(args, "\t"));
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
                             name, gameId, MediaDesc.mdToString(trophyMedia));
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
                game.name, game.gameId, MediaDesc.mdToString(game.getThumbMedia()));

        } else {
            // TODO: multiplayer message
        }
    }

    /**
     * Resolves the necessary names and converts the supplied list of feed messages to runtime
     * records.
     */
    protected List<FeedMessage> resolveFeedMessages (List<FeedMessageRecord> records)
    {
        // find out which member and group names we'll need
        Set<Integer> memberIds = new ArrayIntSet(), groupIds = Sets.newHashSet();
        for (FeedMessageRecord record : records) {
            record.addReferences(memberIds, groupIds);
        }

        // generate a lookup for the member names
        Map<Integer, MemberName> memberNames = _memberRepo.loadMemberNames(memberIds);

        // generate a lookup for the group names
        Map<Integer, GroupName> groupNames = Maps.newHashMap();
        for (GroupRecord group : _groupRepo.loadGroups(groupIds)) {
            groupNames.put(group.groupId, group.toGroupName());
        }

        // create our list of feed messages
        List<FeedMessage> messages = Lists.newArrayList();
        for (FeedMessageRecord record : records) {
            messages.add(record.toMessage(memberNames, groupNames));
        }

        return messages;
    }

    @Inject protected FeedRepository _feedRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;

    protected static final int MAX_PERSONAL_FEED_CUTOFF_DAYS = 7;
    protected static final int MIN_PERSONAL_FEED_CUTOFF_DAYS = 2;
    protected static final int FRIENDS_PER_DAY = 25;
    protected static final int GROUP_FEED_CUTOFF_DAYS = 7;
    protected static final String FAKE_PUBLIC_IMAGE_URL =
        "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png";
}
