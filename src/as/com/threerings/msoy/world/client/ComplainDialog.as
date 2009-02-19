//
// $Id$

package com.threerings.msoy.world.client {

import mx.controls.Label;
import mx.controls.TextInput;
import mx.containers.VBox;

import com.threerings.util.MessageBundle;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

public class ComplainDialog extends FloatingPanel
{
    public static const MAX_COMPLAINT_CHARS :int = 255;

    public function ComplainDialog (ctx :MsoyContext, name :String, sendComplaint :Function)
    {
        super(ctx, Msgs.GENERAL.xlate(MessageBundle.tcompose("t.complain", name)));
        _sendComplaint = sendComplaint;
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        var tainer :VBox = new VBox();
        tainer.addChild(FlexUtil.createText(Msgs.GENERAL.get("m.complain_descrip"), 350));

        _complaint = new TextInput();
        _complaint.maxChars = MAX_COMPLAINT_CHARS;
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
            _sendComplaint(_complaint.text);
        }
    }

    protected var _complaint :TextInput;
    protected var _status :Label;
    protected var _sendComplaint :Function;
}
}
