//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.friends != null);
    }

    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new FlexTable();
        _content.setCellSpacing(0);
        _content.setCellPadding(0);
        _content.setWidth("100%");
        _content.addStyleName("friendsBlurb");
        return _content;
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.friendsTitle());

        ProfileGrid grid = new ProfileGrid(
            FRIEND_ROWS, FRIEND_COLUMNS, ProfileGrid.NAV_ON_BOTTOM, "");
        grid.setVerticalOrienation(true);
        grid.setWidth("100%");
        grid.setHeight("100%");

        String empty = CProfile.getMemberId() == _name.getMemberId() ?
            CProfile.msgs.noFriendsSelf() : CProfile.msgs.noFriendsOther();
        grid.setEmptyMessage(empty);

        grid.setModel(new SimpleDataModel(pdata.friends), 0);
        _content.setWidget(0, 0, grid);
        _content.getFlexCellFormatter().setColSpan(0, 0, 2);

        boolean canInvite = CProfile.getMemberId() > 0 &&
            CProfile.getMemberId() != _name.getMemberId();
        for (int ii = 0, ll = pdata.friends.size(); ii < ll; ii++) {
            MemberCard friend = (MemberCard)pdata.friends.get(ii);
            canInvite = canInvite && !(friend.name.getMemberId() == CProfile.getMemberId());
        }
        if (canInvite) {
            _content.setWidget(2, 0, new Button(CProfile.msgs.inviteFriend(), new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_name, CProfile.msgs.inviteTitle(),
                                        new FriendInvite.Composer(),
                                        CProfile.msgs.inviteBody()).show();
                }
            }));
        } else if (CProfile.getMemberId() == _name.getMemberId()) {
            _content.setWidget(2, 0, new Button(CProfile.msgs.findFriends(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "search");
                }
            }));
        }

        if (pdata.totalFriendCount > FRIEND_COLUMNS * FRIEND_ROWS) {
            Widget more = Application.createLink(
                CProfile.msgs.seeAllFriends(""+pdata.totalFriendCount),
                Page.PROFILE, Args.compose("f", pdata.name.getMemberId()));
            more.addStyleName("tipLabel");
            _content.setWidget(2, 1, more);
            _content.getFlexCellFormatter().setHorizontalAlignment(2, 1, HasAlignment.ALIGN_RIGHT);
        }
    }

    protected FlexTable _content;

    protected static final int FRIEND_COLUMNS = 3;
    protected static final int FRIEND_ROWS = 2;
}
