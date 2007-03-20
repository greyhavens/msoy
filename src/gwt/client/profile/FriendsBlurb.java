//
// $Id$

package client.profile;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.FriendEntry;

import client.msgs.FriendInvite;
import client.msgs.MailComposition;
import client.shell.Application;

/**
 * Displays a person's friends list.
 */
public class FriendsBlurb extends Blurb
{
    // @Override // from Blurb
    protected Panel createContent ()
    {
        return (_content = new FlexTable());
    }

    // @Override // from Blurb
    protected void didInit (Object blurbData)
    {
        setHeader("Friends");

        ArrayList friends = (ArrayList)blurbData;
        boolean canInvite = (CProfile.getMemberId() > 0 && CProfile.getMemberId() != _memberId);
        if (friends.size() == 0) {
            setStatus(CProfile.getMemberId() == _memberId ?
                      "You have no friends. Boo hoo." : "This person has no friends. How sad.");

        } else {
            for (int ii = 0, ll = friends.size(); ii < ll; ii++) {
                FriendEntry friend = (FriendEntry)friends.get(ii);
                canInvite = canInvite && !(friend.getMemberId() == CProfile.getMemberId());
                Hyperlink link = Application.memberViewLink(
                    friend.name.toString(), friend.name.getMemberId());
                _content.setWidget(ii, 0, link);
            }
        }

        if (canInvite) {
            Button inviteButton = new Button("Invite To Be Your Friend");
            inviteButton.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    new MailComposition(_memberId, "Be my Friend", new FriendInvite.Composer(),
                                        "Let's be buddies!").show();
                }
            });
            _content.setWidget(_content.getRowCount(), 0, inviteButton);
        }
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setHeader("Error");
        setStatus("Failed to load friends: " + cause);
    }

    protected void setStatus (String text)
    {
        _content.setText(0, 0, text);
    }

    protected FlexTable _content;
}
