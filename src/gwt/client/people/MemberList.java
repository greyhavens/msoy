//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a list of members.
 */
public class MemberList extends PagedGrid<MemberCard>
{
    public static final int PEOPLE_PER_PAGE = 20;

    /**
     * Creates a new member list.
     * @param emptyMessage text to show when no members are in the list
     * @param id identifier to use for tracking friending events
     */
    public MemberList (String emptyMessage, String id)
    {
        super(PEOPLE_PER_PAGE, 1, MemberList.NAV_ON_BOTTOM);
        _id = id;
        setWidth("100%");
        addStyleName("dottedGrid");
        _emptyMessage = emptyMessage;
    }

    @Override // from PagedGrid
    protected Widget createWidget (MemberCard member)
    {
        return new StandardMemberWidget(member);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _emptyMessage;
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true;
    }

    protected class StandardMemberWidget extends MemberWidget
    {
        public StandardMemberWidget (MemberCard card)
        {
            super(card);
        }

        @Override
        protected void addExtras (SmartTable extras, final MemberCard card)
        {
            super.addExtras(extras, card);

            int row = extras.getRowCount();
            boolean isNotMe = CShell.getMemberId() != card.name.getMemberId();
            ClickListener onClick;

            // potentially show the add friend button
            if (isNotMe && !card.isFriend) {
                onClick = new FriendInviter(card.name, _id);
                extras.setWidget(row, 0, MsoyUI.createActionImage(
                                     "/images/profile/addfriend.png", onClick));
                extras.setWidget(row++, 1, MsoyUI.createActionLabel(
                                     _msgs.mlAddFriend(), onClick));
            }

            // if we're not a guest, we can send them mail
            if (isNotMe && !CShell.isGuest()) {
                onClick = Link.createListener(
                    Pages.MAIL, Args.compose("w", "m", ""+card.name.getMemberId()));
                extras.setWidget(row, 0,
                    MsoyUI.createActionImage("/images/profile/sendmail.png", onClick));
                extras.setWidget(row++, 1,
                    MsoyUI.createActionLabel(_msgs.mlSendMail(), onClick));
            }

            onClick = Link.createListener(Pages.WORLD, "m" + card.name.getMemberId());
            extras.setWidget(row, 0,
                MsoyUI.createActionImage("/images/profile/visithome.png", onClick));
            extras.setWidget(row++, 1,
                MsoyUI.createActionLabel(_msgs.mlVisitHome(), onClick));

            // if they are our friend, show the remove friend button
            if (isNotMe && card.isFriend) {
                onClick = new FriendRemover(card.name, new Command() {
                    public void execute () {
                        removeItem(card);
                    }
                });
                extras.setWidget(row, 0,
                    MsoyUI.createActionImage("/images/profile/remove.png", onClick));
                extras.setWidget(row++, 1,
                    MsoyUI.createActionLabel(_msgs.mlRemoveFriend(), onClick));
            }
        }
    }

    protected String _emptyMessage;
    protected String _id; // for event tracking

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
