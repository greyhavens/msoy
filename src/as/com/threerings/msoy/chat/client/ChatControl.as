//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.DisplayObjectContainer;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import flash.ui.Keyboard;

import mx.containers.HBox;

import mx.core.Application;

import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;

/**
 * The chat control widget.
 */
public class ChatControl extends HBox
{
    /**
     * Request focus for the oldest ChatControl.
     */
    public static function grabFocus () :void
    {
        if (_controls.length > 0) {
            (_controls[0] as ChatControl).setFocus();
        }
    }

    public function ChatControl (ctx :WorldContext, height :int)
    {
        _ctx = ctx;
        _chatDtr = _ctx.getChatDirector();

        this.height = height;
        styleName = "chatControl";

        _locObs = new LocationAdapter(null, locationDidChange);

        addChild(_txt = new ChatInput());
        _txt.height = height;

        _but = new CommandButton();
        _but.label = Msgs.CHAT.get("b.send");
        _but.height = height;
        _but.setCallback(sendChat);
        addChild(_but);

        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
    }

    /**
     * Request focus to this chat control.
     */
    override public function setFocus () :void
    {
        _txt.setFocus();
    }

    /**
     * Enables or disables our chat input.
     */
    public function setEnabled (enabled :Boolean) :void
    {
        _txt.enabled = enabled;
        _but.enabled = enabled;
    }

    /**
     * Configures the chat director to which we should send our chat. Pass null to restore our
     * default chat director.
     */
    public function setChatDirector (chatDtr :ChatDirector) :void
    {
        _chatDtr = (chatDtr == null) ? _ctx.getChatDirector() : chatDtr;
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            // set up any already-configured text
            _txt.text = _curLine;

            // request focus
            callLater(function () :void {
                _txt.setFocus();
            });

            _ctx.getLocationDirector().addLocationObserver(_locObs);
            _controls.push(this);

        } else {
            _curLine = _txt.text;

            _ctx.getLocationDirector().removeLocationObserver(_locObs);
            ArrayUtil.removeAll(_controls, this);
        }
    }

    /**
     * Handles FlexEvent.ENTER and the action from the send button.
     */
    protected function sendChat (... ignored) :void
    {
        var message :String = StringUtil.trim(_txt.text);
        if ("" == message) {
            return;
        }

        var result :String = _chatDtr.requestChat(null, message, true);
        if (result != ChatCodes.SUCCESS) {
            _ctx.displayFeedback(null, result);
            return;
        }

        // if there was no error, clear the entry area in prep for the next entry event
        _txt.text = "";
    }

    /**
     * Routed from our LocationAdapter, we observe the entry or exit from places.
     */
    protected function locationDidChange (plobj :PlaceObject) :void
    {
        updateTarget();
    }

    /**
     * Called to update the text we display in our target button.
     */
    protected function updateTarget () :void
    {
        var plobj :PlaceObject = _ctx.getLocationDirector().getPlaceObject();
        _txt.enabled = (plobj != null);
    }

    /** Our client-side context. */
    protected var _ctx :WorldContext;

    /** The director to which we are sending chat requests. */
    protected var _chatDtr :ChatDirector;

    /** Our location observer. */
    protected var _locObs :LocationAdapter;

    /** The place where the user may enter chat. */
    protected var _txt :ChatInput;

    /** The button for sending chat. */
    protected var _but :CommandButton;

    /** An array of the currently shown-controls. */
    protected static var _controls :Array = [];

    /** The preserved current line of text when traversing history or carried between instances of
     * ChatControl. */
    protected static var _curLine :String;
}
}
