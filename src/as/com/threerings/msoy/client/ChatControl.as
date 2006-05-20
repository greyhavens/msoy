package com.threerings.msoy.client {

import flash.events.Event;

import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.TextInput;

import com.threerings.crowd.chat.data.ChatCodes;

/**
 * IMPORTANT NOTE: this class was written for testing things and does not
 * necessarily represent a valid starting point for writing the chat
 * widget we'll eventually need.
 */
public class ChatControl extends HBox
{
    public function ChatControl (ctx :MsoyContext)
    {
        _ctx = ctx;

        addChild(_txt = new TextInput());
        var but :Button = new Button();
        but.label = "Send"; // TODO: xlate
        addChild(but);

        //_txt.addEventListener(FlexEvent.ENTER, sendChat);
        //but.addEventListener(FlexEvent.BUTTON_DOWN, sendChat);
        _txt.addEventListener("enter", sendChat);
        but.addEventListener("buttonDown", sendChat);
    }

    protected function sendChat (event :Event) :void
    {
        var message :String = _txt.text;
        _txt.text = "";
        var result :String = _ctx.getChatDirector().requestChat(
            null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _ctx.displayFeedback(null, result);
        }
    }

    /** Our client-side context. */
    protected var _ctx :MsoyContext;

    protected var _txt :TextInput;
}
}
