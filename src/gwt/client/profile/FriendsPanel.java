//
// $Id$

package client.profile;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.web.client.ProfileService;

import client.shell.Page;

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
                    return;
                }

                ProfileService.FriendsResult data = (ProfileService.FriendsResult)result;
                _page.setPageTitle(CProfile.msgs.friendsTitle(), data.name.toString());
                String empty = (CProfile.getMemberId() == _memberId) ?
                    CProfile.msgs.noFriendsSelf() : CProfile.msgs.noFriendsOther();
                int rows = Math.max(1, (int)Math.ceil(data.friends.size() / (float)COLUMNS));
                ProfileGrid grid = new ProfileGrid(rows, COLUMNS, ProfileGrid.NAV_ON_TOP, empty);
                grid.setModel(new SimpleDataModel(data.friends), 0);
                add(grid);
            }

            public void onFailure (Throwable cause) {
                clear();
                add(new Label(CProfile.serverError(cause)));
                CProfile.log("Failed to load blurbs", cause);
            }
        });
    }

    protected Page _page;
    protected int _memberId;

    protected static final int COLUMNS = 3;
}
