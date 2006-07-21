package com.threerings.msoy.client {

import flash.events.Event;

import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UITextField;

import com.threerings.util.Name;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCredentials;

public class LogonPanel extends HBox
{
    public function LogonPanel (ctx :MsoyContext)
    {
        _ctx = ctx;
        var label :UITextField = new UITextField();
        label.text = ctx.xlate("l.email");
        addChild(label);

        _email = new TextInput();
        addChild(_email);

        label = new UITextField();
        label.text = ctx.xlate("l.password");
        addChild(label);

        _password = new TextInput();
        _password.displayAsPassword = true;
        addChild(_password);

        var but :Button = new Button();
        but.label = ctx.xlate("b.logon");
        addChild(but);

        _password.addEventListener("enter", doLogon, false, 0, true);
        but.addEventListener("buttonDown", doLogon, false, 0, true);
    }

    /**
     * Handles 
     */
    protected function doLogon (event :Event) :void
    {
        dispatchEvent(new CommandEvent(MsoyController.LOGON,
            new MsoyCredentials(new Name(_email.text), _password.text)));
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    protected var _email :TextInput;
    protected var _password :TextInput;
}
}
