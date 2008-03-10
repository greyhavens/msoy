//
// $Id$

package com.threerings.msoy.server;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberFlowRecord;

import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.peer.server.MemberNodeAction;

/**
 * Contains various member node actions.
 */
public class MemberNodeActions
{
    /**
     * Dispatches a notification that a member's display name has changed to whichever server they
     * are logged into.
     */
    public static void displayNameChanged (MemberName name)
    {
        MsoyServer.peerMan.invokeNodeAction(new DisplayNameChanged(name));
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

    protected static class DisplayNameChanged extends MemberNodeAction
    {
        public DisplayNameChanged (MemberName name) {
            super(name.getMemberId());
            _name = name.toString();
        }

        protected void execute (MemberObject memobj) {
            memobj.updateDisplayName(_name);
            MsoyServer.memberMan.updateOccupantInfo(memobj);
        }

        protected String _name;
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
            if (memobj.newMailCount != _newMailCount) {
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
}
