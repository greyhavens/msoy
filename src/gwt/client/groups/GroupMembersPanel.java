//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.gwt.GroupDetail;
import com.threerings.msoy.group.gwt.GroupMemberCard;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MemberStatusLabel;
import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MsoyPagedServiceDataModel;

/**
 * Displays the members of a particular Group. Allows managers to manage ranks and membership.
 */
public class GroupMembersPanel extends PagedGrid<GroupMemberCard>
{
   public GroupMembersPanel (GroupDetail detail)
    {
        super(5, 2, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");

        _detail = detail;
        _invite.addClickHandler(Link.createHandler(Pages.MAIL, "w", "g", _detail.group.groupId));
        _invite.setEnabled(Group.canInvite(detail.group.policy, detail.myRank));

        setModel(new MsoyPagedServiceDataModel<GroupMemberCard, PagedResult<GroupMemberCard>>(){
            @Override protected void callFetchService (
                int start, int count, boolean needCount,
                AsyncCallback<PagedResult<GroupMemberCard>> callback) {
                _groupsvc.getGroupMembers(_detail.group.groupId, start, count, callback);
            }

        }, 0);
    }

    @Override // from PagedGrid
    protected Widget createWidget (GroupMemberCard card)
    {
        return new MemberWidget(card);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return "This group has no members.";
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        controls.setWidget(0, 0, _invite = new Button(_msgs.wmpInvite()));
    }

    @Override // from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    public boolean amSenior (GroupMemberCard member)
    {
        int cmp = _detail.myRank.compareTo(member.rank);
        return cmp > 0 || (cmp == 0 && _detail.myRankAssigned < member.rankAssigned);
    }

    protected Command updateMemberRank (final GroupMemberCard card, final Rank rank)
    {
        return new Command() {
            public void execute () {
                _groupsvc.updateMemberRank(_detail.group.groupId, card.name.getId(), rank,
                    new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            card.rank = rank;
                            card.rankAssigned = System.currentTimeMillis();
                            // force a page refresh so we see the new rank...
                            displayPage(_page, true);
                        }
                    });
            }
        };
    }

    protected Command removeMember (final GroupMemberCard card)
    {
        return new Command() {
            public void execute () {
                _groupsvc.leaveGroup(
                    _detail.group.groupId, card.name.getId(), new InfoCallback<Void>() {
                        public void onSuccess (Void result) {
                            removeItem(card);
                        }
                    });
            }
        };
    }

    protected class MemberWidget extends SmartTable
    {
        public MemberWidget (GroupMemberCard card)
        {
            super("Member", 0, 2);
            MemberName name = card.name;

            int mid = name.getId();
            setWidget(0, 0, new ThumbBox(card.photo, Pages.PEOPLE, ""+mid), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);
            setWidget(0, 1, Link.memberView(card), 1, "Name");
            String rankStr = card.rank == Rank.MANAGER ? "Manager" : "";
            setText(1, 0, rankStr, 1, "tipLabel");
            setWidget(2, 0, new MemberStatusLabel(card), 2);

            // if we aren't authorized for membership control on this member, skip these widgets
            if (!(CShell.isSupport() || (_detail.myRank == Rank.MANAGER && amSenior(card)))) {
                return;
            }

            if (card.rank == Rank.MEMBER) {
                setAction(0, 2, _msgs.detailPromote(),
                          _msgs.detailPromotePrompt(""+card.name),
                          updateMemberRank(card, Rank.MANAGER));
            } else {
                setAction(0, 2, _msgs.detailDemote(),
                          _msgs.detailDemotePrompt(""+card.name),
                          updateMemberRank(card, Rank.MEMBER));
            }

            setAction(1, 1, _msgs.detailRemove(), _msgs.detailRemovePrompt(
                          ""+card.name, _detail.group.name), removeMember(card));
        }

        protected void setAction (int row, int col, String label, String confirm, Command command)
        {
            setWidget(row, col, MsoyUI.createActionLabel(
                          label, new PromptPopup(confirm, command)), 1, "Edit");
        }
    }

    protected GroupDetail _detail;
    protected Button _invite;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
