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

import mx.controls.Button;
import mx.controls.TextInput;

import mx.events.FlexEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandMenu;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.MemberName;

/**
 * The chat control widget.
 */
public class ChatControl extends HBox
{
    public function ChatControl (ctx :WorldContext, height :int)
    {
        _ctx = ctx;

        this.height = height;
        styleName = "chatControl";
        
        _locObs = new LocationAdapter(null, locationDidChange);

        addChild(_txt = new ChatInput());
        _txt.styleName = "chatInput";
        _txt.height = height;
        _txt.width = 200;
        
        _but = new Button();
        _but.label = Msgs.GENERAL.get("b.send");
        _but.height = height;
        addChild(_but);

//        _txt.addEventListener(KeyboardEvent.KEY_UP, keyEvent, false, 0, true);
        _txt.addEventListener(FlexEvent.ENTER, sendChat, false, 0, true);
        _but.addEventListener(FlexEvent.BUTTON_DOWN, sendChat, false, 0, true);

//        updateTarget();
    }

    /**
     * Called by various entities to start doing a Tell on someone.
     */
    public static function initiateTell (name :MemberName) :void
    {
        for each (var control :ChatControl in _controls) {
            control.initiateTell(name);
        }
    }

    /**
     * Request focus for the oldest ChatControl.
     */
    public static function grabFocus () :void
    {
        if (_controls.length > 0) {
            (_controls[0] as ChatControl).setFocus();
        }
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
     * Initiate a tell to the specified user.
     */
    public function initiateTell (name :MemberName) :void
    {
//        _ctx.getChatDirector().addChatter(name);
//        _target = name;
//        updateTarget();
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
     * Handles FlexEvent.ENTER and FlexEvent.BUTTON_DOWN to send chat.
     */
    protected function sendChat (event :FlexEvent) :void
    {
        var message :String = StringUtil.trim(_txt.text);
        if ("" == message) {
            return;
        }

        if (_target == null) {
            var result :String = _ctx.getChatDirector().requestChat(
                null, message, true);
            if (result != ChatCodes.SUCCESS) {
                _ctx.displayFeedback(null, result);
                return;
            }

        } else {
            _ctx.getChatDirector().requestTell(_target, message, null);
        }

        // if there was no error, clear the entry area in prep
        // for the next entry event
        _txt.text = "";
    }

//    protected function keyEvent (event :KeyboardEvent) :void
//    {
//        switch (event.keyCode) {
//        case Keyboard.DOWN:
//            selectTarget(true);
//            break;
//
//        case Keyboard.UP:
//            selectTarget(false);
//            break;
//        }
//    }

    /**
     * Routed from our LocationAdapter, we observe the entry or exit
     * from places.
     */
    protected function locationDidChange (plobj :PlaceObject) :void
    {
        updateTarget();
    }

//    /**
//     * Select a different target from the list of chat targets.
//     */
//    protected function selectTarget (down :Boolean) :void
//    {
//        var chatters :Array = _ctx.getChatDirector().getChatters();
//
//        var idx :int = ArrayUtil.indexOf(chatters, _target);
//        if (down) {
//            idx = Math.min(chatters.length - 1, idx + 1);
//        } else {
//            idx = Math.max(-1, idx - 1);
//        }
//
//        if (idx >= 0) {
//            _target = (chatters[idx] as MemberName);
//        } else {
//            _target = null;
//        }
//        updateTarget();
//    }

    /**
     * Called to update the text we display in our target button.
     */
    protected function updateTarget () :void
    {
        var plobj :PlaceObject = _ctx.getLocationDirector().getPlaceObject();
        _txt.enabled = (plobj != null);
//        var label :String;
//        var enabled :Boolean = true;
//        if (_target == null) {
//            if (plobj == null) {
//                label = Msgs.GENERAL.get("l.select_chatter");
//                enabled = false;
//
//            } else {
//                label = Msgs.GENERAL.get("m.world");
//            }
//        } else {
//            label = _target.toString();
//        }
//
//        _txt.enabled = enabled;
    }

    /** Our client-side context. */
    protected var _ctx :WorldContext;

    /** Our location observer. */
    protected var _locObs :LocationAdapter;

    /** Our actual currently-selected chat target, or null for 'world'. */
    protected var _target :MemberName;

    /** The place where the user may enter chat. */
    protected var _txt :ChatInput;

    /** The button for sending chat. */
    protected var _but :Button;

    /** An array of the currently shown-controls. */
    protected static var _controls :Array = [];

    /** The preserved current line of text when traversing history or
     * carried between instances of ChatControl. */
    protected static var _curLine :String;
}
}
