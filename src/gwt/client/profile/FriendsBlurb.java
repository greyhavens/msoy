//
// $Id$

package client.profile;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new VerticalPanel();
        _content.setWidth("100%");
        _content.addStyleName("friendsBlurb");
        return _content;
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        if (pdata.friends == null) {
            setHeader(CProfile.msgs.errorTitle());
            _content.add(new Label(CProfile.msgs.friendsLoadFailed()));
            return;
        }

        setHeader(CProfile.msgs.friendsTitle());

        ProfileGrid grid = new ProfileGrid(
            FRIEND_ROWS, FRIEND_COLUMNS, ProfileGrid.NAV_ON_BOTTOM, "");
        grid.setVerticalOrienation(true);
        grid.setWidth("100%");

        String empty = CProfile.getMemberId() == _name.getMemberId() ?
            CProfile.msgs.noFriendsSelf() : CProfile.msgs.noFriendsOther();
        grid.setEmptyMessage(empty);

        grid.setModel(new SimpleDataModel(pdata.friends), 0);
        _content.add(grid);

        boolean canInvite = CProfile.getMemberId() > 0 &&
            CProfile.getMemberId() != _name.getMemberId();
        for (int ii = 0, ll = pdata.friends.size(); ii < ll; ii++) {
            MemberCard friend = (MemberCard)pdata.friends.get(ii);
            canInvite = canInvite && !(friend.name.getMemberId() == CProfile.getMemberId());
        }
        if (canInvite) {
            _content.add(WidgetUtil.makeShim(5, 5));
            _content.add(new Button(CProfile.msgs.inviteFriend(), new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_name, CProfile.msgs.inviteTitle(),
                                        new FriendInvite.Composer(),
                                        CProfile.msgs.inviteBody()).show();
                }
            }));
        }
    }

    protected VerticalPanel _content;

    protected static final int FRIEND_COLUMNS = 3;
    protected static final int FRIEND_ROWS = 2;
}
