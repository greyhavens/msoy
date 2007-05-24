//
// $Id$

package com.threerings.msoy.client.notifications {

import mx.containers.HBox;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.flex.CommandButton;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.FriendStatusChangeNotification;
import com.threerings.msoy.world.client.WorldDirector;
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
        box.percentWidth = 100;
        addChild(box);
        
        var label :Label = new Label();
        label.text = Msgs.GENERAL.get(_loggedOn ? "m.friend_online" : "m.friend_offline",
                                      _friend.toString());
        box.addChild(label);

        if (_loggedOn) {
            var spacer :Spacer = new Spacer();
            spacer.percentWidth = 100;
            box.addChild(spacer);
            
            var button :CommandButton = new CommandButton();
            button.label = Msgs.GENERAL.get("b.visit_friend");
            button.height = 20;
            button.setCallback(visit);
            box.addChild(button);
        }
    }

    protected function visit () :void
    {
        var world :WorldDirector = _dispatch.getWorldContext().getWorldDirector();
        world.goToMemberHome(_friend.getMemberId());
    }
    
    protected var _friend :MemberName;
    protected var _loggedOn :Boolean;
}
}
