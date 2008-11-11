//
// $Id$

package com.threerings.msoy.server;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DuplicateKeyException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.BasicNavItemData;
import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.NavItemData;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.persist.MemberExperienceRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.BadgeLogic;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.GameRepository;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains member related services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class MemberLogic
{
    /**
     * Looks up the home scene id of the specified entity (member or group).
     *
     * @return the id if the entity was found, null otherwise.
     */
    public Integer getHomeId (byte ownerType, int ownerId)
    {
        switch (ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            MemberRecord member = _memberRepo.loadMember(ownerId);
            return (member == null) ? null : member.homeSceneId;

        case MsoySceneModel.OWNER_TYPE_GROUP:
            GroupRecord group = _groupRepo.loadGroup(ownerId);
            return (group == null) ? null : group.homeSceneId;

        default:
            log.warning("Unknown ownerType provided to getHomeId", "ownerType", ownerType,
                        "ownerId", ownerId);
            return null;
        }
    }

    /**
     * Establishes a friendship between the supplied two members. This handles updating the
     * respective members' stats, publishing to the feed and notifying the dobj runtime system.
     */
    public void establishFriendship (MemberRecord caller, int friendId)
        throws ServiceException
    {
        try {
            MemberCard friend = _memberRepo.noteFriendship(caller.memberId, friendId);
            if (friend == null) {
                throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
            }

            // update the FRIENDS_MADE statistic for both players
            _statLogic.incrementStat(caller.memberId, StatType.FRIENDS_MADE, 1);
            _statLogic.incrementStat(friendId, StatType.FRIENDS_MADE, 1);

            // publish a message to the inviting member's feed
            String data = friend.name.toString() + "\t" + friendId + "\t"
                + MediaDesc.mdToString(friend.photo);
            _feedRepo.publishMemberMessage(
                caller.memberId, FeedMessageType.FRIEND_ADDED_FRIEND, data);

            // add them to the friends list of both parties if/wherever they are online
            MemberCard ccard = _memberRepo.loadMemberCard(caller.memberId);
            _peerMan.invokeNodeAction(new AddFriend(caller.memberId, friend));
            _peerMan.invokeNodeAction(new AddFriend(friendId, ccard));

            // note the happy event in the log
            _eventLog.friendAdded(caller.memberId, friendId);

        } catch (DuplicateKeyException dke) {
            // no problem, just fall through and pretend like things succeeded (we'll have skipped
            // all the announcing and stat fiddling and whatnot)
        }
    }

    /**
     * Clears the friendship between the two specified parties.
     */
    public void clearFriendship (int removerId, int friendId)
        throws ServiceException
    {
        // clear their friendship in the database
        _memberRepo.clearFriendship(removerId, friendId);

        // remove them from the friends list of both parties, wherever they are online
        _peerMan.invokeNodeAction(new RemoveFriend(removerId, friendId));
        _peerMan.invokeNodeAction(new RemoveFriend(friendId, removerId));

        // update the FRIENDS_MADE statistic for both players
        _statLogic.incrementStat(removerId, StatType.FRIENDS_MADE, -1);
        _statLogic.incrementStat(friendId, StatType.FRIENDS_MADE, -1);

        // note the sad event in the log
        _eventLog.friendRemoved(removerId, friendId);
    }

    /**
     * Return the a/b test group that a member or visitor belongs to for a given a/b test,
     * generated psudo-randomly based on their tracking ID and the test name.  If the visitor is
     * not eligible for the a/b test, return < 0.
     *
     * @param testName String identifier for the test
     * @param logEvent If true, track that this visitor was added to this group
     *
     * @return The a/b group the visitor has been assigned to, or < 0 for no group.
     */
    public int getABTestGroup (String testName, VisitorInfo info, boolean logEvent)
    {
        if (info == null) { // sanity check
            log.warning("Received bogus AB test group request", "name", testName, "info", info,
                        "logEvent", logEvent);
            return -1;
        }

        ABTest test = null;
        try {
            ABTestRecord record = _testRepo.loadTestByName(testName);
            if (record == null) {
                log.warning("Unknown A/B Test in getABTestGroup", "name", testName);
                return -1;
            }
            test = record.toABTest();
        } catch (Exception e) {
            log.warning("Failed to load A/B Test", "name", testName, e);
            return -1;
        }

        // test is not running
        if (test.enabled == false) {
            return -1;
        }

        // do affiliate, etc match the requirements for the test
        if (!eligibleForABTest(test, info)) {
            return -1;
        }

        // generate the group number based on trackingID + testName
        final int seed = Math.abs(new String(info.id + testName).hashCode());
        final int group = (seed % test.numGroups) + 1;

        // optionally log an event to say the group was assigned
        if (logEvent && group >= 0) {
            _eventLog.testAction(info.id, "ABTestGroupAssigned", testName, group);
        }

        return group;
    }

    /**
     * Retrieves the last of recent experiences for this member.
     */
    public List<MemberExperience> getExperiences (int memberId)
    {
        return Lists.transform(_memberRepo.getExperiences(memberId),
                new Function<MemberExperienceRecord, MemberExperience>() {
            public MemberExperience apply (MemberExperienceRecord expRecord) {
                // Depending on the action type, convert data to the correct object.
                final int actionData;
                switch (expRecord.action) {
                case HomePageItem.ACTION_ROOM:
                case HomePageItem.ACTION_GAME:
                    actionData = Integer.parseInt(expRecord.data);
                    break;
                default:
                    actionData = 0;
                }
                return new MemberExperience(expRecord.dateOccurred, expRecord.action, actionData);
            }
        });
    }

    /**
     * Saves the member's experiences, clearing out any old ones.
     *
     * @param memberId ID of the member whose experiences are being saved.
     * @param experiences The experiences to save.
     */
    public void saveExperiences (final int memberId, List<MemberExperience> experiences)
    {
        _memberRepo.deleteExperiences(memberId);
        List<MemberExperienceRecord> records = Lists.newArrayList();
        for (MemberExperience mexp : experiences) {
            final String actionData;
            switch (mexp.action) {
            case HomePageItem.ACTION_ROOM:
            case HomePageItem.ACTION_GAME:
                actionData = Integer.toString(mexp.data);
                break;
            default:
                continue; // we don't yet handle this type of experience
            }
            records.add(new MemberExperienceRecord(memberId, mexp.getDateOccurred(),
                                                   mexp.action, actionData));
        }
        _memberRepo.saveExperiences(records);
    }

    /**
     * Loads up the specified member's home page grid items.
     */
    public HomePageItem[] getHomePageGridItems (final MemberObject memObj)
    {
        HomePageItem[] items = new HomePageItem[MWP_COUNT];
        int curItem = 0;

        // The first item on the home page is always a whirled tour unless already onTour
        if (!memObj.onTour) {
            items[curItem++] = EXPLORE_ITEM;
        }

        // The next 2 or 3 items are badges
        List<InProgressBadge> badges = _badgeLogic.getNextSuggestedBadges(
            memObj.getMemberId(), memObj.getLocal(MemberLocal.class).badgesVersion,
            3 - curItem);
        for (InProgressBadge badge : badges) {
            items[curItem++] = new HomePageItem(
                HomePageItem.ACTION_BADGE, badge, badge.imageMedia());
        }

        // The last 6 are determined by the user-specific home page items, depending on
        // where they were last in Whirled.
        Set<Integer> haveRooms = Sets.newHashSet();
        Set<Integer> haveGames = Sets.newHashSet();
        for (HomePageItem item : getHomePageItems(memObj, 6)) {
            items[curItem++] = item;
            int id = ((BasicNavItemData)item.getNavItemData()).getId();
            if (item.getAction() == HomePageItem.ACTION_ROOM) {
                haveRooms.add(id);
            } else if (item.getAction() == HomePageItem.ACTION_GAME) {
                haveGames.add(id);
            }
        }

        // If there are still not enough places, fill in with some currently popular places.
        PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
        if (curItem < items.length) {
            // Half will be games, the other half rooms.
            int roomLimit = curItem + (items.length - curItem) / 2;
            // TODO: This is similar to some code in GalaxyServlet and GameServlet. refactor?
            for (PopularPlacesSnapshot.Place place : pps.getTopScenes()) {
                if (!haveRooms.contains(place.placeId)) {
                    SceneRecord scene = _sceneRepo.loadScene(place.placeId);
                    MediaDesc media = scene.getSnapshot();
                    if (media == null) {
                        media = DEFAULT_ROOM_SNAPSHOT;
                    }
                    items[curItem++] = new HomePageItem(
                        HomePageItem.ACTION_ROOM,
                        new BasicNavItemData(place.placeId, place.name), media);
                    haveRooms.add(place.placeId);
                }
                if (curItem >= roomLimit) {
                    break;
                }
            }
        }

        // Add the top active games.
        if (curItem < items.length) {
            for (PopularPlacesSnapshot.Place place : pps.getTopGames()) {
                if (!haveGames.contains(place.placeId)) {
                    GameRecord game = _msoyGameRepo.loadGameRecord(place.placeId);
                    items[curItem++] = new HomePageItem(
                        HomePageItem.ACTION_GAME, new BasicNavItemData(game.gameId, game.name),
                        game.getThumbMediaDesc());
                    haveGames.add(game.gameId);
                }
                if (curItem == items.length) {
                    break;
                }
            }
        }

        // If we don't have enough games, pull from the list of all games.
        if (curItem < items.length) {
            for (GameRecord game : _gameRepo.loadGenre((byte)-1, items.length)) {
                if (!haveGames.contains(game.gameId)) {
                    items[curItem++] = new HomePageItem(
                        HomePageItem.ACTION_GAME, new BasicNavItemData(game.gameId, game.name),
                        game.getThumbMediaDesc());
                }
                if (curItem == items.length) {
                    break;
                }
            }
        }

        // If there still aren't enough places, fill in with null objects.
        while (curItem < items.length) {
            items[curItem++] = new HomePageItem(HomePageItem.ACTION_NONE, null, null);
        }

        return items;
    }

    /**
     * Return true if the visitor's attributes match those required by the given a/b test
     */
    protected boolean eligibleForABTest (ABTest test, VisitorInfo info)
    {
        // test runs only on new users and visitor is returning
        // (visitor may have been in a group during a previous session!)
        if (test.onlyNewVisitors == true && test.started.after(info.getCreationTime())) {
            return false;
        }
        return true;
    }

    /**
     * Retrieves a list of experiences to be displayed on the home page.  Each experience the
     * member has had recently will be given a weighted score to determine the order of the
     * experience.  Only the number of experiences requested will be returned as home page items.
     * If there are not enough experiences, or the experiences have a low score (too old, etc.),
     * they will not be included here.
     *
     * @param memObj Member object to get home page items for
     * @param count Number of home page items to retrieve.
     * @return List of the home page items.
     */
    protected List<HomePageItem> getHomePageItems (MemberObject memObj, int count)
    {
        List<ScoredExperience> scores = Lists.newArrayList();
        for (MemberExperience experience : memObj.experiences) {
            ScoredExperience newExp = new ScoredExperience(experience);

            // Has this member experienced this more than once?  If so, combine.
            for (Iterator<ScoredExperience> itor = scores.iterator(); itor.hasNext(); ) {
                ScoredExperience thisExp = itor.next();
                if (thisExp.isSameExperience(newExp)) {
                    newExp = new ScoredExperience(newExp, thisExp);
                    itor.remove();
                    break;
                }
            }

            scores.add(newExp);
        }

        // Sort by scores (highest score first), limit it to count, and return the list.
        Collections.sort(scores, new Comparator<ScoredExperience>() {
            public int compare (ScoredExperience exp1, ScoredExperience exp2) {
                return (exp1.score > exp2.score) ? -1 : ((exp1.score < exp2.score) ? 1 : 0);
            }
        });

        // Convert our scored experiences to home page items.
        List<HomePageItem> items = Lists.newArrayList();
        for (ScoredExperience se : scores) {
            MediaDesc media;
            final NavItemData data;
            switch (se.experience.action) {
            case HomePageItem.ACTION_ROOM: {
                SceneRecord scene = _sceneRepo.loadScene(se.experience.data);
                if (scene == null) {
                    continue;
                }
                media = scene.getSnapshot();
                if (media == null) {
                    media = DEFAULT_ROOM_SNAPSHOT;
                }
                data = new BasicNavItemData(se.experience.data, scene.name);
                break;
            }
            case HomePageItem.ACTION_GAME:
                GameRecord game = _msoyGameRepo.loadGameRecord(se.experience.data);
                if (game == null) {
                    continue;
                }
                media = game.getThumbMediaDesc();
                data = new BasicNavItemData(se.experience.data, game.name);
                break;
            default:
                // if we have no data, our caller will freak out, so skip this experience
                continue;
            }
            items.add(se.experience.getHomePageItem(media, data));
            if (items.size() == count) {
                break; // stop when we reach our desired count
            }
        }
        return items;
    }

    /**
     * A member experience that has been scored.
     *
     * @author Kyle Sampson <kyle@threerings.net>
     */
    protected static class ScoredExperience
    {
        public final MemberExperience experience;
        public final float score;

        /**
         * Creates a scored experience based on the information from the given
         * {@link MemberExperienceRecord}.
         */
        public ScoredExperience (MemberExperience experience)
        {
            this.experience = experience;

            // The score for a standard record starts at 14 and decrements by 1 for every day
            // since the experience occurred.  Cap at 0; thus, anything older than 2 weeks has
            // the same score.
            float newScore = 14f -
                (System.currentTimeMillis() - experience.dateOccurred) /
                (1000f * 60f * 60f * 24f);
            score = (newScore < 0) ? 0f : newScore;
        }

        /**
         * Combines two identical (i.e., {@link #isSameExperience(ScoredExperience)} returns true})
         * scored experiences into one, combining their scores.
         */
        public ScoredExperience (ScoredExperience exp1, ScoredExperience exp2)
        {
            experience = exp1.experience;   // exp2.item should be the same.
            score = exp1.score + exp2.score;    // Both scores positive
        }

        /**
         * Null experience
         */
        public ScoredExperience ()
        {
            experience = new MemberExperience(new Date(), HomePageItem.ACTION_NONE, 0);
            score = 0f;
        }

        /**
         * Returns true if the given scored experience represents the same experience as this one.
         * They may have different scores, but this indicates the user did the same thing twice.
         */
        public boolean isSameExperience (ScoredExperience other)
        {
            return this.experience.action == other.experience.action &&
                this.experience.data == other.experience.data;
        }
    }

    protected static class AddFriend extends MemberNodeAction
    {
        public AddFriend (int memberId, MemberCard friend) {
            super(memberId);
            _friendId = friend.name.getMemberId();
            _friendName = friend.name.toString();
            _friendPhoto = friend.photo;
            _friendStatus = friend.headline;
        }

        public AddFriend () {
        }

        @Override protected void execute (final MemberObject memobj) {
            MemberName friend = new MemberName(_friendName, _friendId);
            boolean online = (_peerMan.locateClient(friend) != null);
            memobj.addToFriends(new FriendEntry(friend, online, _friendPhoto, _friendStatus));
            _friendMan.registerFriendInterest(memobj, _friendId);
        }

        protected int _friendId;
        protected String _friendName;
        protected MediaDesc _friendPhoto;
        protected String _friendStatus;

        @Inject protected transient MsoyPeerManager _peerMan;
        @Inject protected transient FriendManager _friendMan;
    }

    protected static class RemoveFriend extends MemberNodeAction
    {
        public RemoveFriend (int memberId, int friendId) {
            super(memberId);
            _friendId = friendId;
        }

        public RemoveFriend () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.removeFromFriends(_friendId);
            _friendMan.clearFriendInterest(memobj, _friendId);
        }

        protected int _friendId;

        @Inject protected transient FriendManager _friendMan;
    }

    // dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected MemberManager _memberMan;
    @Inject protected StatLogic _statLogic;
    @Inject protected BadgeLogic _badgeLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected MsoyGameRepository _msoyGameRepo;
    @Inject protected GameRepository _gameRepo;

    /** The whirled tour home page item. */
    protected static final HomePageItem EXPLORE_ITEM = new HomePageItem(
        HomePageItem.ACTION_EXPLORE, null, new StaticMediaDesc(
            MediaDesc.IMAGE_PNG, "icon", "home_page_tour"));

    /** Static media descriptor for the default room snapshot. */
    protected static final MediaDesc DEFAULT_ROOM_SNAPSHOT = new StaticMediaDesc(
        // It's not obvious from the docs in StaticMediaDesc that you can do this,
        // but this is what Group.getDefaultGroupLogoMedia() does.
        MediaDesc.IMAGE_JPEG, "snapshot", "default_t",
        // we know that we're 66x60
        MediaDesc.HALF_VERTICALLY_CONSTRAINED);

    /** The number of slots we have in My Whired Places. */
    protected static final int MWP_COUNT = 9;
}
