//
// $Id$

package client.people;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.person.gwt.ProfileService;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Frame;
import client.ui.HeaderBox;
import client.ui.MsoyUI;
import client.util.MsoyCallback;

/**
 * Displays all of a member's friends. Allows a member to edit their friends list.
 */
public class FriendsPanel extends FlowPanel
{
    public FriendsPanel (int memberId)
    {
        setStyleName("friendsPanel");

        add(new SearchControls());

        if (memberId <= 0) {
            return;
        }

        _memberId = memberId;
        CPeople.profilesvc.loadFriends(CPeople.ident, _memberId,
            new MsoyCallback<ProfileService.FriendsResult>() {
                public void onSuccess (ProfileService.FriendsResult result) {
                    gotFriends(result);
                }
            });
    }

    protected void gotFriends (ProfileService.FriendsResult data)
    {
        if (data == null) {
            add(MsoyUI.createLabel(CPeople.msgs.friendsNoSuchMember(), null));
            return;
        }

        boolean self = (CPeople.getMemberId() == _memberId);
        Frame.setTitle(self ? CPeople.msgs.friendsSelfTitle() :
                       CPeople.msgs.friendsOtherTitle(data.name.toString()));
        _friends = new MemberList(
            self ? CPeople.msgs.noFriendsSelf() : CPeople.msgs.noFriendsOther());
        String title = CPeople.msgs.friendsWhoseFriends(data.name.toString());
        add(new HeaderBox(title, _friends));
        _friends.setModel(new SimpleDataModel<MemberCard>(data.friends), 0);
    }

    protected int _memberId;
    protected MemberList _friends;
}
