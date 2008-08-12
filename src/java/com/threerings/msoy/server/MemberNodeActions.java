//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;

import com.threerings.presents.peer.server.PeerManager;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatModifier;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.server.BadgeManager;
import com.threerings.msoy.badge.server.persist.EarnedBadgeRecord;
import com.threerings.msoy.badge.server.persist.InProgressBadgeRecord;
import com.threerings.msoy.chat.server.ChatChannelManager;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.notify.data.Notification;
import com.threerings.msoy.notify.server.NotificationManager;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MemberNodeAction;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberFlowRecord;

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
     */
    public static void infoChanged (
        int memberId, String displayName, MediaDesc photo, String status)
    {
        _peerMan.invokeNodeAction(new InfoChanged(memberId, displayName, photo, status));
    }

    /**
     * Updates the member's friends that are online with a new FriendEntry pulled out of the data
     * on the member object.
     */
    public static void updateFriendEntries (MemberObject memobj)
    {
        int[] friends = new int[memobj.friends.size()];
        int ii = 0;
        for (FriendEntry entry : memobj.friends) {
            friends[ii++] = entry.name.getMemberId();
        }
        _peerMan.invokeNodeAction(
            new FriendEntryUpdate(friends, memobj.getMemberId(), memobj.memberName.toString(),
                                  memobj.memberName.getPhoto(), memobj.headline));
    }

    /**
     * Dispatches a notification that a member's flow count has changed to whichever server they
     * are logged into.
     */
    public static void flowUpdated (MemberFlowRecord record)
    {
        _peerMan.invokeNodeAction(new FlowUpdated(record));
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
     */
    public static void avatarUpdated (int memberId, int avatarId)
    {
        _peerMan.invokeNodeAction(new AvatarUpdated(memberId, avatarId));
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

    protected static class InfoChanged extends MemberNodeAction
    {
        public InfoChanged (int memberId, String displayName, MediaDesc photo, String status) {
            super(memberId);
            _displayName = displayName;
            _photo = photo;
            _status = status;
        }

        public InfoChanged () {
        }

        protected void execute (MemberObject memobj) {
            memobj.updateDisplayName(_displayName, _photo);
            memobj.setHeadline(_status);
            _memberMan.updateOccupantInfo(memobj);
            _channelMan.updateMemberOnChannels(memobj.memberName);

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

        @Inject protected transient ChatChannelManager _channelMan;
        @Inject protected transient MemberManager _memberMan;
    }

    protected static class FlowUpdated extends MemberNodeAction
    {
        public FlowUpdated (MemberFlowRecord record) {
            super(record.memberId);
            _flow = record.flow;
            _accFlow = record.accFlow;
        }

        public FlowUpdated () {
        }

        protected void execute (MemberObject memobj) {
            memobj.startTransaction();
            try {
                memobj.setFlow(_flow);
                if (_accFlow != memobj.accFlow) {
                    memobj.setAccFlow(_accFlow);
                }
            } finally {
                memobj.commitTransaction();
            }
        }

        protected int _flow, _accFlow;
    }

    protected static class ReportUnreadMail extends MemberNodeAction
    {
        public ReportUnreadMail (int memberId, int newMailCount) {
            super(memberId);
            _newMailCount = newMailCount;
        }

        public ReportUnreadMail () {
        }

        protected void execute (MemberObject memobj) {
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

        protected void execute (MemberObject memobj) {
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

        protected void execute (MemberObject memobj) {
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

        protected void execute (MemberObject memobj) {
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

        protected void execute (MemberObject memobj) {
            _itemMan.avatarDeletedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;

        @Inject protected transient ItemManager _itemMan;
    }

    protected static class AvatarUpdated extends MemberNodeAction
    {
        public AvatarUpdated (int memberId, int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        public AvatarUpdated () {
        }

        protected void execute (MemberObject memobj) {
            _itemMan.avatarUpdatedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;

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

        protected void execute (MemberObject memobj) {
            _notifyMan.notify(memobj, _notification);
        }

        protected Notification _notification;

        @Inject protected transient NotificationManager _notifyMan;
    }

    protected static class BadgeAwarded extends MemberNodeAction
    {
        public BadgeAwarded (EarnedBadgeRecord record) {
            super(record.memberId);
            _badge = record.toBadge();
        }

        public BadgeAwarded () {
        }

        protected void execute (MemberObject memobj) {
            memobj.badgeAwarded(_badge);
        }

        protected EarnedBadge _badge;
    }

    protected static class InProgressBadgeUpdated extends MemberNodeAction
    {
        public InProgressBadgeUpdated (InProgressBadgeRecord record) {
            super(record.memberId);
            _badge = record.toBadge();
        }

        public InProgressBadgeUpdated () {
        }

        protected void execute (MemberObject memobj) {
            memobj.inProgressBadgeUpdated(_badge);
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

        protected void execute (MemberObject memobj) {
            memobj.stats.syncStat(_modifier);
        }

        protected StatModifier<T> _modifier;

        @Inject protected transient BadgeManager _badgeMan;
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

        @Override // from PeerManager.NodeAction
        public boolean isApplicable (NodeObject nodeobj)
        {
            MsoyNodeObject msoyNode = (MsoyNodeObject)nodeobj;
            for (int friendId : _friends) {
                if (msoyNode.clients.containsKey(MemberName.makeKey(friendId))) {
                    return true;
                }
            }

            // no friends found here, move along
            return false;
        }

        @Override // from PeerManager.NodeAction
        protected void execute ()
        {
            FriendEntry entry = new FriendEntry(
                new MemberName(_displayName, _memberId), true, _photo, _status);
            for (int friendId : _friends) {
                MemberObject memobj = _locator.lookupMember(friendId);
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
        @Inject protected MemberLocator _locator;
    }

    protected static MsoyPeerManager _peerMan;
}
