//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DuplicateKeyException;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.gwt.ABTest;
import com.threerings.msoy.admin.server.persist.ABTestRecord;
import com.threerings.msoy.admin.server.persist.ABTestRepository;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.world.data.MsoySceneModel;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

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
        throws PersistenceException
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
            _feedRepo.publishMemberMessage(caller.memberId, FeedMessageType.FRIEND_ADDED_FRIEND,
                                           friend.name.toString() + "\t" + friendId);

            // add them to the friends list of both parties if/wherever they are online
            MemberCard ccard = _memberRepo.loadMemberCard(caller.memberId);
            _peerMan.invokeNodeAction(new AddFriend(caller.memberId, friend));
            _peerMan.invokeNodeAction(new AddFriend(friendId, ccard));

            // note the happy event in the log
            _eventLog.friendAdded(caller.memberId, friendId);

        } catch (DuplicateKeyException dke) {
            // no problem, just fall through and pretend like things succeeded (we'll have skipped
            // all the announcing and stat fiddling and whatnot)

        } catch (PersistenceException pe) {
            log.warning("establishFriendship failed", "for", caller.memberId,
                        "friendId", friendId, pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Clears the friendship between the two specified parties.
     */
    public void clearFriendship (int removerId, int friendId)
        throws ServiceException
    {
        try {
            // clear their friendship in the database
            _memberRepo.clearFriendship(removerId, friendId);

            // remove them from the friends list of both parties, wherever they are online
            _peerMan.invokeNodeAction(new RemoveFriend(removerId, friendId));
            _peerMan.invokeNodeAction(new RemoveFriend(friendId, removerId));

            // note the sad event in the log
            _eventLog.friendRemoved(removerId, friendId);

        } catch (PersistenceException pe) {
            log.warning("removeFriend failed [for=" + removerId +
                        ", friendId=" + friendId + "].", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Return the a/b test group that a member or visitor belongs to for a given a/b test,
     * generated psudo-randomly based on their tracking ID and the test name.  If the visitor is
     * not eligible for the a/b test, return < 0.
     *
     * @param testName String identifier for the test
     * @param trackingTimestamp When the visitor was first assigned tracking information
     * @param affiliate String identifier for the visitor's affiliate (eg miniclip)
     * @param vector String identifier for the visitor's vector
     * @param creative String identifier for the visitor's creative
     * @param logEvent If true, track that this visitor was added to this group
     *
     * @return The a/b group the visitor has been assigned to, or < 0 for no group.
     */
    public int getABTestGroup (String testName, ReferralInfo info, boolean logEvent)
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
        } catch (PersistenceException pe) {
            log.warning("Failed to load A/B Test", "name", testName, pe);
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
        final int seed = Math.abs(new String(info.tracker + testName).hashCode());
        final int group = (seed % test.numGroups) + 1;

        // optionally log an event to say the group was assigned
        if (logEvent && group >= 0) {
            _eventLog.testAction(info.tracker, "ABTestGroupAssigned", testName, group);
        }

        return group;
    }

    /**
     * Return true if the visitor's attributes match those required by the given a/b test
     */
    protected boolean eligibleForABTest (ABTest test, ReferralInfo info)
    {
        // test runs only on new users and visitor is returning
        // (visitor may have been in a group during a previous session!)
        if (test.onlyNewVisitors == true && test.started.after(info.getCreationTime())) {
            return false;
        }

        // wrong affiliate
        if (test.affiliate != null && test.affiliate.length() > 0
                && (info.affiliate == null || !info.affiliate.trim().equals(test.affiliate))) {
            return false;
        }

        // wrong vector
        if (test.vector != null && test.vector.length() > 0
                && (info.vector == null || !info.vector.trim().equals(test.vector))) {
            return false;
        }

        // wrong creative
        if (test.creative != null && test.creative.length() > 0
                && (info.creative == null || !info.creative.trim().equals(test.creative))) {
            return false;
        }

        return true;
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

        protected void execute (final MemberObject memobj) {
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

        protected void execute (MemberObject memobj) {
            memobj.removeFromFriends(_friendId);
            _friendMan.clearFriendInterest(memobj, _friendId);
        }

        protected int _friendId;

        @Inject protected transient FriendManager _friendMan;
    }

    // dependencies
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected StatLogic _statLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected MsoyEventLogger _eventLog;
}
