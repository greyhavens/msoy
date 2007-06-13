//
// $Id$

package com.threerings.msoy.client.notifications {

import mx.containers.HBox;
import mx.controls.Label;
import mx.controls.Spacer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.data.FriendStatusChangeNotification;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.WorldDirector;

public class FriendStatusChangeDisplay extends NotificationDisplay
{
    public function FriendStatusChangeDisplay (
        dispatch :NotificationHandler, n :FriendStatusChangeNotification)
    {
        super(dispatch, n);

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
            
            var button :CommandButton = new CommandButton(
                MsoyController.GO_MEMBER_SCENE, _friend.getMemberId());
            button.label = Msgs.GENERAL.get("b.visit_friend");
            button.height = 20;
            box.addChild(button);
        }
    }

    protected var _friend :MemberName;
    protected var _loggedOn :Boolean;
}
}
