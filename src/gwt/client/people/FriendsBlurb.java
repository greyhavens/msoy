//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.ContentFooterPanel;
import client.util.MediaUtil;
import client.util.MsoyUI;

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
        setHeader(CPeople.msgs.friendsTitle());

        Widget body;
        if (pdata.friends.size() == 0) {
            if (CPeople.getMemberId() != _name.getMemberId()) {
                body = new Label(CPeople.msgs.noFriendsOther());
            } else {
                body = GroupsBlurb.createEmptyTable(
                    CPeople.msgs.noFriendsSelf(), CPeople.msgs.noFriendsFindEm(),
                    Page.PEOPLE, "search");
            }
        } else {
            SmartTable grid = new SmartTable();
            grid.setWidth("100%");
            for (int ii = 0; ii < pdata.friends.size(); ii++) {
                int row = ii / FRIEND_COLUMNS, col = ii % FRIEND_COLUMNS;
                grid.setWidget(row, col, new FriendWidget((MemberCard)pdata.friends.get(ii)));
            }
            body = grid;
        }

        FlexTable footer = new FlexTable();
        footer.setCellPadding(0);
        footer.setCellSpacing(0);
        footer.setWidth("100%");

        // always show the link if it's your own profile and you have at least one friend because
        // the all friends page is the only way to remove friends
        boolean isSelf = (CPeople.getMemberId() == _name.getMemberId());
        int moreThreshold =  isSelf ? 0 : FRIEND_COLUMNS * FRIEND_ROWS;
        Widget more;
        if (pdata.totalFriendCount > moreThreshold) {
            more = Application.createLink(
                CPeople.msgs.seeAllFriends(""+pdata.totalFriendCount),
                Page.PEOPLE, Args.compose("f", pdata.name.getMemberId()));
        } else {
            more = new HTML("&nbsp;");
        }
        footer.setWidget(0, 0, more);
        footer.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_LEFT);
        footer.getFlexCellFormatter().setWidth(0, 0, "100%");

        boolean canInvite = CPeople.getMemberId() > 0 && !isSelf && !pdata.isOurFriend;
        if (canInvite) {
            footer.setWidget(0, 1, new Button(CPeople.msgs.inviteFriend(), new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_name, CPeople.msgs.inviteTitle(),
                                        new FriendInvite.Composer(),
                                        CPeople.msgs.inviteBody()).show();
                }
            }));

        } else if (isSelf) {
            footer.setWidget(0, 1, new Button(CPeople.msgs.findFriends(), new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PEOPLE, "search");
                }
            }));
        }

        ContentFooterPanel content = new ContentFooterPanel(body, footer);
        content.addStyleName("friendsBlurb");
        setContent(content);
    }

    protected class FriendWidget extends FlowPanel
    {
        public FriendWidget (final MemberCard card) 
        {
            setStyleName("Friend");
            ClickListener profileClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PEOPLE, "" + card.name.getMemberId());
                }
            };
            add(MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE, profileClick));
            add(MsoyUI.createActionLabel(card.name.toString(), profileClick));
        }
    }

    protected static final int FRIEND_COLUMNS = 3;
    protected static final int FRIEND_ROWS = 2;
}
