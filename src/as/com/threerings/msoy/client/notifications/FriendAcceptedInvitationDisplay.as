//
// $Id$

package com.threerings.msoy.client.notifications {

import mx.controls.Text;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.FriendAcceptedInvitationNotification;
import com.threerings.msoy.ui.FloatingPanel;

public class FriendAcceptedInvitationDisplay extends NotificationDisplay
{
    public function FriendAcceptedInvitationDisplay (
        n :FriendAcceptedInvitationNotification, dispatch :NotificationHandler)
    {
        super(n, dispatch);

        _displayName = n.inviteeDisplayName;
        _email = n.invitationEmail;
    }

    // from NotificationDisplay
    override public function get timeout () :Number
    {
        return 10000; // wee bit longer than usual
    }

    // from TitleWindow
    override protected function createChildren () :void
    {
        super.createChildren();

        var label :Text = new Text();
        label.text = Msgs.GENERAL.get("m.invite_accepted", _email, _displayName);
        label.percentWidth = 100;
        addChild(label);
    }

    protected var _displayName :String;
    protected var _email :String;
}
}
