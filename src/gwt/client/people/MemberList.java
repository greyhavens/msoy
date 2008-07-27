//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.Page;

import client.util.Link;
import client.util.MediaUtil;
import client.util.MemberStatusLabel;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;
import client.util.ServiceUtil;

/**
 * Displays a list of members.
 */
public class MemberList extends PagedGrid<MemberCard>
{
    public static final int PEOPLE_PER_PAGE = 20;

    public MemberList (String emptyMessage)
    {
        super(PEOPLE_PER_PAGE, 1, MemberList.NAV_ON_BOTTOM);
        setWidth("100%");
        addStyleName("dottedGrid");
        _emptyMessage = emptyMessage;
    }

    @Override // from PagedGrid
    protected Widget createWidget (MemberCard member)
    {
        return new MemberWidget(member);
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

    protected Command removeFriend (final MemberCard card)
    {
        return new Command() {
            public void execute () {
                _membersvc.removeFriend(
                    CPeople.ident, card.name.getMemberId(), new MsoyCallback<Void>() {
                    public void onSuccess (Void result) {
                        MsoyUI.info(CPeople.msgs.mlRemoved(card.name.toString()));
                        removeItem(card);
                    }
                });
            }
        };
    }

    protected class MemberWidget extends SmartTable
    {
        public MemberWidget (MemberCard card)
        {
            super("memberWidget", 0, 5);

            setWidget(0, 0, MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE,
                                                      Link.createListener(
                                                      Page.PEOPLE, "" + card.name.getMemberId())),
                      1, "Photo");
            getFlexCellFormatter().setRowSpan(0, 0, 3);

            setWidget(0, 1, Link.create(card.name.toString(), Page.PEOPLE,
                                                   ""+card.name.getMemberId()), 1, "Name");

            // we'll overwrite these below if we have anything to display
            getFlexCellFormatter().setStyleName(1, 0, "Status");
            setHTML(1, 0, "&nbsp;");
            setHTML(2, 0, "&nbsp;");
            if (card.headline != null && card.headline.length() > 0) {
                setText(1, 0, card.headline);
            }
            setWidget(2, 0, new MemberStatusLabel(card.status));

            boolean isNotMe = CPeople.getMemberId() != card.name.getMemberId();
            SmartTable extras = new SmartTable("Extras", 0, 5);
            ClickListener onClick;
            int row = 0;

            // potentially show the add friend button
            if (isNotMe && !card.isFriend && !CPeople.isGuest()) {
                onClick = InviteFriendPopup.createListener(card.name);
                extras.setWidget(row, 0, MsoyUI.createActionImage(
                                     "/images/profile/addfriend.png", onClick));
                extras.setWidget(row++, 1, MsoyUI.createActionLabel(
                                     CPeople.msgs.mlAddFriend(), onClick));
            }

            // if we're not a guest, we can send them mail
            if (isNotMe && !CPeople.isGuest()) {
                onClick = Link.createListener(
                    Page.MAIL, Args.compose("w", "m", ""+card.name.getMemberId()));
                extras.setWidget(row, 0,
                    MsoyUI.createActionImage("/images/profile/sendmail.png", onClick));
                extras.setWidget(row++, 1,
                    MsoyUI.createActionLabel(CPeople.msgs.mlSendMail(), onClick));
            }

            // always show the visit home button
            onClick = Link.createListener(Page.WORLD, "m" + card.name.getMemberId());
            extras.setWidget(row, 0,
                MsoyUI.createActionImage("/images/profile/visithome.png", onClick));
            extras.setWidget(row++, 1,
                MsoyUI.createActionLabel(CPeople.msgs.mlVisitHome(), onClick));

            // if they are our friend, show the remove friend button
            if (isNotMe && card.isFriend) {
                onClick = new PromptPopup(
                    CPeople.msgs.mlRemoveConfirm(""+card.name), removeFriend(card));
                extras.setWidget(row, 0,
                    MsoyUI.createActionImage("/images/profile/remove.png", onClick));
                extras.setWidget(row++, 1,
                    MsoyUI.createActionLabel(CPeople.msgs.mlRemoveFriend(), onClick));
            }

            setWidget(0, 2, extras);
            getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
            getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        }
    }

    protected String _emptyMessage;

    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
}
