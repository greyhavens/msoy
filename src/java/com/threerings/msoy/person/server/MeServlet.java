//
// $Id$

package com.threerings.msoy.person.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.IntSet;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.gwt.StampCategory;
import com.threerings.msoy.badge.server.BadgeLogic;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;

import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MyWhirledData.FeedCategory;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.person.gwt.PassportData;
import com.threerings.msoy.person.server.persist.FeedMessageRecord;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.server.persist.FriendFeedMessageRecord;
import com.threerings.msoy.person.server.persist.GroupFeedMessageRecord;
import com.threerings.msoy.person.util.FeedMessageType.Category;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.ContestRecord;
import com.threerings.msoy.server.persist.ContestRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.PromotionRecord;
import com.threerings.msoy.server.persist.PromotionRepository;

import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.RPCProfiler;
import com.threerings.msoy.web.server.ServletLogic;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;

/**
 * Implements the {@link MeService}.
 */
public class MeServlet extends MsoyServiceServlet
    implements MeService
{
    // from MeService
    public MyWhirledData getMyWhirled ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        MyWhirledData data = new MyWhirledData();
        data.whirledPopulation = _memberMan.getPPSnapshot().getPopulationCount();

        if (PROFILING_ENABLED) {
            _profiler.enter("promotions");
        }

        // include all our active promotions
        data.promos = Lists.newArrayList(
            Iterables.transform(_promoRepo.loadPromotions(), PromotionRecord.TO_PROMOTION));

        if (PROFILING_ENABLED) {
            _profiler.swap("friends");
        }

        // load information on their friends
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        data.friendCount = friendIds.size();
        if (data.friendCount > 0) {
            data.friends = _mhelper.resolveMemberCards(friendIds, true, friendIds);
        }

        if (PROFILING_ENABLED) {
            _profiler.swap("feed");
        }

        // load their feed
        data.feed = loadFeedCategories(mrec, friendIds, FeedCategory.DEFAULT_COUNT, null);

        if (PROFILING_ENABLED) {
            _profiler.swap("greeters");
        }

        // load the eligible greeters
        HashSet<Integer> greeterIds = new HashSet<Integer>(
            _memberMan.getPPSnapshot().getOnlineGreeters());
        greeterIds.remove(mrec.memberId);
        greeterIds.removeAll(friendIds);

        // prune
        Collection<Integer> shortList = CollectionUtil.selectRandomSubset(
            greeterIds, Math.min(greeterIds.size(), MAX_GREETERS_TO_SHOW));

        // load cards
        data.greeters = _mhelper.resolveMemberCards(shortList, true, null);

        // shuffle to avoid greeter fighting (shortList is sorted, thus the cards are too)
        Collections.shuffle(data.greeters);

        if (PROFILING_ENABLED) {
            _profiler.exit(null);
        }

        return data;
    }

    // from interface MeService
    public FeedCategory loadFeedCategory (int category, boolean fullSize)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        int itemsPerCategory = fullSize ? FeedCategory.FULL_COUNT : FeedCategory.DEFAULT_COUNT;
        List<FeedCategory> categories = loadFeedCategories(
            mrec, _memberRepo.loadFriendIds(mrec.memberId), itemsPerCategory,
            Category.values()[category]);
        return (categories.size() > 0) ? categories.get(0) : null;
    }

    // from interface MeService
    public PassportData loadBadges (int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // PassportData contains the owner's name because we'll eventually be viewing passports for
        // other players as well
        PassportData data = new PassportData();

        if (mrec.memberId == memberId) {
            data.stampOwner = mrec.name;
            // for now, we just ship along every badge relevant to this player.
            data.nextBadges = _badgeLogic.getInProgressBadges(
                mrec.memberId, mrec.badgesVersion, true);

        } else {
            MemberName stampOwner = _memberRepo.loadMemberName(memberId);
            if (stampOwner == null) {
                return null;
            }
            data.stampOwner = stampOwner.toString();
            // we leave data.nextBadges empty when viewing other people's passport page.
        }

        data.stamps = Maps.newHashMap();
        Iterable<Badge> badgeUnion = data.nextBadges == null ?
            Lists.<EarnedBadgeRecord, Badge>transform(
                _badgeRepo.loadEarnedBadges(memberId),  EarnedBadgeRecord.TO_BADGE) :
            // Create a set union between the in progress badges retrieved above, and earned
            // badge records from the database.  Due to InProgressFilter, we're guaranteed that
            // in the intersection between the EarnedBadges and InProgressBadges, we'll end
            // up with an InProgressBadge, which is what we want for client display.
            Sets.union(Sets.newHashSet(Lists.transform(_badgeRepo.loadEarnedBadges(memberId),
                                                       new InProgressFilter(data.nextBadges))),
                       Sets.newHashSet(data.nextBadges));
        for (StampCategory category : StampCategory.values()) {
            data.stamps.put(category, Lists.newArrayList(
                                Iterables.filter(badgeUnion, new FilterByCategory(category))));
        }
        return data;
    }

    // from interface MeService
    public List<Badge> loadAllBadges ()
        throws ServiceException
    {
        long now = System.currentTimeMillis();
        List<Badge> badges = Lists.newArrayList();
        int progress = 0;
        for (BadgeType type : BadgeType.values()) {
            int code = type.getCode();
            for (int ii = 0; ii < type.getNumLevels(); ii++) {
                String levelUnits = type.getRequiredUnitsString(ii);
                int coinValue = type.getCoinValue(ii);
                badges.add(new InProgressBadge(code, ii, levelUnits, type.getLevel(ii).coinValue,
                    // range through progresses from 0 - 100% inclusive, in 10% increments
                    (progress = (progress + 1) % 11)/(float)10));
                badges.add(new EarnedBadge(code, ii, levelUnits, coinValue, now));
            }
        }
        return badges;
    }

    // from interface MeService
    public List<Contest> loadContests ()
        throws ServiceException
    {
        return Lists.newArrayList(Lists.transform(_contestRepo.loadContests(),
            ContestRecord.TO_CONTEST));
    }

    /**
     * Pull up a list of news feed events for the current member, grouped by category. Only
     * itemsPerCategory items will be returned, or in the case of aggregation only items from the
     * first itemsPerCategory actors.
     *
     * @param category If null, load all categories, otherwise only load that one.
     */
    protected List<FeedCategory> loadFeedCategories (
        MemberRecord mrec, IntSet friendIds, int itemsPerCategory, Category onlyCategory)
        throws ServiceException
    {
        int feedDays = MAX_FEED_CUTOFF_DAYS;
        // if we're loading all categories, adjust the number of days of data we load based on how
        // many friends this member has
        if (onlyCategory == null) {
            feedDays = Math.max(MIN_FEED_CUTOFF_DAYS, feedDays-friendIds.size()/FRIENDS_PER_DAY);
        }

        // fetch all messages for the member's friends & groups from the past feedDays days
        Set<Integer> groupMemberships = Sets.newHashSet(Iterables.transform(
            _groupRepo.getMemberships(mrec.memberId), GroupMembershipRecord.TO_GROUP_ID));
        long since = System.currentTimeMillis() - feedDays * 24*60*60*1000L;
        List<FeedMessageRecord> allRecords = _feedRepo.loadPersonalFeed(
            mrec.memberId, friendIds, groupMemberships, since);

        // sort all the records by date
        Collections.sort(allRecords, new Comparator<FeedMessageRecord>() {
            public int compare (FeedMessageRecord f1, FeedMessageRecord f2) {
                return f2.posted.compareTo(f1.posted);
            }
        });

        List<FeedMessageRecord> allChosenRecords = Lists.newArrayList();
        Map<Category, List<String>> keysByCategory = Maps.newHashMap();

        // limit the feed messages to itemsPerCategory per category
        for (FeedMessageRecord record : allRecords) {
            Category category = FeedMessageType.getCategory(record.type);

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
            if (record.type == FeedMessageType.FRIEND_GAINED_LEVEL.getCode()) {
                // all levelling records are returned, they get aggregated into a single item
                key = "";
            } else if (record.type == FeedMessageType.GROUP_UPDATED_ROOM.getCode()) {
                // include room updates from the first itemsPerCategory groups
                key = "group_" + ((GroupFeedMessageRecord)record).groupId + "";
            } else if (record.type == FeedMessageType.SELF_ROOM_COMMENT.getCode()) {
                // include comments on the first itemsPerCategory rooms and/or items
                key = "room_" + record.data.split("\t")[0];
            } else if (record.type == FeedMessageType.SELF_ITEM_COMMENT.getCode()) {
                // include comments on the first itemsPerCategory rooms and/or items
                key = "item_" + record.data.split("\t")[1];
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
        List<FeedMessage> allChosenMessages = _servletLogic.resolveFeedMessages(allChosenRecords);

        // group up the resolved messages by category
        List<FeedCategory> feed = Lists.newArrayList();
        for (Category category : FeedMessageType.Category.values()) {

            // pull out messages of the right category (combine global & group announcements)
            List<FeedMessage> typeMessages = Lists.newArrayList();
            for (FeedMessage message : allChosenMessages) {
                if (FeedMessageType.getCategory(message.type) == category) {
                    typeMessages.add(message);
                }
            }
            allChosenMessages.removeAll(typeMessages);

            if (typeMessages.size() == 0) {
                continue;
            }

            FeedCategory feedCategory = new FeedCategory();
            feedCategory.category = category.ordinal();
            feedCategory.messages = typeMessages.toArray(new FeedMessage[typeMessages.size()]);
            feed.add(feedCategory);
        }
        return feed;
    }

    /** Helper for loadBadges */
    protected static class FilterByCategory implements Predicate<Badge>
    {
        public FilterByCategory (StampCategory category) {
            _category = category;
        }

        public boolean apply (Badge badge) {
            return BadgeType.getType(badge.badgeCode).getCategory().equals(_category);
        }

        protected StampCategory _category;
    }

    /** Helper for loadBadges. */
    protected static class InProgressFilter implements Function<EarnedBadgeRecord, Badge>
    {
        public InProgressFilter (List<InProgressBadge> existing)  {
            _index = Maps.uniqueIndex(existing, BadgeType.BADGE_TO_CODE);
        }

        public Badge apply (EarnedBadgeRecord record) {
            // Return an EarnedBadge only in the case where an InProgressBadge isn't found for the
            // EarnedBadgeRecord's badgeCode.
            Badge badge = _index.get(record.badgeCode);
            return badge == null ? record.toBadge() : badge;
        }

        protected Map<Integer, InProgressBadge> _index;
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected PromotionRepository _promoRepo;
    @Inject protected ContestRepository _contestRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected RPCProfiler _profiler;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int MAX_GREETERS_TO_SHOW = 10;
    protected static final int MAX_FEED_CUTOFF_DAYS = 7;
    protected static final int MIN_FEED_CUTOFF_DAYS = 2;
    protected static final int FRIENDS_PER_DAY = 25;
}
