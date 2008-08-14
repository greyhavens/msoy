//
// $Id$

package com.threerings.msoy.person.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.sql.Timestamp;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntSet;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.SceneBookmarkEntry;
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

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;
import com.threerings.msoy.web.server.ServletLogic;

import com.threerings.msoy.room.server.persist.MsoySceneRepository;


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

        try {
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

            return data;

        } catch (PersistenceException pe) {
            log.warning("getMyWhirled failed", "memberId", mrec.memberId, "exception", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
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
    public List<MeService.Room> loadMyRooms ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            List<MeService.Room> rooms = Lists.newArrayList();
            for (SceneBookmarkEntry scene : _sceneRepo.getOwnedScenes(mrec.memberId)) {
                MeService.Room room = new MeService.Room();
                room.sceneId = scene.sceneId;
                room.name = scene.sceneName;
                // TODO: load decor thumbnail
                rooms.add(room);
            }
            return rooms;

        } catch (PersistenceException pe) {
            log.warning("Load rooms failed", "memberId", mrec.memberId, "exception", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MeService
    public List<FeedMessage> loadFeed (int cutoffDays)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        try {
            List<GroupMembershipRecord> groups = _groupRepo.getMemberships(mrec.memberId);
            ArrayIntSet groupIds = new ArrayIntSet(groups.size());
            for (GroupMembershipRecord record : groups) {
                groupIds.add(record.groupId);
            }
            return loadFeed(mrec, groupIds, cutoffDays);

        } catch (PersistenceException pe) {
            log.warning("Load feed failed", "memberId", mrec.memberId, "exception", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface MeService
    public PassportData loadBadges ()
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // PassportData contains the owner's name because we'll eventually be viewing passports for
        // other players as well
        PassportData data = new PassportData();
        data.stampOwner = mrec.name;

        try {
            // for now, we just ship along every badge relevant to this player.
            data.nextBadges = _badgeLogic.getInProgressBadges(mrec.memberId, true);

            data.stamps = Maps.newHashMap();
            List<EarnedBadge> stamps = Lists.transform(
                _badgeRepo.loadEarnedBadges(mrec.memberId), EarnedBadgeRecord.TO_BADGE);
            for (StampCategory category : StampCategory.values()) {
                // the list returned from Lists.newArrayList() can't be sent to GWT, or this would
                // all sit happily in one statement.
                List<EarnedBadge> catBadges = new ArrayList<EarnedBadge>();
                Iterables.addAll(
                    catBadges, Iterables.filter(stamps, new FilterByCategory(category)));
                data.stamps.put(category, catBadges);
            }
        } catch (PersistenceException pe) {
            log.warning("Loading badges failed ", "memberId", mrec.memberId, "exception", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
        return data;
    }

    // from interface MeService
    public List<Badge> loadAllBadges ()
        throws ServiceException
    {
        long now = (new Date()).getTime();
        List<Badge> badges = new ArrayList<Badge>();
        for (BadgeType type : BadgeType.values()) {
            int code = type.getCode();
            for (int ii = 0; ii < type.getNumLevels(); ii++) {
                badges.add(new InProgressBadge(code, ii, (float)0.5));
                badges.add(new EarnedBadge(code, ii, now));
            }
        }
        return badges;
    }

    /**
     * Helper function for {@link #loadFeed} and {@link #getMyWhirled}.
     */
    protected List<FeedMessage> loadFeed (MemberRecord mrec, IntSet groupIds, int cutoffDays)
        throws PersistenceException
    {
        Timestamp since = new Timestamp(System.currentTimeMillis() - cutoffDays * 24*60*60*1000L);
        IntSet friendIds = _memberRepo.loadFriendIds(mrec.memberId);
        return _servletLogic.resolveFeedMessages(
            _feedRepo.loadPersonalFeed(mrec.memberId, friendIds, groupIds, since));
    }

    /** Helper for loadBadges */
    protected static class FilterByCategory
        implements Predicate<EarnedBadge>
    {
        public FilterByCategory (StampCategory category)
        {
            _category = category;
        }

        public boolean apply (EarnedBadge badge) {
            return BadgeType.getType(badge.badgeCode).getCategory().equals(_category);
        }

        protected StampCategory _category;
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected BadgeRepository _badgeRepo;
    @Inject protected BadgeLogic _badgeLogic;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;
}
