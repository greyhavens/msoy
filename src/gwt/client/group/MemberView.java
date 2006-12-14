//
// $Id$

package client.group;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ClickListener;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupMembership;

import client.shell.MsoyEntryPoint;
import client.util.HeaderValueTable;
import client.util.WebContext;

import client.group.GroupEdit.GroupSubmissionListener;

/**
 * Display the details of a group member.  Right now this means a link to their profile, and their
 * rank.  Managers are allowed to promote members to Manager in all groups, and boot members in 
 * non-public groups.  Managers can also demote any Manager that is their Junior.
 */
public class MemberView extends PopupPanel
{
    public MemberView (WebContext ctx, final GroupMembership membership, Group group, 
        GroupMembership me, GroupSubmissionListener listener)
    {
        super(true);
        _ctx = ctx;
        _group = group;
        _listener = listener;
        setStyleName("memberPopup");

        DockPanel content = new DockPanel();
        setWidget(content);

        HeaderValueTable table = new HeaderValueTable();
        content.add(table, DockPanel.CENTER);

        _errorContainer = new VerticalPanel();
        _errorContainer.setStyleName("memberViewErrors");
        content.add(_errorContainer, DockPanel.NORTH);

        table.addHeader("" + membership.member);

        table.addRow(new HTML("<a href='" + MsoyEntryPoint.memberViewPath(
            membership.member.getMemberId()) + "'>Profile</a>"));
        table.addRow("Rank", Byte.toString(membership.rank));
        
        if (isSenior(me, membership)) { 
            if (membership.rank == GroupMembership.RANK_MEMBER) {
                Label promoteLabel = new InlineLabel("Promote Member", false);
                promoteLabel.addClickListener(new ClickListener() {
                    public void onClick(Widget sender) {
                        updateMemberRank(membership.member.getMemberId(), 
                            GroupMembership.RANK_MANAGER);
                    }
                });
                table.addRow(promoteLabel);
            } else {
                Label demoteLabel = new InlineLabel("Demote Member", false);
                demoteLabel.addClickListener(new ClickListener() {
                    public void onClick(Widget sender) {
                        updateMemberRank(membership.member.getMemberId(),
                            GroupMembership.RANK_MEMBER);
                    }
                });
                table.addRow(demoteLabel);
            }
            if (_group.policy != Group.POLICY_PUBLIC) {
                Label removeLabel = new InlineLabel("Remove Member", false);
                removeLabel.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        removeMember(membership.member.getMemberId());
                    }
                });
                table.addRow(removeLabel);
            }
        }
    }

    /**
     * Decide if member1 is senior to member2.
     */
    public boolean isSenior (GroupMembership member1, GroupMembership member2) 
    {
        if (member1.rank == GroupMembership.RANK_MANAGER && 
            (member2.rank == GroupMembership.RANK_MEMBER || 
            member1.rankAssignedDate < member2.rankAssignedDate)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes a member from the group, and then trigger a reload/UI rebuild.
     */
    protected void removeMember (final int memberId)
    {
        _ctx.groupsvc.leaveGroup(_ctx.creds, _group.groupId, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _listener.groupSubmitted(_group);
                MemberView.this.hide();
            }
            public void onFailure (Throwable caught) {
                GWT.log("Failed to remove member [groupId=" + _group.groupId +
                        ", memberId=" + memberId + "]", caught);
                addError("Failed to remove member: " + caught.getMessage());
            }
        });
    }

    /**
     * Updates the member's rank, which does not currently require a UI rebuild.
     */
    protected void updateMemberRank (final int memberId, final byte rank) 
    {
        _ctx.groupsvc.updateMemberRank(_ctx.creds, _group.groupId, memberId, rank,
            new AsyncCallback() {
            public void onSuccess (Object result) {
                _listener.groupSubmitted(_group);
                MemberView.this.hide();
            }
            public void onFailure (Throwable caught) {
                GWT.log("Failed to update member rank [groupId=" + _group.groupId +
                    ", memberId=" + memberId + ", newRank=" + rank + "]", caught);
                addError("Failed to update member rank: " + caught.getMessage());
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    protected WebContext _ctx;
    protected Group _group;
    protected GroupSubmissionListener _listener;

    protected VerticalPanel _errorContainer;
}
