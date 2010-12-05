//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.server.BodyManager;
import com.threerings.crowd.server.PlaceRegistry;

import com.threerings.whirled.server.SceneSender;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatModifier;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;

import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;

import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthName;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserOccupantInfo;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.group.data.all.GroupMembership;

import com.threerings.msoy.item.data.all.Avatar.QuicklistState;
import com.threerings.msoy.item.server.ItemManager;

import com.threerings.msoy.notify.data.BadgeEarnedNotification;

import com.threerings.msoy.notify.server.MsoyNotificationManager;

import com.threerings.msoy.party.server.PartyRegistry;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MemberPlayerNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyPortal;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.server.RoomManager;

/**
 * Contains various member node actions.
 */
public class MemberNodeActions
{
    /**
     * Provides us with our peer manager reference. TODO: nix this and require callers to inject a
     * MemberNodeActions instance.
     */
    public static void init (MsoyPeerManager peerMan)
    {
        _peerMan = peerMan;
    }

    /**
     * Dispatches a notification that a member's info has changed to whichever server they are
     * logged into.
     * Any of these may be null to not set them.
     */
    public static void infoChanged (
        int memberId, String displayName, MediaDesc photo, String status)
    {
        _peerMan.invokeNodeAction(new InfoChanged(memberId, displayName, photo, status));
    }

    /**
     * Dispatches a notification that a member's privileges have changed to whichever server they
     * are logged into.
     */
    public static void tokensChanged (int memberId, MsoyTokenRing tokens)
    {
        _peerMan.invokeNodeAction(new TokensChanged(memberId, tokens));
    }

    /**
     * Updates the member's friends that are online with a new FriendEntry pulled out of the data
     * on the member object.
     */
    public static void updateFriendEntries (MemberObject memobj)
    {
        if (memobj.friends.size() == 0) {
            return;
        }
        _peerMan.invokeNodeAction(new FriendEntryUpdate(memobj));
    }

    /**
     * Send a mass-notification to all your friends.
     */
    public static void notifyAllFriends (MemberObject memobj, Notification notif)
    {
        if (memobj.friends.size() == 0) {
            return;
        }
        _peerMan.invokeNodeAction(new NotifyFriendsAction(memobj, notif));
    }

    /**
     * Dispatches a notification that a member's unread mail count has changed to whichever server
     * they are logged into.
     *
     * @param newMailCount a positive integer to set the absolute value, a negative integer to
     * adjust the current value down by the specified negative amount.
     */
    public static void reportUnreadMail (int memberId, int newMailCount)
    {
        _peerMan.invokeNodeAction(new ReportUnreadMail(memberId, newMailCount));
    }

    /**
     * Dispatches a notification that a member has joined the specified group to whichever server
     * they are logged into.
     */
    public static void joinedGroup (int memberId, GroupMembership gm)
    {
        _peerMan.invokeNodeAction(new JoinedGroup(memberId, gm));
    }

    /**
     * Dispatches a notification that a member has left the specified group to whichever server
     * they are logged into.
     */
    public static void leftGroup (int memberId, int groupId)
    {
        _peerMan.invokeNodeAction(new LeftGroup(memberId, groupId));
    }

    /**
     * Boots a member from any server into which they are logged in.
     */
    public static void bootMember (int memberId)
    {
        _peerMan.invokeNodeAction(new BootMember(memberId));
    }

    /**
     * Update a changed avatar.
     * @param validForTheme
     */
    public static void avatarUpdated (int memberId, int avatarId, QuicklistState state)
    {
        _peerMan.invokeNodeAction(new AvatarUpdated(memberId, avatarId, state));
    }

    /**
     * Act upon a deleted avatar.
     */
    public static void avatarDeleted (int memberId, int avatarId)
    {
        _peerMan.invokeNodeAction(new AvatarDeleted(memberId, avatarId));
    }

    /**
     * Send a notification to a member
     */
    public static void sendNotification (int memberId, Notification notification)
    {
        _peerMan.invokeNodeAction(new SendNotification(memberId, notification));
    }

    /**
     * Dispatches a notification that a member has updates their badgesVersion.
     */
    public static void updateBadgesVersion (int memberId, short badgesVersion)
    {
        _peerMan.invokeNodeAction(new BadgesVersionUpdated(memberId, badgesVersion));
    }

    /**
     * Dispatches a notification that a member has won a badge.
     */
    public static void badgeAwarded (EarnedBadgeRecord record)
    {
        _peerMan.invokeNodeAction(new BadgeAwarded(record));
    }

    /**
     * Dispatches a notification that an in-progress badge record has been updated.
     */
    public static void inProgressBadgeUpdated (InProgressBadgeRecord record)
    {
        _peerMan.invokeNodeAction(new InProgressBadgeUpdated(record));
    }

    /**
     * Update a member's StatSet.
     */
    public static <T extends Stat> void statUpdated (int memberId, StatModifier<T> modifier)
    {
        _peerMan.invokeNodeAction(new StatUpdated<T>(memberId, modifier));
    }

    /**
     * Adds an experience to the specified member's experiences set.
     */
    public static void addExperience (int memberId, byte action, int data)
    {
        _peerMan.invokeNodeAction(new AddExperienceAction(memberId, action, data));
    }

    /**
     * Sends an invite to the specified member to the specified party.
     */
    public static void inviteToParty (
        int memberId, MemberName inviter, int partyId, String partyName)
    {
        _peerMan.invokeNodeAction(new PartyInviteAction(memberId, inviter, partyId, partyName));
    }

    /**
     * Sends an invite to all friends of the supplied inviter to the specified party.
     */
    public static void inviteAllFriendsToParty (MemberObject inviter, int partyId, String partyName)
    {
        if (inviter.friends.size() == 0) {
            return;
        }
        _peerMan.invokeNodeAction(new AllFriendsPartyInviteAction(inviter, partyId, partyName));
    }

    /**
     * Notifies a follower that the leader is on the move (and potentially decouples this follower
     * from the leader if that turns out to be the right thing to do).
     */
    public static void followTheLeader (final int followerId, final int leaderId, int sceneId)
    {
        _peerMan.invokeNodeAction(new FollowTheLeaderAction(followerId, leaderId, sceneId),
                                  new Runnable() {
            public void run () {
                // didn't find the follower anywhere, remove them from the leader's follower set
                removeFollower(leaderId, followerId);
            }
        });
    }

    /**
     * Removes the specified follower from the specified leader's follower set.
     */
    public static void removeFollower (int leaderId, int followerId)
    {
        _peerMan.invokeNodeAction(new RemoveFollowerAction(leaderId, followerId));
    }

    /**
     * Forces the specified member to move to the specified scene. This simply sends a notification
     * to the client to instruct it to move to the specified scene. The scene move may still fail
     * for all the standard reasons.
     */
    public static void forcedMove (int memberId, int sceneId, MsoyLocation exit)
    {
        _peerMan.invokeNodeAction(new ForcedMoveAction(memberId, sceneId, exit));
    }

    /**
     * Notifies any active connections for this member that they have gained a level. Gratz!
     */
    public static void gainedLevel (int memberId, int newLevel)
    {
        _peerMan.invokeNodeAction(new LevelChanged(memberId, newLevel));
    }

    protected static class InfoChanged extends MemberNodeAction
    {
        /** Any field may be null to not change it. */
        public InfoChanged (int memberId, String displayName, MediaDesc photo, String status) {
            super(memberId);
            _displayName = displayName;
            _photo = photo;
            _status = status;
        }

        public InfoChanged () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (_status != null) {
                memobj.setHeadline(_status);
            }
            if (_displayName != null || _photo != null) {
                String name = (_displayName != null) ? _displayName : memobj.memberName.toString();
                memobj.updateDisplayName(name, _photo); // can cope with _photo == null.
                _bodyMan.updateOccupantInfo(
                    memobj, new OccupantInfo.NameUpdater(memobj.getVisibleName()));
            }

            // Update FriendEntrys on friend's member objects.  Rather than preparing a
            // MemberNodeAction for every friend, we use a custom NodeAction to check for servers
            // that contain at least one friend of this member, and do all the updating on that
            // server.  Note that we don't even take this potentially expensive step if this
            // member isn't logged in.
            updateFriendEntries(memobj); // always do this, we assume at least one thing changed!
        }

        protected String _displayName;
        protected MediaDesc _photo;
        protected String _status;

        @Inject protected transient BodyManager _bodyMan;
    }

    protected static class TokensChanged extends MemberPlayerNodeAction
    {
        public TokensChanged (int memberId, MsoyTokenRing tokens) {
            super(memberId);
            _tokens = tokens;
        }

        public TokensChanged () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.setTokens(_tokens);
            updateOccInfo(memobj);
        }

        @Override protected void execute (PlayerObject plobj) {
            plobj.setTokens(_tokens);
            updateOccInfo(plobj);
        }

        /**
         * Update the OccupantInfo of the specified body object.
         */
        protected void updateOccInfo (BodyObject body)
        {
            _bodyMan.updateOccupantInfo(body, new OccupantInfo.Updater<OccupantInfo>() {
                public boolean update (OccupantInfo info) {
                    return (info instanceof MsoyUserOccupantInfo) &&
                        ((MsoyUserOccupantInfo) info).updateTokens(_tokens);
                }
            });
        }

        protected MsoyTokenRing _tokens;

        @Inject protected transient BodyManager _bodyMan;
    }

    protected static class ReportUnreadMail extends MemberNodeAction
    {
        public ReportUnreadMail (int memberId, int newMailCount) {
            super(memberId);
            _newMailCount = newMailCount;
        }

        public ReportUnreadMail () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (_newMailCount < 0) {
                memobj.setNewMailCount(memobj.newMailCount + _newMailCount);
            } else if (memobj.newMailCount != _newMailCount) {
                memobj.setNewMailCount(_newMailCount);
            }
        }

        protected int _newMailCount;
    }

    protected static class JoinedGroup extends MemberNodeAction
    {
        public JoinedGroup (int memberId, GroupMembership gm) {
            super(memberId);
            _gm = gm;
        }

        public JoinedGroup () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.addToGroups(_gm);
        }

        protected GroupMembership _gm;
    }

    protected static class LeftGroup extends MemberNodeAction
    {
        public LeftGroup (int memberId, int groupId) {
            super(memberId);
            _groupId = groupId;
        }

        public LeftGroup () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.removeFromGroups(_groupId);
        }

        protected int _groupId;
    }

    protected static class BootMember extends MemberNodeAction
    {
        public BootMember (int memberId) {
            super(memberId);
        }

        public BootMember () {
        }

        @Override protected void execute (MemberObject memobj) {
            _memberMan.bootMember(_memberId);
        }

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class AvatarDeleted extends MemberNodeAction
    {
        public AvatarDeleted (int memberId, int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        public AvatarDeleted () {
        }

        @Override protected void execute (MemberObject memobj) {
            _itemMan.avatarDeletedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;

        @Inject protected transient ItemManager _itemMan;
    }

    protected static class AvatarUpdated extends MemberNodeAction
    {
        public AvatarUpdated (int memberId, int avatarId, QuicklistState validForTheme) {
            super(memberId);
            _avatarId = avatarId;
            _validForTheme = validForTheme;
        }

        public AvatarUpdated () {
        }

        @Override protected void execute (MemberObject memobj) {
            _itemMan.avatarUpdatedOnPeer(memobj, _avatarId, _validForTheme);
        }

        protected int _avatarId;
        protected QuicklistState _validForTheme;

        @Inject protected transient ItemManager _itemMan;
    }

    protected static class SendNotification extends MemberNodeAction
    {
        public SendNotification (int memberId, Notification notification) {
            super(memberId);
            _notification = notification;
        }

        public SendNotification () {
        }

        @Override protected void execute (MemberObject memobj) {
            _notifyMan.notify(memobj, _notification);
        }

        protected Notification _notification;

        @Inject protected transient MsoyNotificationManager _notifyMan;
    }

    protected static class BadgesVersionUpdated extends MemberNodeAction
    {
        public BadgesVersionUpdated (int memberId, short badgesVersion) {
            super(memberId);
            _badgesVersion = badgesVersion;
        }

        public BadgesVersionUpdated () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.getLocal(MemberLocal.class).badgesVersion = _badgesVersion;
        }

        protected short _badgesVersion;
    }

    protected static class BadgeAwarded extends MemberNodeAction
    {
        public BadgeAwarded (EarnedBadgeRecord record) {
            super(record.memberId);
            _badge = record.toBadge();
        }

        public BadgeAwarded () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (memobj.getLocal(MemberLocal.class).badgeAwarded(_badge)) {
                _notifyMan.notify(memobj, new BadgeEarnedNotification(_badge));
            }
        }

        protected EarnedBadge _badge;

        @Inject transient protected MsoyNotificationManager _notifyMan;
    }

    protected static class InProgressBadgeUpdated extends MemberNodeAction
    {
        public InProgressBadgeUpdated (InProgressBadgeRecord record) {
            super(record.memberId);
            _badge = record.toBadge();
        }

        public InProgressBadgeUpdated () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.getLocal(MemberLocal.class).inProgressBadgeUpdated(_badge);
        }

        protected InProgressBadge _badge;
    }

    protected static class StatUpdated<T extends Stat> extends MemberNodeAction
    {
        public StatUpdated (int memberId, StatModifier<T> modifier) {
            super(memberId);
            _modifier = modifier;
        }

        public StatUpdated () {
        }

        @Override protected void execute (MemberObject memobj) {
            memobj.getLocal(MemberLocal.class).stats.syncStat(_modifier);
        }

        protected StatModifier<T> _modifier;
    }

    /**
     * An action for all *online* friends.
     */
    protected static abstract class AllFriendsAction extends PeerManager.NodeAction
    {
        public AllFriendsAction () {}

        public AllFriendsAction (MemberObject memobj)
        {
            _friends = new int[memobj.friends.size()];
            int ii = 0;
            for (FriendEntry entry : memobj.friends) {
                _friends[ii++] = entry.name.getId();
            }
        }

        @Override public boolean isApplicable (NodeObject nodeobj)
        {
            MsoyNodeObject msoyNode = (MsoyNodeObject)nodeobj;
            for (int friendId : _friends) {
                if (msoyNode.clients.containsKey(MsoyAuthName.makeKey(friendId))) {
                    return true;
                }
            }
            // no friends found here, move along
            return false;
        }

        @Override protected void execute ()
        {
            for (int friendId : _friends) {
                MemberObject memobj = _locator.lookupMember(friendId);
                if (memobj != null) {
                    execute(memobj);
                }
            }
        }

        protected abstract void execute (MemberObject memobj);

        protected int[] _friends;

        /** Used to look up member objects. */
        @Inject protected transient MemberLocator _locator;
    }

    protected static class FriendEntryUpdate extends AllFriendsAction
    {
        public FriendEntryUpdate () {}

        public FriendEntryUpdate (MemberObject memobj)
        {
            super(memobj);

            _entry = new FriendEntry(memobj.memberName, memobj.headline);
        }

        @Override protected void execute (MemberObject memobj)
        {
            if (memobj.friends.containsKey(_entry.getKey())) {
                memobj.updateFriends(_entry);
            } else {
                memobj.addToFriends(_entry);
            }
        }

        protected FriendEntry _entry;
    }

    protected static class NotifyFriendsAction extends AllFriendsAction
    {
        public NotifyFriendsAction () {}

        public NotifyFriendsAction (MemberObject memobj, Notification notification)
        {
            super(memobj);
            _notification = notification;
        }

        @Override protected void execute (MemberObject memobj)
        {
            _notifyMan.notify(memobj, _notification);
        }

        protected Notification _notification;

        @Inject protected transient MsoyNotificationManager _notifyMan;
    }

    protected static class PartyInviteAction extends MemberNodeAction
    {
        public PartyInviteAction () {}

        public PartyInviteAction (
            int targetId, MemberName inviter, int partyId, String partyName)
        {
            super(targetId);
            _inviter = inviter;
            _partyId = partyId;
            _partyName = partyName;
        }

        @Override protected void execute (MemberObject memObj) {
            _partyReg.issueInvite(memObj, _inviter, _partyId, _partyName);
        }

        protected MemberName _inviter;
        protected int _partyId;
        protected String _partyName;
        @Inject protected transient PartyRegistry _partyReg;
    }

    protected static class AllFriendsPartyInviteAction extends AllFriendsAction
    {
        public AllFriendsPartyInviteAction () {}

        public AllFriendsPartyInviteAction (MemberObject inviter, int partyId, String partyName)
        {
            super(inviter);
            _inviter = inviter.memberName.toMemberName();
            _partyId = partyId;
            _partyName = partyName;
        }

        @Override protected void execute (MemberObject memObj) {
            _partyReg.issueInvite(memObj, _inviter, _partyId, _partyName);
        }

        protected MemberName _inviter;
        protected int _partyId;
        protected String _partyName;
        @Inject protected transient PartyRegistry _partyReg;
    }

    protected static class AddExperienceAction extends MemberNodeAction
    {
        public AddExperienceAction (int memberId, byte action, int data) {
            super(memberId);
            _action = action;
            _data = data;
        }

        public AddExperienceAction () {
        }

        @Override protected void execute (MemberObject memObj) {
            _memberMan.addExperience(memObj, new MemberExperience(new Date(), _action, _data));
        }

        protected byte _action;
        protected int _data;

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class FollowTheLeaderAction extends MemberNodeAction
    {
        public FollowTheLeaderAction (int memberId, int leaderId, int sceneId) {
            super(memberId);
            _sceneId = sceneId;
            _leaderId = leaderId;
        }

        public FollowTheLeaderAction () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (memobj.following == null || memobj.following.getId() != _leaderId) {
                // oops, no longer following this leader
                _peerMan.invokeNodeAction(
                    new RemoveFollowerAction(_leaderId, memobj.getMemberId()));
            } else {
                memobj.postMessage(RoomCodes.FOLLOWEE_MOVED, _sceneId);
            }
        }

        protected int _leaderId;
        protected int _sceneId;

        @Inject protected transient MsoyPeerManager _peerMan;
    }

    protected static class RemoveFollowerAction extends MemberNodeAction
    {
        public RemoveFollowerAction (int leaderId, int followerId) {
            super(leaderId);
            _followerId = followerId;
        }

        public RemoveFollowerAction () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (memobj.followers.containsKey(_followerId)) {
                memobj.removeFromFollowers(_followerId);
            }
        }

        protected int _followerId;
    }

    protected static class ForcedMoveAction extends MemberNodeAction
    {
        public ForcedMoveAction () {}

        public ForcedMoveAction (int memberId, int sceneId, MsoyLocation exit)
        {
            super(memberId);
            _sceneId = sceneId;
            _exit = exit;
        }

        @Override protected void execute (MemberObject memObj) {
            // if we have a desired exit location, look up the room manager and fake a
            // willTraversePortal call with a fake portal at our exit location
            if (_exit != null) {
                RoomManager rmgr = (RoomManager)_plreg.getPlaceManager(memObj.getPlaceOid());
                if (rmgr != null) {
                    MsoyPortal portal = new MsoyPortal();
                    portal.loc = _exit;
                    rmgr.willTraversePortal(memObj, portal);
                }
            }
            // now send the notification to the client to switch scenes

            ClientObject clobj = _clmgr.getClientObject(memObj.username);
            // sanity check
            if (!(clobj instanceof MemberClientObject)) {
                log.warning("Really expected a MemberClientObject here", "clobj", clobj);
            }
            SceneSender.forcedMove(clobj, _sceneId);
        }

        protected int _sceneId;
        protected MsoyLocation _exit;

        @Inject protected transient PlaceRegistry _plreg;
    }

    protected static class LevelChanged extends MemberNodeAction
    {
        public LevelChanged () {}

        public LevelChanged (int memberId, int newLevel)
        {
            super(memberId);
            _newLevel = newLevel;
        }

        @Override // from MemberNodeAction
        protected void execute (MemberObject memobj)
        {
            memobj.setLevel(_newLevel);
        }

        protected int _newLevel;
    }

    protected static MsoyPeerManager _peerMan;
}
