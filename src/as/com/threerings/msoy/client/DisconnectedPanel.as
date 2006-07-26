package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import flash.errors.IOError;

import flash.text.TextField;

import mx.containers.VBox;

import mx.core.UITextField;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.LogonError;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

/**
 * Shown to users when we're disconnected.
 */
public class DisconnectedPanel extends VBox
    implements PlaceView
{
    public function DisconnectedPanel (ctx :MsoyContext, msg :String = null)
    {
        _ctx = ctx;
        _clientObs = new ClientAdapter(
            clientObserver, clientObserver, clientObserver, clientObserver,
            clientObserver, clientObserver, clientObserver);

        // TODO: piece of festering shit...
        // I want to use a UITextField here so we can get all the standard
        // love from using an advanced ui toolkit, but it is broken here
        // for reasons beyond me. We can use a TextField and it will at
        // least work.
        _message = new TextField();
        setMessage(msg);
        rawChildren.addChild(_message);
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            _ctx.getClient().addClientObserver(_clientObs);

        } else {
            _ctx.getClient().removeClientObserver(_clientObs);
        }
    }

    /**
     * Set the message displayed on the panel.
     */
    public function setMessage (msg :String) :void
    {
        _message.text = (msg == null) ? _ctx.xlate("m.disconnected") : msg;
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nada
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nada
    }

    /**
     * We route all ClientObserver methods here.
     */
    protected function clientObserver (event :ClientEvent) :void
    {
        var msg :String = null;

        if (event.type == ClientEvent.CLIENT_FAILED_TO_LOGON) {
            msg = decodeLogonError(event.getCause());

            // TODO: more special cases for error message will live here

            msg = MessageBundle.compose("m.logon_failed", msg);
        }

        if (msg != null) {
            setMessage(_ctx.xlate(msg));
        }
    }

    // TODO: hack function required by TopPanel
    public function setViewSize (w :Number, h :Number) :void
    {
        width = w;
        height = h;
    }

    /**
     * Return a translatable String that sums up the cause of the
     * logon error.
     */
    protected static function decodeLogonError (cause :Error) :String
    {
        var msg :String;
        if (cause is LogonError) {
            msg = cause.message;

        } else {
            msg = "m.network_error";
        }
        return msg;
    }

    protected var _ctx :MsoyContext;

    protected var _clientObs :ClientAdapter;

    protected var _message :TextField;
}
}
