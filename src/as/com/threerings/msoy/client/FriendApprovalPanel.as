//
// $Id$

package com.threerings.msoy.client {

import mx.controls.Button;

import com.threerings.mx.events.CommandEvent;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberName;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MsoyUI;

public class FriendApprovalPanel extends FloatingPanel
{
    public static const DENY_BUTTON :int = -1;

    public function FriendApprovalPanel (ctx :MsoyContext, asker :MemberName)
    {
        super(ctx, Msgs.GENERAL.get("t.approve_friend"));
        _asker = asker;
        open(false); // non-modal..
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(MsoyUI.createLabel(
            Msgs.GENERAL.get("m.approve_friend", _asker)));

        addButtons(DENY_BUTTON, OK_BUTTON);
    }

    override protected function createButton (buttonId :int) :Button
    {
        if (buttonId == DENY_BUTTON) {
            var btn :Button = new Button();
            btn.label = Msgs.GENERAL.get("b.deny_friend");
            return btn;
        }

        return super.createButton(buttonId);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        var approve :Boolean = (buttonId == OK_BUTTON);
        switch (buttonId) {
        case DENY_BUTTON:
            close();
            break;

        default:
            super.buttonClicked(buttonId);
            break;
        }
        CommandEvent.dispatch(_ctx.getRootPanel(),
            MsoyController.ALTER_FRIEND, [ _asker.getMemberId(), approve ]);
    }

    protected var _asker :MemberName;
}
}
