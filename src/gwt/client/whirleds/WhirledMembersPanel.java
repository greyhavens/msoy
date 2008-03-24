//
// $Id$

package client.whirleds;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.GroupDetail;
import com.threerings.msoy.group.data.GroupMemberCard;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.person.data.Profile;
import com.threerings.msoy.web.client.GroupService;

import client.shell.Application;
import client.shell.Page;
import client.util.MemberStatusLabel;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;
import client.util.ThumbBox;

/**
 * Displays the members of a particular Whirled. Allows managers to manage ranks and membership.
 */
public class WhirledMembersPanel extends PagedGrid
{
    public WhirledMembersPanel (GroupDetail detail)
    {
        super(5, 2, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        _detail = detail;
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _loaded) {
            return;
        }

        CWhirleds.groupsvc.getGroupMembers(
            CWhirleds.ident, _detail.group.groupId, new MsoyCallback() {
            public void onSuccess (Object result) {
                setModel(new SimpleDataModel(((GroupService.MembersResult)result).members), 0);
            }
        });
        _loaded = true; // note that we've asked for our data
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new MemberWidget((GroupMemberCard)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return "This group has no members.";
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    public boolean amSenior (GroupMemberCard member) 
    {
        return (_detail.myRank > member.rank) ||
            (_detail.myRank == member.rank && _detail.myRankAssigned < member.rankAssigned);
    }

    protected Command updateMemberRank (final GroupMemberCard card, final byte rank) 
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.updateMemberRank(
                    CWhirleds.ident, _detail.group.groupId, card.name.getMemberId(), rank,
                    new MsoyCallback() {
                    public void onSuccess (Object result) {
                        card.rank = rank;
                        card.rankAssigned = System.currentTimeMillis();
                        // TODO: feedback?
                    }
                });
            }
        };
    }

    protected Command removeMember (final GroupMemberCard card)
    {
        return new Command() {
            public void execute () {
                CWhirleds.groupsvc.leaveGroup(CWhirleds.ident, _detail.group.groupId,
                                              card.name.getMemberId(), new MsoyCallback() {
                    public void onSuccess (Object result) {
                        removeItem(card);
                        // TODO: feedback
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

            int mid = name.getMemberId();
            ClickListener onClick = Application.createLinkListener(Page.PEOPLE, ""+mid);
            setWidget(0, 0, new ThumbBox(card.photo, onClick), 1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);
            setWidget(0, 1, Application.memberViewLink(""+name, mid), 1, "Name");
            String rankStr = card.rank == GroupMembership.RANK_MANAGER ? "Manager" : "";
            setText(1, 0, rankStr, 1, "tipLabel");
            setWidget(2, 0, new MemberStatusLabel(card.status), 2, null);

            // if we're not a manager above this member in rank, or we're not support+ don't add 
            // the edit controls
            if (!CWhirleds.isSupport() && 
                (_detail.myRank != GroupMembership.RANK_MANAGER || !amSenior(card))) {
                return;
            }

            if (card.rank == GroupMembership.RANK_MEMBER) {
                setAction(0, 2, CWhirleds.msgs.detailPromote(),
                          CWhirleds.msgs.detailPromotePrompt(""+card.name),
                          updateMemberRank(card, GroupMembership.RANK_MANAGER));
            } else {
                setAction(0, 2, CWhirleds.msgs.detailDemote(),
                          CWhirleds.msgs.detailDemotePrompt(""+card.name),
                          updateMemberRank(card, GroupMembership.RANK_MEMBER));
            }

            setAction(1, 1, CWhirleds.msgs.detailRemove(), CWhirleds.msgs.detailRemovePrompt(
                          ""+card.name, _detail.group.name), removeMember(card));
        }

        protected void setAction (int row, int col, String label, String confirm, Command command)
        {
            setWidget(row, col, MsoyUI.createActionLabel(
                          label, new PromptPopup(confirm, command)), 1, "Edit");
        }
    }

    protected boolean _loaded;
    protected GroupDetail _detail;
}
