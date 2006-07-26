package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import flash.events.Event;

import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.Label;
import mx.controls.TextInput;

import mx.core.UITextField;

import mx.events.FlexEvent;

import com.adobe.crypto.MD5;

import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.mx.controls.CommandButton;
import com.threerings.mx.events.CommandEvent;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MsoyUserObject;

public class LogonPanel extends HBox
{
    public function LogonPanel (ctx :MsoyContext)
    {
        _ctx = ctx;
        _clientObs = new ClientAdapter(
            recheckGuest, recheckGuest, recheckGuest, recheckGuest,
            recheckGuest, recheckGuest, recheckGuest);

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
        addChild(_password);

        _logonBtn = new Button();
        _logonBtn.label = ctx.xlate("b.logon");
        _logonBtn.enabled = false;
        addChild(_logonBtn);

        _guestBtn = new CommandButton(MsoyController.LOGON);
        _guestBtn.label = ctx.xlate("b.logon_guest");

        _password.addEventListener(FlexEvent.ENTER, doLogon, false, 0, true);
        _logonBtn.addEventListener(
            FlexEvent.BUTTON_DOWN, doLogon, false, 0, true);

        _email.addEventListener(Event.CHANGE, checkTexts, false, 0, true);
        _password.addEventListener(Event.CHANGE, checkTexts, false, 0, true);
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            _ctx.getClient().addClientObserver(_clientObs);
            recheckGuest(null);

        } else {
            _ctx.getClient().removeClientObserver(_clientObs);
        }
    }

    /**
     * Called to configure the current user.
     */
    public function recheckGuest (event :ClientEvent) :void
    {
        // we only get the option here to log in as a guest if
        // we aren't even logged in
        _guestBtn.visible = (_ctx.getClientObject() == null);
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

        var creds :MsoyCredentials = new MsoyCredentials(
            new Name(_email.text),
            MD5.hash(_password.text));
        creds.ident = ""; // TODO?

        dispatchEvent(new CommandEvent(MsoyController.LOGON, creds));
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    protected var _email :TextInput;
    protected var _password :TextInput;

    protected var _logonBtn :Button;
    protected var _guestBtn :CommandButton;

    protected var _clientObs :ClientAdapter;
}
}
