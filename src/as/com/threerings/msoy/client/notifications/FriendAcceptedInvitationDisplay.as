//
// $Id$

package com.threerings.msoy.client.notifications {

import com.threerings.msoy.data.FriendAcceptedInvitationNotification;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.FloatingPanel;

public class FriendAcceptedInvitationDisplay extends NotificationDisplay
{
    public function FriendAcceptedInvitationDisplay (
        n :FriendAcceptedInvitationNotification, dispatch :NotificationHandler)
    {
        super(n, dispatch);

        _friend = n.friend;
    }

    // from NotificationDisplay
    override public function get timeout () :Number
    {
        return 5000;
    }

    // from TitleWindow
    override protected function createChildren () :void
    {
        super.createChildren();

        // to do, so much to do... :)
    }        

    protected var _friend :MemberName;
}
}
