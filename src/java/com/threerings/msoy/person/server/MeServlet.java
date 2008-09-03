//
// $Id$

package com.threerings.msoy.person.server;

import java.util.List;
import java.util.Map;

import java.sql.Timestamp;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.MsoyAuthCodes;
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
import com.threerings.msoy.person.gwt.MyWhirledData;
import com.threerings.msoy.person.gwt.PassportData;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
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

        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        data.friendCount = friendIds.size();
        if (data.friendCount > 0) {
            data.friends = _mhelper.resolveMemberCards(friendIds, true, friendIds);
        }

        IntSet groupMemberships = new ArrayIntSet();
        for (GroupMembershipRecord gmr : _groupRepo.getMemberships(mrec.memberId)) {
            groupMemberships.add(gmr.groupId);
        }
        data.feed = loadFeed(mrec, groupMemberships, DEFAULT_FEED_DAYS);

        data.badges = _badgeLogic.getNextSuggestedBadges(mrec.memberId, 4);
        return data;
    }

    // from interface MeService
    public void updateWhirledNews (final String newsHtml)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (!mrec.isAdmin()) {
            throw new ServiceException(MsoyAuthCodes.ACCESS_DENIED);
        }

        postDObjectAction(new Runnable() {
            public void run () {
                RuntimeConfig.server.setWhirledwideNewsHtml(newsHtml);
            }
        });
    }

    // from interface MeService
    public List<FeedMessage> loadFeed (int cutoffDays)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        List<GroupMembershipRecord> groups = _groupRepo.getMemberships(mrec.memberId);
        ArrayIntSet groupIds = new ArrayIntSet(groups.size());
        for (GroupMembershipRecord record : groups) {
            groupIds.add(record.groupId);
        }
        return loadFeed(mrec, groupIds, cutoffDays);
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
            data.nextBadges = _badgeLogic.getInProgressBadges(mrec.memberId, true);

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
            if (type.isHidden()) {
                continue;
            }

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

    /**
     * Helper function for {@link #loadFeed} and {@link #getMyWhirled}.
     */
    protected List<FeedMessage> loadFeed (MemberRecord mrec, IntSet groupIds, int cutoffDays)
    {
        Timestamp since = new Timestamp(System.currentTimeMillis() - cutoffDays * 24*60*60*1000L);
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        return _servletLogic.resolveFeedMessages(
            _feedRepo.loadPersonalFeed(mrec.memberId, friendIds, groupIds, since));
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
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected BadgeLogic _badgeLogic;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;
}
