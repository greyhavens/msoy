//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.ui.HeaderBox;
import client.ui.MsoyUI;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

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
        _membersvc.loadFriends(_memberId, true,
            new MsoyCallback<WebMemberService.FriendsResult>() {
                public void onSuccess (WebMemberService.FriendsResult result) {
                    gotFriends(result);
                }
            });
    }

    protected void gotFriends (WebMemberService.FriendsResult data)
    {
        if (data == null) {
            add(MsoyUI.createLabel(_msgs.friendsNoSuchMember(), null));
            return;
        }

        boolean self = (CShell.getMemberId() == _memberId);
        CShell.frame.setTitle(
            self ? _msgs.friendsSelfTitle() : _msgs.friendsOtherTitle(data.name.toString()));
        _friends = new MemberList(
            self ? _msgs.noFriendsSelf() : _msgs.noFriendsOther());
        
        int numFriends = 0;
        for (MemberCard card : data.friendsAndGreeters) {
            if (card.isFriend) {
                numFriends++;
            }
        }
        
        String title;
        if (numFriends == data.friendsAndGreeters.size()) { // no greeters
            title = _msgs.friendsWhoseFriends(data.name.toString());
        } else if (numFriends == 0) { // no friends
            title = _msgs.friendsWhoseGreeters();
        } else { // mixed
            title = _msgs.friendsWhoseFriendsAndGreeters(data.name.toString());
        }
        add(new HeaderBox(title, _friends));
        _friends.setModel(new SimpleDataModel<MemberCard>(data.friendsAndGreeters), 0);
    }

    protected int _memberId;
    protected MemberList _friends;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
