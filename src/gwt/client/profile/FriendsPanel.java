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

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.MailComposition;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PromptPopup;

/**
 * Displays all of a member's friends. Allows a member to edit their friends list.
 */
public class FriendsPanel extends VerticalPanel
{
    public FriendsPanel (Page page, int memberId)
    {
        setStyleName("friendsPanel");
        _page = page;
        _page.setPageTitle(CProfile.msgs.friendsTitle());
        _memberId = memberId;

        if (memberId <= 0) {
            add(new Label(CProfile.msgs.friendsNoSuchMember()));
            return;
        } else {
            add(new Label(CProfile.msgs.friendsLoading()));
        }

        CProfile.profilesvc.loadFriends(CProfile.ident, _memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                clear();
                if (result == null) {
                    add(new Label(CProfile.msgs.friendsNoSuchMember()));
                } else {
                    ProfileService.FriendsResult data = (ProfileService.FriendsResult)result;
                    _page.setPageTitle(CProfile.msgs.friendsTitle(), data.name.toString());
                    add(_grid = new FriendsGrid(data.friends));
                }
            }

            public void onFailure (Throwable cause) {
                clear();
                add(new Label(CProfile.serverError(cause)));
                CProfile.log("Failed to load blurbs", cause);
            }
        });
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
            super(Math.max(1, (int)Math.ceil(friends.size() / (float)COLUMNS)), COLUMNS,
                  ProfileGrid.NAV_ON_TOP, (CProfile.getMemberId() == _memberId) ?
                  CProfile.msgs.noFriendsSelf() : CProfile.msgs.noFriendsOther());
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

                if (CProfile.getMemberId() != _memberId) {
                    return;
                }

                getFlexCellFormatter().setWidth(0, 1, "100%");
                setWidget(0, 2, MsoyUI.createActionLabel("", "Remove", new ClickListener() {
                    public void onClick (Widget widget) {
                        removeFriend(card, false);
                    }
                }));
                setWidget(0, 3, MsoyUI.createActionLabel("", "SendMail", new ClickListener() {
                    public void onClick (Widget widget) {
                        new MailComposition(card.name, null, null, null).show();
                    }
                }));

                getFlexCellFormatter().setColSpan(1, 0, 3);
                getFlexCellFormatter().setColSpan(2, 0, 3);
            }
        }
    }

    protected Page _page;
    protected int _memberId;
    protected FriendsGrid _grid;

    protected static final int COLUMNS = 3;
}
