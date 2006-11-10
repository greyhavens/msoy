package com.threerings.msoy.chat.client {

import flash.display.DisplayObjectContainer;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.TextEvent;

import flash.ui.Keyboard;

import mx.containers.HBox;

import mx.core.Application;

import mx.controls.Button;
import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.StringUtil;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

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
        but.label = Msgs.GENERAL.get("b.send");
        addChild(but);

        _txt.addEventListener(KeyboardEvent.KEY_UP, keyEvent, false, 0, true);
        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        but.addEventListener(FlexEvent.BUTTON_DOWN, sendChat, false, 0, true);

        _txt.addEventListener(FocusEvent.FOCUS_IN, handleFocusIn,
            false, 0, true);
        _txt.addEventListener(FocusEvent.FOCUS_OUT, handleFocusOut,
            false, 0, true);
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            // set up any already-configured text
            _txt.text = _curLine;
            _histIdx = -1;
            var willShowTip :Boolean = StringUtil.isBlank(_curLine);

            // request focus
            callLater(function () :void {
                _txt.setFocus();
                if (willShowTip) {
                    showTip(true);
                }
            });

        } else {
            showTip(false);
            _curLine = _txt.text;
        }
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

    protected function showTip (show :Boolean) :void
    {
        if (show == _showingTip) {
            return;
        }
        if (show) {
            _txt.setStyle("color", 0x666666);
            _txt.text = Msgs.GENERAL.get("i.chat");
            _txt.addEventListener(TextEvent.TEXT_INPUT, handleTextInput,
                false, 0, true)

        } else {
            _txt.setStyle("color", 0x000000);
            _txt.text = "";
            _txt.removeEventListener(TextEvent.TEXT_INPUT, handleTextInput,
                false);
        }
        _showingTip = show;
    }

    protected function handleFocusIn (evt :FocusEvent) :void
    {
        showTip(false);
    }

    protected function handleFocusOut (evt :FocusEvent) :void
    {
        if (_txt.text == "") {
            showTip(true);
        }
    }

    protected function handleTextInput (evt :TextEvent) :void
    {
        // turn off tippage, set the entered text to what the user entered
        showTip(false);
        callLater(function () :void {
            _txt.text = evt.text;
        });
    }

    /** Our client-side context. */
    protected var _ctx :MsoyContext;

    protected var _txt :TextInput;

    /** The current index in the chat command history. */
    protected var _histIdx :int = -1;

    protected var _showingTip :Boolean = false;

    /** The preserved current line of text when traversing history or
     * carried between instances of ChatControl. */
    protected static var _curLine :String;
}
}
