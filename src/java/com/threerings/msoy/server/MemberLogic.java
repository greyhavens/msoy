//
// $Id$

package com.threerings.msoy.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.DuplicateKeyException;

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

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.persist.MemberExperienceRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.room.data.MsoySceneModel;

import static com.threerings.msoy.Log.log;

/**
 * Contains member related services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class MemberLogic
{
    public static final int MAX_EXPERIENCES = 20;
    
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
     * Adds a member experience, removing old ones if necessary.
     * 
     * @param memberId ID of the member who had the experience.
     * @param action Action number that occurred.  See {@link HomePageItem}.
     * @param data Associated data for the action.
     */
    public void addExperience (int memberId, byte action, String data)
    {
        // This will be a check-and-set operation subject to race conditions.  In the worst case
        // scenario, the member will have extra experiences stored beyond the MAX_EXPERIENCES
        // defined.  This is acceptable though, since it will not affect the calculation 
        // significantly.
        List<MemberExperienceRecord> experiences = _memberRepo.getExperiences(memberId);
        if (experiences.size() >= MAX_EXPERIENCES) {
            _memberRepo.deleteExperience(experiences.get(0));
        }
        
        _memberRepo.addExperience(new MemberExperienceRecord(memberId, action, data));
    }
    
    /**
     * Retrieves a list of experiences to be displayed on the home page.  Each experience the
     * member has had recently will be given a weighted score to determine the order of the
     * experience.  Only the number of experiences requested will be returned as home page
     * items.  If there are not enough experiences, or the experiences have a low score
     * (too old, etc.), then a set of experiences designed to show the player around Whirled will
     * be returned.
     * 
     * @param memberId ID of the member to retrieve home page items for.
     * @param count Number of home page items to retrieve.
     * @return List of the home page items.  This will always have precisely the number of items
     * requested.
     */
    public List<HomePageItem> getHomePageItems (int memberId, int count)
    {
        List<ScoredExperience> scores = new ArrayList<ScoredExperience>();
        for (MemberExperienceRecord experience : _memberRepo.getExperiences(memberId)) {
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
        
        // TODO: Add default experiences.
        for (int i = scores.size(); i < count; i++) {
            scores.add(new ScoredExperience());
        }
        
        // Sort by scores (highest score first), limit it to count, and return the list.
        Collections.sort(scores, new Comparator<ScoredExperience>() {
            public int compare (ScoredExperience exp1, ScoredExperience exp2) {
                return (exp1.score > exp2.score) ? -1 : ((exp1.score < exp2.score) ? 1 : 0);
            }
        });
        while (scores.size() > count) {
            scores.remove(scores.size() - 1);
        }
        return Lists.transform(scores, new Function<ScoredExperience, HomePageItem>() {
            public HomePageItem apply (ScoredExperience experience) {
                return experience.item;
            }
        });
    }
    
    /**
     * A member experience that has been scored.
     * 
     * @author Kyle Sampson <kyle@threerings.net>
     */
    protected static class ScoredExperience
    {
        public final HomePageItem item;
        public final float score;
        
        /**
         * Creates a scored experience based on the information from the given 
         * {@link MemberExperienceRecord}.
         */
        public ScoredExperience (MemberExperienceRecord experience)
        {
            item = experience.getHomePageItem();
            
            // The score for a standard record starts at 14 and decrements by 1 for every day
            // since the experience occurred.  Cap at 0; thus, anything older than 2 weeks has
            // the same score.
            float newScore = 14f - 
                (float)(System.currentTimeMillis() - experience.dateOccurred.getTime()) / 
                (1000f * 60f * 60f * 24f);
            score = (newScore < 0) ? 0f : newScore;
        }
        
        /**
         * Combines two identical (i.e., {@link #isSameExperience(ScoredExperience)} returns true})
         * scored experiences into one, combining their scores.
         */
        public ScoredExperience (ScoredExperience exp1, ScoredExperience exp2)
        {
            item = exp1.item;   // exp2.item should be the same.
            score = exp1.score + exp2.score;    // Both scores positive
        }
        
        /**
         * Null experience
         */
        public ScoredExperience ()
        {
            item = new HomePageItem(HomePageItem.ACTION_NONE, null, null);
            score = 0f;
        }

        /**
         * Returns true if the given scored experience represents the same experience as this one.
         * They may have different scores, but this indicates the user did the same thing twice.
         */
        public boolean isSameExperience (ScoredExperience other)
        {
            return this.item.getAction() == other.item.getAction() &&
                this.item.getActionData().equals(other.item.getActionData());
        }
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
    @Inject protected MsoyPeerManager _peerMan;
    @Inject protected StatLogic _statLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected ABTestRepository _testRepo;
    @Inject protected MsoyEventLogger _eventLog;
}
