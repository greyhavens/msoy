//
// $Id$

package com.threerings.msoy.person.server;

import java.sql.Timestamp;
import java.util.List;

import com.google.common.collect.Lists;
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
            log.warning("getMyWhirled failed [for=" + mrec.memberId + "]", pe);
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
            log.warning("Load rooms failed [memberId=" + mrec.memberId + "]", pe);
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
            log.warning("Load feed failed [memberId=" + mrec.memberId + "]", pe);
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
        return data;
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

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected ServletLogic _servletLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected MsoySceneRepository _sceneRepo;

    protected static final int TARGET_MYWHIRLED_GAMES = 6;
    protected static final int DEFAULT_FEED_DAYS = 2;
}
