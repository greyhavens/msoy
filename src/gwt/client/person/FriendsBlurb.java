//
// $Id$

package client.person;

import java.util.ArrayList;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.web.data.FriendInfo;

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
        setTitle("Friends");

        ArrayList friends = (ArrayList)blurbData;
        if (friends.size() == 0) {
            setStatus("You have no friends. Boo hoo.");

        } else {
            for (int ii = 0, ll = friends.size(); ii < ll; ii++) {
                FriendInfo friend = (FriendInfo)friends.get(ii);
                Hyperlink link = new Hyperlink(friend.name,
                    String.valueOf(friend.memberId));
                _content.setWidget(ii, 0, link);
            }
        }
    }

    // @Override // from Blurb
    protected void didFail (String cause)
    {
        setTitle("Error");
        setStatus("Failed to load friends: " + cause);
    }

    protected void setStatus (String text)
    {
        _content.setText(0, 0, text);
    }

    protected FlexTable _content;
}
