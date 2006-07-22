package com.threerings.msoy.chat.client {

import flash.events.Event;
import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.TextInput;

import mx.events.FlexEvent;

import mx.utils.StringUtil;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MediaData;

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
        _txt.styleName = "chatInput";
        var but :Button = new Button();
        but.label = ctx.xlate("b.send");
        addChild(but);

        addEventListener(Event.ADDED, wasAdded, false, 0, true);
        addEventListener(Event.REMOVED, wasRemoved, false, 0, true);

        //_txt.addEventListener(FlexEvent.ENTER, sendChat);
        //but.addEventListener(FlexEvent.BUTTON_DOWN, sendChat);
        _txt.addEventListener(KeyboardEvent.KEY_UP, keyEvent, false, 0, true);
        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        but.addEventListener(FlexEvent.BUTTON_DOWN, sendChat, false, 0, true);
    }

    /**
     * Called when this is added to the display hierarchy.
     */
    protected function wasAdded (event :Event) :void
    {
        // set up any already-configured text
        _txt.text = _curLine;
        _histIdx = -1;

        // request focus
        if (_txt.focusManager != null) {
            _txt.focusManager.setFocus(_txt);
        }
    }

    /**
     * Called when this is removed to the display hierarchy.
     */
    protected function wasRemoved (event :Event) :void
    {
        _curLine = _txt.text;
    }

    /**
     * Handles FlexEvent.ENTER and FlexEvent.BUTTON_DOWN to send chat.
     */
    protected function sendChat (event :FlexEvent) :void
    {
        var message :String = StringUtil.trim(_txt.text);
        if ("" == message) {
            return;
        }

        var result :String = _ctx.getChatDirector().requestChat(
            null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _ctx.displayFeedback(null, result);

        } else {
            // if there was no error, clear the entry area in prep
            // for the next entry event
            _txt.text = "";
            _histIdx = -1;
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.DOWN:
            scrollHistory(true);
            break;

        case Keyboard.UP:
            scrollHistory(false);
            break;
        }
    }

    /**
     * Sets the chat input text to the next or previous command
     * in the cached historical chat commands.
     */
    protected function scrollHistory (next :Boolean) :void
    {
        var size :int = _ctx.getChatDirector().getCommandHistorySize();
        if ((_histIdx == -1) || (_histIdx == size)) {
            _curLine = _txt.text;
            _histIdx = size;
        }

        _histIdx = next ? Math.min(_histIdx + 1, size)
                        : Math.max(_histIdx - 1, 0);
        var text :String = (_histIdx == size) ? _curLine :
            _ctx.getChatDirector().getCommandHistory(_histIdx);
        _txt.text = text;
        // and position the caret at the end of the entry
        _txt.setSelection(text.length, text.length);
    }

    /** Our client-side context. */
    protected var _ctx :MsoyContext;

    protected var _txt :TextInput;

    /** The current index in the chat command history. */
    protected var _histIdx :int = -1;

    /** The preserved current line of text when traversing history or
     * carried between instances of ChatControl. */
    protected static var _curLine :String;
}
}
