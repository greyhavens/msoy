//
// $Id$

package com.threerings.msoy.server;

import com.google.inject.Inject;

import com.threerings.presents.peer.data.NodeObject;

import com.threerings.presents.peer.server.PeerManager;

import com.threerings.msoy.badge.server.persist.BadgeRecord;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.peer.data.MsoyNodeObject;

import com.threerings.msoy.server.persist.MemberFlowRecord;

import com.threerings.msoy.notify.data.Notification;

import com.threerings.msoy.group.data.GroupMembership;

import com.threerings.msoy.peer.server.MemberNodeAction;

/**
 * Contains various member node actions.
 */
public class MemberNodeActions
{
    /**
     * Dispatches a notification that a member's info has changed to whichever server they are 
     * logged into.
     */
    public static void infoChanged (
        int memberId, String displayName, MediaDesc photo, String status)
    {
        MsoyServer.peerMan.invokeNodeAction(new InfoChanged(memberId, displayName, photo, status));
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
        MsoyServer.peerMan.invokeNodeAction(new FriendEntryUpdate(
            friends, memobj.getMemberId(), memobj.memberName.toString(), 
            memobj.memberName.getPhoto(), memobj.headline));
    }

    /**
     * Dispatches a notification that a member's flow count has changed to whichever server they
     * are logged into.
     */
    public static void flowUpdated (MemberFlowRecord record)
    {
        MsoyServer.peerMan.invokeNodeAction(new FlowUpdated(record));
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
        MsoyServer.peerMan.invokeNodeAction(new ReportUnreadMail(memberId, newMailCount));
    }

    /**
     * Dispatches a notification that a member has joined the specified group to whichever server
     * they are logged into.
     */
    public static void joinedGroup (int memberId, GroupMembership gm)
    {
        MsoyServer.peerMan.invokeNodeAction(new JoinedGroup(memberId, gm));
    }

    /**
     * Dispatches a notification that a member has left the specified group to whichever server
     * they are logged into.
     */
    public static void leftGroup (int memberId, int groupId)
    {
        MsoyServer.peerMan.invokeNodeAction(new LeftGroup(memberId, groupId));
    }

    /**
     * Boots a member from any server into which they are logged in.
     */
    public static void bootMember (int memberId)
    {
        MsoyServer.peerMan.invokeNodeAction(new BootMember(memberId));
    }

    /**
     * Update a changed avatar.
     */
    public static void avatarUpdated (int memberId, int avatarId)
    {
        MsoyServer.peerMan.invokeNodeAction(new AvatarUpdated(memberId, avatarId));
    }

    /**
     * Act upon a deleted avatar.
     */
    public static void avatarDeleted (int memberId, int avatarId)
    {
        MsoyServer.peerMan.invokeNodeAction(new AvatarDeleted(memberId, avatarId));
    }

    /**
     * Send a notification to a member
     */
    public static void sendNotification (int memberId, Notification notification)
    {
        MsoyServer.peerMan.invokeNodeAction(new SendNotification(memberId, notification));
    }

    /**
     * Dispatches a notification that a member has won a badge.
     */
    public static void badgeAwarded (BadgeRecord record)
    {
        MsoyServer.peerMan.invokeNodeAction(new BadgeAwarded(record));
    }

    protected static class InfoChanged extends MemberNodeAction
    {
        public InfoChanged (int memberId, String displayName, MediaDesc photo, String status) {
            super(memberId);
            _displayName = displayName;
            _photo = photo;
            _status = status;
        }

        protected void execute (MemberObject memobj) {
            memobj.updateDisplayName(_displayName, _photo);
            memobj.setHeadline(_status);
            MsoyServer.memberMan.updateOccupantInfo(memobj);
            MsoyServer.channelMan.updateMemberOnChannels(memobj.memberName);
            
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
    }

    protected static class FlowUpdated extends MemberNodeAction
    {
        public FlowUpdated (MemberFlowRecord record) {
            super(record.memberId);
            _flow = record.flow;
            _accFlow = record.accFlow;
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

        protected void execute (MemberObject memobj) {
            MsoyServer.memberMan.bootMember(_memberId);
        }
    }

    protected static class AvatarDeleted extends MemberNodeAction
    {
        public AvatarDeleted (int memberId, int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        protected void execute (MemberObject memobj) {
            MsoyServer.itemMan.avatarDeletedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;
    }

    protected static class AvatarUpdated extends MemberNodeAction
    {
        public AvatarUpdated (int memberId, int avatarId) {
            super(memberId);
            _avatarId = avatarId;
        }

        protected void execute (MemberObject memobj) {
            MsoyServer.itemMan.avatarUpdatedOnPeer(memobj, _avatarId);
        }

        protected int _avatarId;
    }

    protected static class SendNotification extends MemberNodeAction
    {
        public SendNotification (int memberId, Notification notification) {
            super(memberId);
            _notification = notification;
        }

        protected void execute (MemberObject memobj) {
            MsoyServer.notifyMan.notify(memobj, _notification);
        }

        protected Notification _notification;
    }

    protected static class BadgeAwarded extends MemberNodeAction
    {
        public BadgeAwarded (BadgeRecord record) {
            super(record.memberId);
        }

        protected void execute (MemberObject memobj) {
            // TODO something magical happens here
        }
    }

    protected static class FriendEntryUpdate extends PeerManager.NodeAction
    {
        public FriendEntryUpdate (
            int[] friends, int memberId, String displayName, MediaDesc photo, String status) {
            _friends = friends;
            _memberId = memberId;
            _displayName = displayName;
            _photo = photo;
            _status = status;
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
}
