//
// $Id$

package com.threerings.msoy.client.notifications {

import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Label;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.FriendStatusChangeNotification;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.FloatingPanel;

public class FriendStatusChangeDisplay extends NotificationDisplay
{
    public function FriendStatusChangeDisplay (
        n :FriendStatusChangeNotification, dispatch :NotificationHandler)
    {
        super(n, dispatch);

        _friend = n.friend;
        _loggedOn = n.loggedOn;
    }

    // from TitleWindow
    override protected function createChildren () :void
    {
        super.createChildren();

        var box :HBox = new HBox();
        box.styleName = "notificationBox";
        addChild(box);
        
        var label :Label = new Label();
        label.text = Msgs.GENERAL.get(_loggedOn ? "m.friend_online" : "m.friend_offline",
                                      _friend.toString());
        box.addChild(label);

        var button :Button = new Button();
        button.label = Msgs.GENERAL.get("b.visit_friend");
        button.height = 20;
        box.addChild(button);
    }        

    protected var _friend :MemberName;
    protected var _loggedOn :Boolean;
}
}
