//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.web.gwt.EmailContact;

import client.ui.MsoyUI;
import client.ui.TongueBox;

/**
 * Presents various ways to find friends already on Whirled.
 */
public class FindFriendsPanel extends InvitePanel
{
    public FindFriendsPanel ()
    {
        addStyleName("findFriends");

        add(MsoyUI.createLabel(_msgs.ffIntro(), "Title"));

        add(new TongueBox(_msgs.ffWebmail(), new WebMailControls(_msgs.ffCheckWebmail(), _msgs.ffFind()) {
            protected void handleAddresses (List<EmailContact> addresses) {
            }
        }));
    }
}
