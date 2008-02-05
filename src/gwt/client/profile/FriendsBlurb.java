//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;
import client.util.ContentFooterPanel;
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
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.friendsTitle());

        FriendsGrid grid = new FriendsGrid();
        grid.setModel(new SimpleDataModel(pdata.friends), 0);

        FlexTable footer = new FlexTable();
        footer.setCellPadding(0);
        footer.setCellSpacing(0);
        footer.setWidth("100%");

        // always show the link if it's your own profile and you have at least one friend because
        // the all friends page is the only way to remove friends
        boolean isSelf = (CProfile.getMemberId() == _name.getMemberId());
        int moreThreshold =  isSelf ? 0 : FRIEND_COLUMNS * FRIEND_ROWS;
        Widget more;
        if (pdata.totalFriendCount > moreThreshold) {
            more = Application.createLink(
                CProfile.msgs.seeAllFriends(""+pdata.totalFriendCount),
                Page.PROFILE, Args.compose("f", pdata.name.getMemberId()));
        } else {
            more = new HTML("&nbsp;");
        }
        footer.setWidget(0, 0, more);
        footer.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_LEFT);
        footer.getFlexCellFormatter().setWidth(0, 0, "100%");

        boolean canInvite = CProfile.getMemberId() > 0 && !isSelf && !pdata.isOurFriend;
        if (canInvite) {
            footer.setWidget(0, 1, new Button(CProfile.msgs.inviteFriend(), new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_name, CProfile.msgs.inviteTitle(),
                                        new FriendInvite.Composer(),
                                        CProfile.msgs.inviteBody()).show();
                }
            }));

        } else if (isSelf) {
            footer.setWidget(0, 1, new Button(CProfile.msgs.findFriends(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "search");
                }
            }));
        }

        ContentFooterPanel content = new ContentFooterPanel(grid, footer);
        content.addStyleName("friendsBlurb");
        content.getFlexCellFormatter().setStyleName(0, 0, ""); // avoid double dottage
        setContent(content);
    }

    protected class FriendsGrid extends ProfileGrid
    {
        public FriendsGrid () {
            super(FRIEND_ROWS, FRIEND_COLUMNS, NAV_ON_BOTTOM, CProfile.msgs.noFriendsOther());
            addStyleName("dottedGrid");
            setVerticalOrienation(true);
            setWidth("100%");
        }

        // @Override // from PagedGrid
        protected Widget createEmptyContents ()
        {
            if (CProfile.getMemberId() != _name.getMemberId()) {
                return super.createEmptyContents();
            }
            return GroupsBlurb.createEmptyTable(
                CProfile.msgs.noFriendsSelf(), CProfile.msgs.noFriendsFindEm(),
                Page.PROFILE, "search");
        }
    }

    protected static final int FRIEND_COLUMNS = 3;
    protected static final int FRIEND_ROWS = 2;
}
