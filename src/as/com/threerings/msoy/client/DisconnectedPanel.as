//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import mx.containers.VBox;

import mx.controls.Text;

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

        setStyle("horizontalAlign", "center");
        setStyle("verticalAlign", "middle");

        _message = new Text();
        _message.setStyle("fontSize", 12);
        _message.setStyle("fontWeight", "bold");
        _message.styleName = "topLevelLabel";
        addChild(_message);

        if (msg != null) {
            setMessage(msg, true);
        }
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
    public function setMessage (msg :String, isHtml :Boolean = false) :void
    {
        if (isHtml) {
            _message.htmlText = Msgs.GENERAL.xlate(msg);
        } else {
            _message.text = Msgs.GENERAL.xlate(msg);
        }
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
            setMessage(msg);
        }
    }

    /**
     * Return a translatable String that sums up the cause of the
     * logon error.
     */
    protected static function decodeLogonError (cause :Error) :String
    {
        var msg :String;
        if (cause is LogonError || cause.message.match("^[em]\\.")) {
            msg = cause.message;
        } else {
            msg = "m.network_error";
        }
        return msg;
    }

    protected var _ctx :MsoyContext;
    protected var _clientObs :ClientAdapter;
    protected var _message :Text;
}
}
