//
// $Id$

package client.profile;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.MailComposition;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays all of a member's friends. Allows a member to edit their friends list.
 */
public class FriendsPanel extends SmartTable
{
    public FriendsPanel (int memberId)
    {
        super("friendsPanel", 0, 5);

        Frame.setTitle(CProfile.msgs.friendsTitle());

        setWidget(0, 0, new SearchControls());

        if (memberId <= 0) {
            return;
        }

        setText(1, 0, CProfile.msgs.friendsLoading());

        _memberId = memberId;
        CProfile.profilesvc.loadFriends(CProfile.ident, _memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotFriends((ProfileService.FriendsResult)result);
            }
            public void onFailure (Throwable cause) {
                CProfile.log("Failed to load friends", cause);
                setText(1, 0, CProfile.serverError(cause));
            }
        });
    }

    protected void gotFriends (ProfileService.FriendsResult data)
    {
        if (data == null) {
            setText(1, 0, CProfile.msgs.friendsNoSuchMember());
        } else {
            Frame.setTitle(CProfile.msgs.friendsTitle(), data.name.toString());
            _grid = new FriendsGrid(data.friends);
            String title = CProfile.msgs.friendsWhoseFriends(data.name.toString());
            setWidget(1, 0, MsoyUI.createBox("people", title, _grid));
        }
    }

    protected void removeFriend (final MemberCard friend, boolean confirmed)
    {
        if (!confirmed) {
            new PromptPopup(CProfile.msgs.friendsRemoveConfirm(friend.name.toString())) {
                public void onAffirmative () {
                    removeFriend(friend, true);
                }
            }.prompt();
            return;
        }

        CProfile.membersvc.removeFriend(
            CProfile.ident, friend.name.getMemberId(), new MsoyCallback() {
            public void onSuccess (Object result) {
                MsoyUI.error(CProfile.msgs.friendsRemoved(friend.name.toString()));
                _grid.removeItem(friend);
            }
        });
    }

    protected class FriendsGrid extends ProfileGrid
    {
        public FriendsGrid (List friends) {
            super(Math.max(1, friends.size()), 1,
                  ProfileGrid.NAV_ON_TOP, (CProfile.getMemberId() == _memberId) ?
                  CProfile.msgs.noFriendsSelf() : CProfile.msgs.noFriendsOther());
            setWidth("600px");
            addStyleName("dottedGrid");
            setModel(new SimpleDataModel(friends), 0);
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new FriendWidget((MemberCard)item);
        }

        protected class FriendWidget extends ProfileWidget
        {
            public FriendWidget (final MemberCard card) {
                super(card);
                if (card == null) {
                    return;
                }

                SmartTable extras = new SmartTable(0, 5);
                int row = 0;
                ClickListener onClick;

                if (CProfile.getMemberId() == _memberId) {
                    onClick = new ClickListener() {
                        public void onClick (Widget widget) {
                            removeFriend(card, false);
                        }
                    };
                    extras.setWidget(
                        row, 0, MsoyUI.createActionImage("/images/profile/remove.png", onClick));
                    extras.setWidget(
                        row++, 1, MsoyUI.createActionLabel("Remove friend", onClick));
                }

                onClick = new ClickListener() {
                    public void onClick (Widget widget) {
                        new MailComposition(card.name, null, null, null).show();
                    }
                };
                extras.setWidget(
                    row, 0, MsoyUI.createActionImage("/images/profile/sendmail.png", onClick));
                extras.setWidget(
                    row++, 1, MsoyUI.createActionLabel("Send mail", onClick));

                onClick = new ClickListener() {
                    public void onClick (Widget widget) {
                        Application.go(Page.WORLD, Args.compose("m", card.name.getMemberId()));
                    }
                };
                extras.setWidget(
                    row, 0, MsoyUI.createActionImage("/images/profile/visithome.png", onClick));
                extras.setWidget(
                    row++, 1, MsoyUI.createActionLabel("Visit home", onClick));

                setWidget(0, 2, extras);
                getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
                getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
            }
        }
    }

    protected int _memberId;
    protected int _friendsRow;
    protected FriendsGrid _grid;
}
