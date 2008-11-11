//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;
import com.threerings.presents.peer.server.PeerManager;

import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatModifier;

import com.threerings.msoy.data.MemberExperience;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.notify.data.BadgeEarnedNotification;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.room.data.RoomCodes;

/**
 * Contains various member node actions.
 */
public class MemberNodeActions
{
    /**
     * Provides us with our peer manager reference. TODO: nix this and require callers to inject a
     * MemberNodeActions instance.
     */
    public static void init (final MsoyPeerManager peerMan)
    {
        _peerMan = peerMan;
    }

    /**
     * Dispatches a notification that a member's info has changed to whichever server they are
     * logged into.
     */
    public static void infoChanged (
        final int memberId, final String displayName, final MediaDesc photo, final String status)
    {
        _peerMan.invokeNodeAction(new InfoChanged(memberId, displayName, photo, status));
    }

    /**
     * Dispatches a notification that a member's privileges have changed to whichever server they
     * are logged into.
     */
    public static void tokensChanged (final int memberId, MsoyTokenRing tokens)
    {
        _peerMan.invokeNodeAction(new TokensChanged(memberId, tokens));
    }

    /**
     * Updates the member's friends that are online with a new FriendEntry pulled out of the data
     * on the member object.
     */
    public static void updateFriendEntries (final MemberObject memobj)
    {
        final int[] friends = new int[memobj.friends.size()];
        int ii = 0;
        for (final FriendEntry entry : memobj.friends) {
            friends[ii++] = entry.name.getMemberId();
        }
        _peerMan.invokeNodeAction(
            new FriendEntryUpdate(friends, memobj.getMemberId(), memobj.memberName.toString(),
                                  memobj.memberName.getPhoto(), memobj.headline));
    }

    /**
     * Dispatches a notification that a member's unread mail count has changed to whichever server
     * they are logged into.
     *
     * @param newMailCount a positive integer to set the absolute value, a negative integer to
     * adjust the current value down by the specified negative amount.
     */
    public static void reportUnreadMail (final int memberId, final int newMailCount)
    {
        _peerMan.invokeNodeAction(new ReportUnreadMail(memberId, newMailCount));
    }

    /**
     * Dispatches a notification that a member has joined the specified group to whichever server
     * they are logged into.
     */
    public static void joinedGroup (final int memberId, final GroupMembership gm)
    {
        _peerMan.invokeNodeAction(new JoinedGroup(memberId, gm));
    }

    /**
     * Dispatches a notification that a member has left the specified group to whichever server
     * they are logged into.
     */
    public static void leftGroup (final int memberId, final int groupId)
    {
        _peerMan.invokeNodeAction(new LeftGroup(memberId, groupId));
    }

    /**
     * Boots a member from any server into which they are logged in.
     */
    public static void bootMember (final int memberId)
    {
        _peerMan.invokeNodeAction(new BootMember(memberId));
    }

    /**
     * Update a changed avatar.
     */
    public static void avatarUpdated (final int memberId, final int avatarId)
    {
        _peerMan.invokeNodeAction(new AvatarUpdated(memberId, avatarId));
    }

    /**
     * Act upon a deleted avatar.
     */
    public static void avatarDeleted (final int memberId, final int avatarId)
    {
        _peerMan.invokeNodeAction(new AvatarDeleted(memberId, avatarId));
    }

    /**
     * Send a notification to a member
     */
    public static void sendNotification (final int memberId, final Notification notification)
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
    public static void badgeAwarded (final EarnedBadgeRecord record)
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

    protected static class InfoChanged extends MemberNodeAction
    {
        public InfoChanged (
            int memberId, String displayName, MediaDesc photo, String status) {
            super(memberId);
            _displayName = displayName;
            _photo = photo;
            _status = status;
        }

        public InfoChanged () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.updateDisplayName(_displayName, _photo);
            memobj.setHeadline(_status);
            _memberMan.updateOccupantInfo(memobj);

            // Update FriendEntrys on friend's member objects.  Rather than preparing a
            // MemberNodeAction for every friend, we use a custom NodeAction to check for servers
            // that contain at least one friend of this member, and do all the updating on that
            // server.  Note that we don't even take this potentially expensive step if this
            // member isn't logged in.
            updateFriendEntries(memobj);
        }

        protected String _displayName;
        protected MediaDesc _photo;
        protected String _status;

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class TokensChanged extends MemberNodeAction
    {
        public TokensChanged (
            int memberId, MsoyTokenRing tokens) {
            super(memberId);
            _tokens = tokens;
        }

        public TokensChanged () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.setTokens(_tokens);
            _memberMan.updateOccupantInfo(memobj);
        }

        protected MsoyTokenRing _tokens;

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class ReportUnreadMail extends MemberNodeAction
    {
        public ReportUnreadMail (final int memberId, final int newMailCount) {
            super(memberId);
            _newMailCount = newMailCount;
        }

        public ReportUnreadMail () {
        }

        @Override protected void execute (final MemberObject memobj) {
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
        public JoinedGroup (final int memberId, final GroupMembership gm) {
            super(memberId);
            _gm = gm;
        }

        public JoinedGroup () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.addToGroups(_gm);
        }

        protected GroupMembership _gm;
    }

    protected static class LeftGroup extends MemberNodeAction
    {
        public LeftGroup (final int memberId, final int groupId) {
            super(memberId);
            _groupId = groupId;
        }

        public LeftGroup () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.removeFromGroups(_groupId);
        }

        protected int _groupId;
    }

    protected static class BootMember extends MemberNodeAction
    {
        public BootMember (final int memberId) {
            super(memberId);
        }

        public BootMember () {
        }

        @Override protected void execute (final MemberObject memobj) {
            _memberMan.bootMember(_memberId);
        }

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class AvatarDeleted extends MemberNodeAction
    {
        public AvatarDeleted (final int memberId, final int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        public AvatarDeleted () {
        }

        @Override protected void execute (final MemberObject memobj) {
            _itemMan.avatarDeletedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;

        @Inject protected transient ItemManager _itemMan;
    }

    protected static class AvatarUpdated extends MemberNodeAction
    {
        public AvatarUpdated (final int memberId, final int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        public AvatarUpdated () {
        }

        @Override protected void execute (final MemberObject memobj) {
            _itemMan.avatarUpdatedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;

        @Inject protected transient ItemManager _itemMan;
    }

    protected static class SendNotification extends MemberNodeAction
    {
        public SendNotification (final int memberId, final Notification notification) {
            super(memberId);
            _notification = notification;
        }

        public SendNotification () {
        }

        @Override protected void execute (final MemberObject memobj) {
            _notifyMan.notify(memobj, _notification);
        }

        protected Notification _notification;

        @Inject protected transient NotificationManager _notifyMan;
    }

    protected static class BadgesVersionUpdated extends MemberNodeAction
    {
        public BadgesVersionUpdated (int memberId, short badgesVersion) {
            super(memberId);
            _badgesVersion = badgesVersion;
        }

        public BadgesVersionUpdated () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.getLocal(MemberLocal.class).badgesVersion = _badgesVersion;
        }

        protected short _badgesVersion;
    }

    protected static class BadgeAwarded extends MemberNodeAction
    {
        public BadgeAwarded (final EarnedBadgeRecord record) {
            super(record.memberId);
            _badge = record.toBadge();
        }

        public BadgeAwarded () {
        }

        @Override protected void execute (final MemberObject memobj) {
            if (memobj.getLocal(MemberLocal.class).badgeAwarded(_badge)) {
                _notifyMan.notify(memobj, new BadgeEarnedNotification(_badge));
            }
        }

        protected EarnedBadge _badge;

        @Inject transient protected NotificationManager _notifyMan;
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
        public StatUpdated (final int memberId, final StatModifier<T> modifier) {
            super(memberId);
            _modifier = modifier;
        }

        public StatUpdated () {
        }

        @Override protected void execute (final MemberObject memobj) {
            memobj.getLocal(MemberLocal.class).stats.syncStat(_modifier);
        }

        protected StatModifier<T> _modifier;
    }

    protected static class FriendEntryUpdate extends PeerManager.NodeAction
    {
        public FriendEntryUpdate (int[] friends, int memberId, String displayName, MediaDesc photo,
                                  String status) {
            _friends = friends;
            _memberId = memberId;
            _displayName = displayName;
            _photo = photo;
            _status = status;
        }

        public FriendEntryUpdate () {
        }

        @Override public boolean isApplicable (final NodeObject nodeobj)
        {
            final MsoyNodeObject msoyNode = (MsoyNodeObject)nodeobj;
            for (final int friendId : _friends) {
                if (msoyNode.clients.containsKey(MemberName.makeKey(friendId))) {
                    return true;
                }
            }
            // no friends found here, move along
            return false;
        }

        @Override protected void execute ()
        {
            final FriendEntry entry = new FriendEntry(
                new MemberName(_displayName, _memberId), true, _photo, _status);
            for (final int friendId : _friends) {
                final MemberObject memobj = _locator.lookupMember(friendId);
                if (memobj != null) {
                    memobj.updateFriends(entry);
                }
            }
        }

        protected int[] _friends;
        protected int _memberId;
        protected String _displayName;
        protected MediaDesc _photo;
        protected String _status;

        /** Used to look up member objects. */
        @Inject protected transient MemberLocator _locator;
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

        protected /* final */ byte _action;
        protected /* final */ int _data;

        @Inject protected transient MemberManager _memberMan;
    }

    protected static class FollowTheLeaderAction extends MemberNodeAction
    {
        public FollowTheLeaderAction (int memberId, int leaderId, int sceneId) {
            super(memberId);
            _sceneId = sceneId;
        }

        public FollowTheLeaderAction () {
        }

        @Override protected void execute (MemberObject memobj) {
            if (memobj.following == null || memobj.following.getMemberId() != _leaderId) {
                // oops, no longer following this leader
                _peerMan.invokeNodeAction(new RemoveFollowerAction(_leaderId, memobj.getMemberId()));
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
            memobj.removeFromFollowers(_followerId);
        }
        
        protected int _followerId;
    }

    protected static MsoyPeerManager _peerMan;
}
