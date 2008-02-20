//
// $Id$

package client.people;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Frame;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * Displays all of a member's friends. Allows a member to edit their friends list.
 */
public class FriendsPanel extends SmartTable
{
    public FriendsPanel (int memberId)
    {
        super("friendsPanel", 0, 5);

        setWidget(0, 0, new SearchControls());

        if (memberId <= 0) {
            return;
        }

        setText(1, 0, CPeople.msgs.friendsLoading());

        _memberId = memberId;
        CPeople.profilesvc.loadFriends(CPeople.ident, _memberId, new MsoyCallback() {
            public void onSuccess (Object result) {
                gotFriends((ProfileService.FriendsResult)result);
            }
        });
    }

    protected void gotFriends (ProfileService.FriendsResult data)
    {
        if (data == null) {
            setText(1, 0, CPeople.msgs.friendsNoSuchMember());
        } else {
            boolean self = (CPeople.getMemberId() == _memberId);
            Frame.setTitle(self ? CPeople.msgs.friendsSelfTitle() :
                           CPeople.msgs.friendsOtherTitle(data.name.toString()));
            _friends = new MemberList(
                self ? CPeople.msgs.noFriendsSelf() : CPeople.msgs.noFriendsOther());
            String title = CPeople.msgs.friendsWhoseFriends(data.name.toString());
            setWidget(1, 0, MsoyUI.createBox("people", title, _friends));
            _friends.setModel(new SimpleDataModel(data.friends), 0);
        }
    }

    protected int _memberId;
    protected MemberList _friends;
}
