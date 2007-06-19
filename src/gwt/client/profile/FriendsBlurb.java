//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;
import com.threerings.msoy.web.data.MemberCard;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;
import client.util.ProfileGrid;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        _content = new ProfileGrid(FRIEND_ROWS, FRIEND_COLUMNS, "");
        _content.setVerticalOrienation(true);
        _content.setWidth("100%");
        _content.setStyleName("friendsBlurb");
        return _content;
    }

    // @Override // from Blurb
    protected void didInit (Object blurbData)
    {
        setHeader("Friends");

        String empty = CProfile.getMemberId() == _name.getMemberId() ?
            "You have no friends. Boo hoo." : "This person has no friends. How sad.";
        _content.setEmptyMessage(empty);

        ArrayList friends = (ArrayList)blurbData;
        _content.setModel(new SimpleDataModel(friends), 0);

        boolean canInvite = CProfile.getMemberId() > 0 &&
            CProfile.getMemberId() != _name.getMemberId();
        for (int ii = 0, ll = friends.size(); ii < ll; ii++) {
            MemberCard friend = (MemberCard)friends.get(ii);
            canInvite = canInvite && !(friend.name.getMemberId() == CProfile.getMemberId());
        }
        if (canInvite) {
            Button inviteButton = new Button("Invite To Be Your Friend", new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_name, "Be my Friend", new FriendInvite.Composer(),
                                        "Let's be buddies!").show();
                }
            });
            _content.addToHeader(WidgetUtil.makeShim(15, 1));
            _content.addToHeader(inviteButton);
        }
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader("Error");
        _content.setEmptyMessage("Failed to load friends: " + cause);
        _content.setModel(new SimpleDataModel(new ArrayList()), 0);
    }

    protected ProfileGrid _content;

    protected static final int FRIEND_COLUMNS = 3;
    protected static final int FRIEND_ROWS = 2;
}
