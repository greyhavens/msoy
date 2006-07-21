package com.threerings.msoy.client {

import flash.text.TextField;

import mx.containers.VBox;

import mx.core.UITextField;

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
        // TODO: piece of festering shit...
        // I want to use a UITextField here so we can get all the standard
        // love from using an advanced ui toolkit, but it is broken here
        // for reasons beyond me. We can use a TextField and it will at
        // least work.
        _message = new TextField();
        setMessage(msg);
        rawChildren.addChild(_message);
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

    // TODO: hack function required by TopPanel
    public function setViewSize (w :Number, h :Number) :void
    {
        width = w;
        height = h;
    }

    protected var _ctx :MsoyContext;

    protected var _message :TextField;
}
}
