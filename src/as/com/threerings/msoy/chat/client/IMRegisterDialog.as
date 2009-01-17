//
// $Id$

package com.threerings.msoy.chat.client {

import mx.controls.TextInput;
import mx.containers.Grid;
import mx.containers.VBox;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.util.MessageBundle;

public class IMRegisterDialog extends FloatingPanel
{
    public function IMRegisterDialog (ctx :MsoyContext, gateway :String)
    {
        super(ctx, Msgs.CHAT.xlate(MessageBundle.compose("t.im_registration", "m." + gateway)));
        _gateway = gateway;
        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var tainer :VBox = new VBox();
        tainer.label = Msgs.PREFS.get("t.chat");

        var grid :Grid = new Grid();

        _username = new TextInput();
        _username.width = 100;
        _password = new TextInput();
        _password.width = 100;
        _password.displayAsPassword = true;

        GridUtil.addRow(grid, Msgs.CHAT.get("l.im_username"), _username);
        GridUtil.addRow(grid, Msgs.CHAT.get("l.im_password"), _password);

        tainer.addChild(grid);
        addChild(tainer);

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function okButtonClicked () :void
    {
        (_ctx.getChatDirector() as MsoyChatDirector).registerIM(
            _gateway, _username.text, _password.text);
    }

    protected var _username :TextInput;
    protected var _password :TextInput;
    protected var _gateway :String;
}
}
