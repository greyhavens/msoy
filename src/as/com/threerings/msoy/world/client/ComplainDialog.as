//
// $Id$

package com.threerings.msoy.world.client {

import mx.controls.Label;
import mx.controls.Text;
import mx.controls.TextInput;
import mx.containers.VBox;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

public class ComplainDialog extends FloatingPanel
{
    public function ComplainDialog (ctx :MsoyContext, memberId :int, name :String)
    {
        super(ctx, Msgs.GENERAL.xlate(MessageBundle.tcompose("t.complain", name)));
        _memberId = memberId;
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        var tainer :VBox = new VBox();
        var cText :Text = new Text();
        cText.text = Msgs.GENERAL.get("m.complain_descrip");
        cText.width = 350;
        tainer.addChild(cText);

        _complaint = new TextInput();
        _complaint.width = 350;
        tainer.addChild(_complaint);

        _status = new Label();
        _status.text = "";
        tainer.addChild(_status);

        addChild(tainer);
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON && _complaint.text == "") {
            _status.text = Msgs.GENERAL.get("e.complain_required");
            return;
        }
        super.buttonClicked(buttonId);
        if (buttonId == OK_BUTTON) {
            var msvc :MemberService =
                (_ctx.getClient().requireService(MemberService) as MemberService);
            msvc.complainMember(_ctx.getClient(), _memberId, _complaint.text);
        }
    }

    protected var _memberId :int;
    protected var _complaint :TextInput;
    protected var _status :Label;
}
}
