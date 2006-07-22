package com.threerings.msoy.client {

import flash.events.Event;

import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UITextField;

import mx.events.FlexEvent;

import com.threerings.util.Name;
import com.threerings.util.StringUtil;

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
        _email.text = Prefs.getUsername();
        addChild(_email);

        label = new UITextField();
        label.text = ctx.xlate("l.password");
        addChild(label);

        _password = new TextInput();
        _password.displayAsPassword = true;
        _password.text = Prefs.getPassword();
        addChild(_password);

        _logonBtn = new Button();
        _logonBtn.label = ctx.xlate("b.logon");
        addChild(_logonBtn);

        _password.addEventListener(FlexEvent.ENTER, doLogon, false, 0, true);
        _logonBtn.addEventListener(
            FlexEvent.BUTTON_DOWN, doLogon, false, 0, true);

        _email.addEventListener(Event.CHANGE, checkTexts, false, 0, true);
        _password.addEventListener(Event.CHANGE, checkTexts, false, 0, true);
    }

    /**
     * Are the username/password fields non-blank such that we can attempt
     * logon?
     */
    protected function canTryLogon () :Boolean
    {
        return (!StringUtil.isBlank(_email.text) &&
            !StringUtil.isBlank(_password.text));
    }

    /**
     * Handles Event.CHANGE events from the text input fields.
     */
    protected function checkTexts (event :Event) :void
    {
        _logonBtn.enabled = canTryLogon();
    }

    /**
     * Handles FlexEvent.ENTER or FlexEvent.BUTTON_DOWN events
     * generated to process a logon. 
     */
    protected function doLogon (event :FlexEvent) :void
    {
        if (!canTryLogon()) {
            // we disable the button, but they could still try pressing
            // return in the password field, and I don't want to mess
            // with adding/removing the listener in checkTexts
            return;
        }

        dispatchEvent(new CommandEvent(MsoyController.LOGON,
            new MsoyCredentials(new Name(_email.text), _password.text)));
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    protected var _email :TextInput;
    protected var _password :TextInput;

    protected var _logonBtn :Button;
}
}
