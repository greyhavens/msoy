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
    public function FriendApprovalPanel (ctx :MsoyContext, asker :MemberName)
    {
        super(ctx, ctx.xlate(null, "t.approve_friend"));
        _asker = asker;
        open(false); // non-modal..
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(MsoyUI.createLabel(
            _ctx.xlate(null, "m.approve_friend", _asker)));

        addButtons(CANCEL_BUTTON, OK_BUTTON);
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :Button = super.createButton(buttonId);
        if (buttonId == CANCEL_BUTTON) {
            btn.label = _ctx.xlate(null, "b.deny_friend");
        }
        return btn;
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        var approve :Boolean = (buttonId == OK_BUTTON);
        super.buttonClicked(buttonId);
        CommandEvent.dispatch(_ctx.getRootPanel(),
            MsoyController.ALTER_FRIEND, [ _asker.getMemberId(), approve ]);
    }

    protected var _asker :MemberName;
}
}
