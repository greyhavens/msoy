//
// $Id$

package com.threerings.msoy.person.server;

import java.util.Collection;
import java.util.Collections;
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

import com.samskivert.util.CollectionUtil;

import com.threerings.gwt.util.ExpanderResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.gwt.StampCategory;
import com.threerings.msoy.badge.server.BadgeLogic;
import com.threerings.msoy.badge.server.persist.BadgeRepository;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.data.all.Award.AwardType;
import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.EarnedMedalRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.MedalRecord;
import com.threerings.msoy.group.server.persist.MedalRepository;
import com.threerings.msoy.person.gwt.MeService;
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.person.gwt.PassportData;
import com.threerings.msoy.person.server.persist.ProfileRecord;
import com.threerings.msoy.person.server.persist.ProfileRepository;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.ContestRecord;
import com.threerings.msoy.server.persist.ContestRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.PromotionRecord;
import com.threerings.msoy.server.persist.PromotionRepository;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.Contest;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;
// import com.threerings.msoy.web.server.RPCProfiler;

import static com.threerings.msoy.Log.log;

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

        // if (PROFILING_ENABLED) {
        //     _profiler.enter("promotions");
        // }

        // include all our active promotions
        data.promos = Lists.newArrayList(
            Iterables.transform(_promoRepo.loadActivePromotions(), PromotionRecord.TO_PROMOTION));

        // if (PROFILING_ENABLED) {
        //     _profiler.swap("friends");
        // }

        // load information on their friends
        Set<Integer> friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        data.friendCount = friendIds.size();
        if (data.friendCount > 0) {
            data.friends = _mhelper.resolveMemberCards(friendIds, true, null);
        }

        // if (PROFILING_ENABLED) {
        //     _profiler.swap("greeters");
        // }

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

        // if (PROFILING_ENABLED) {
        //     _profiler.swap("forums");
        // }

        Set<Integer> groupIds = _groupLogic.getMemberGroupIds(mrec.memberId);
        data.updatedThreads = _forumRepo.countUnreadThreads(mrec.memberId, groupIds);

        // if (PROFILING_ENABLED) {
        //     _profiler.swap("stream");
        // }

        data.stream = _feedLogic.loadStreamActivity(mrec.memberId, System.currentTimeMillis(),
            MyWhirledData.STREAM_PAGE_LENGTH);

        // if (PROFILING_ENABLED) {
        //     _profiler.exit(null);
        // }

        return data;
    }

    // from interface MeService
    public ExpanderResult<Activity> loadStream (long beforeTime, int count)
        throws ServiceException
    {
        // Sanity check
        if (count > 100) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        MemberRecord mrec = requireAuthedUser();
        return _feedLogic.loadStreamActivity(mrec.memberId, beforeTime, count);
    }

    // from interface MeService
    public PassportData loadBadges (int memberId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        PassportData data = new PassportData();

        if (mrec.memberId == memberId) {
            data.stampOwner = mrec.name;
            // for now, we just ship along every badge relevant to this player except OUTSPOKEN
            data.nextBadges = Lists.newArrayList(Iterables.filter(
                    _badgeLogic.getInProgressBadges(mrec.memberId, mrec.badgesVersion, true),
                    new Predicate<InProgressBadge>() {
                        public boolean apply (InProgressBadge badge) {
                            return BadgeType.getType(badge.badgeCode) != BadgeType.OUTSPOKEN;
                        }
                    }));

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

        // filter out the OUTSPOKEN badge for display
        badgeUnion = Iterables.filter(badgeUnion, new Predicate<Badge>() {
            public boolean apply (Badge badge) {
                return BadgeType.getType(badge.badgeCode) != BadgeType.OUTSPOKEN;
            }
        });

        for (StampCategory category : StampCategory.values()) {
            data.stamps.put(category, Lists.newArrayList(
                                Iterables.filter(badgeUnion, new FilterByCategory(category))));
        }

        // first grab the set of earned medals
        data.medals = Maps.newHashMap();
        data.officialGroups = Lists.newArrayList();
        Map<Integer, Award> medals = Maps.newHashMap();
        for (EarnedMedalRecord earnedMedalRec : _medalRepo.loadEarnedMedals(memberId)) {
            Award medal = new Award();
            medal.awardId = earnedMedalRec.medalId;
            medal.whenEarned = earnedMedalRec.whenEarned.getTime();
            medals.put(earnedMedalRec.medalId, medal);
        }
        // flesh out the details from the MedalRecord
        Map<Integer, List<Award>> groupMedals = Maps.newHashMap();
        for (MedalRecord medalRec : _medalRepo.loadMedals(medals.keySet())) {
            Award medal = medals.get(medalRec.medalId);
            medal.name = medalRec.name;
            medal.description = medalRec.description;
            medal.icon = medalRec.createIconMedia();

            List<Award> medalList = groupMedals.get(medalRec.groupId);
            if (medalList == null) {
                groupMedals.put(medalRec.groupId, medalList = Lists.newArrayList());
            }
            medalList.add(medal);
        }
        // finally get the group names and the officialness of each group.
        for (GroupRecord groupRec : _groupRepo.loadGroups(groupMedals.keySet())) {
            GroupName groupName = groupRec.toGroupName();
            data.medals.put(groupName, groupMedals.get(groupRec.groupId));
            if (groupRec.official) {
                data.officialGroups.add(groupName);
            }
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

    // from interface MeService
    public void deleteEarnedMedal (int memberId, int medalId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        if (!mrec.isSupport()) {
            log.warning("Non-support attempted to delete an earned medal", "deleter", mrec.memberId,
                "memberId", memberId, "medalId", medalId);
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        ProfileRecord record = _profileRepo.loadProfile(memberId);
        if (record != null && record.profileMedalId == medalId) {
            _profileRepo.updateProfileAward(memberId, 0, 0);
        }

        if (!_medalRepo.deleteEarnedMedal(memberId, medalId)) {
            log.warning("Failed to delete an earned medal", "memberId", memberId, "medalId",
                medalId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MeService
    public void selectProfileAward (AwardType type, int awardId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        int badgeCode = 0, medalId = 0;
        if (type == AwardType.BADGE) {
            if (_badgeRepo.loadEarnedBadge(mrec.memberId, awardId) == null) {
                log.warning("Player selected unowned badge for their profile",
                    "memberId", mrec.memberId, "badgeCode", awardId);
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
            badgeCode = awardId;

        } else {
            if (_medalRepo.loadEarnedMedal(mrec.memberId, awardId) == null) {
                log.warning("Player selected unowned medal for their profile",
                    "memberId", mrec.memberId, "medalId", awardId);
                throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
            }
            medalId = awardId;
        }

        _profileRepo.updateProfileAward(mrec.memberId, badgeCode, medalId);
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
    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected ContestRepository _contestRepo;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected ForumRepository _forumRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MedalRepository _medalRepo;
    @Inject protected MemberManager _memberMan;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected ProfileRepository _profileRepo;
    @Inject protected PromotionRepository _promoRepo;
    // @Inject protected RPCProfiler _profiler;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int MAX_GREETERS_TO_SHOW = 10;
}
