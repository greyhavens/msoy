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

import com.threerings.mx.controls.CommandMenu;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.crowd.chat.data.ChatCodes;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.web.data.MemberName;

/**
 * The chat control widget.
 */
public class ChatControl extends HBox
{
    public function ChatControl (ctx :MsoyContext)
    {
        _ctx = ctx;

        _locObs = new LocationAdapter(null, locationDidChange);

        _targetBtn = new Button();
        _targetBtn.minWidth = 130;
        _targetBtn.maxWidth = 130;
        _targetBtn.addEventListener(MouseEvent.CLICK, handleTargetClicked);
        addChild(_targetBtn);

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

        updateTarget();
    }

    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);

        if (p != null) {
            // set up any already-configured text
            _txt.text = _curLine;
//            _histIdx = -1;
            var willShowTip :Boolean = StringUtil.isBlank(_curLine);

            // request focus
            callLater(function () :void {
                _txt.setFocus();
                if (willShowTip) {
                    showTip(true);
                }
            });

            _ctx.getLocationDirector().addLocationObserver(_locObs);

        } else {
            showTip(false);
            _curLine = _txt.text;

            _ctx.getLocationDirector().removeLocationObserver(_locObs);
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
//        _histIdx = -1;
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.DOWN:
            selectTarget(true);
            break;

        case Keyboard.UP:
            selectTarget(false);
            break;
        }
    }

// History-related business. Probably not going to be used.
//    /**
//     * Sets the chat input text to the next or previous command
//     * in the cached historical chat commands.
//     */
//    protected function scrollHistory (next :Boolean) :void
//    {
//        var size :int = _ctx.getChatDirector().getCommandHistorySize();
//        if ((_histIdx == -1) || (_histIdx == size)) {
//            _curLine = _txt.text;
//            _histIdx = size;
//        }
//
//        _histIdx = next ? Math.min(_histIdx + 1, size)
//                        : Math.max(_histIdx - 1, 0);
//        var text :String = (_histIdx == size) ? _curLine :
//            _ctx.getChatDirector().getCommandHistory(_histIdx);
//        _txt.text = text;
//        // and position the caret at the end of the entry
//        _txt.setSelection(text.length, text.length);
//    }

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

    /**
     * Routed from our LocationAdapter, we observe the entry or exit
     * from places.
     */
    protected function locationDidChange (plobj :PlaceObject) :void
    {
        updateTarget();
    }

    /**
     * Select a different target from the list of chat targets.
     */
    protected function selectTarget (down :Boolean) :void
    {
        var chatters :Array = _ctx.getChatDirector().getChatters();

        var idx :int = ArrayUtil.indexOf(chatters, _target);
        if (down) {
            idx = Math.min(chatters.length - 1, idx + 1);
        } else {
            idx = Math.max(-1, idx - 1);
        }

        if (idx >= 0) {
            _target = (chatters[idx] as MemberName);
        } else {
            _target = null;
        }
        updateTarget();
    }

    /**
     * Called to update the text we display in our target button.
     */
    protected function updateTarget () :void
    {
        var plobj :PlaceObject = _ctx.getLocationDirector().getPlaceObject();
        var label :String;
        var enabled :Boolean = true;
        if (_target == null) {
            if (plobj == null) {
                label = Msgs.GENERAL.get("l.select_chatter");
                enabled = false;

            } else {
                label = Msgs.GENERAL.get("m.world");
            }
        } else {
            label = _target.toString();
        }

        _targetBtn.label = label;
        _txt.enabled = enabled;
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

    protected function handleTargetClicked (evt :MouseEvent) :void
    {
        var memObj :MemberObject = _ctx.getClientObject();
        // we're going to build a menu containing recent chatters,
        // followed by a friends submenu and a recent people submenu

        // make a list of recent chatters
        var chatters :Array = _ctx.getChatDirector().getChatters();
        chatters = chatters.map(
            function (name :MemberName, index :int, array :Array) :Object {
                return {
                    label: name.toString(),
                    callback: targetChosen,
                    arg: name
                };
            });

        // make a list of online friends
        var friends :Array = memObj.getSortedEstablishedFriends();
        friends = friends.filter(
            function (fe :FriendEntry, index :int, array :Array) :Boolean {
                return fe.online;
            });
        friends = friends.map(
            function (fe :FriendEntry, index :int, array :Array) :Object {
                return {
                    label: fe.name.toString(),
                    callback: targetChosen,
                    arg: fe.name
                };
            });
        if (friends.length == 0) {
            friends.push({ label: Msgs.GENERAL.get("m.no_online_friends") });
        }

        // make a list of people in the room
        var inRoom :Array;
        var plobj :PlaceObject = _ctx.getLocationDirector().getPlaceObject();
        if (plobj != null) {
            inRoom = plobj.occupantInfo.toArray();

            // filter out guests and ourselves
            inRoom = inRoom.filter(
                function (mi :MemberInfo, index :int, array :Array) :Boolean {
                    return !mi.isGuest() && (mi.bodyOid != memObj.getOid());
                });

            inRoom = inRoom.map(
                function (mi :MemberInfo, index :int, array :Array) :Object {
                    return {
                        label: mi.username.toString(),
                        callback: targetChosen,
                        arg: mi.username
                    };
                });

            if (inRoom.length == 0) {
                inRoom.push({ label: Msgs.GENERAL.get("m.no_occupants") });
            }
        }

        // finally, make a list of "recent" people
        var recent :Array = [ { label: "TODO" } ];

        var menuData :Array = chatters;

        // if we're in a place, add 'world' as a choice
        if (plobj != null) {
            menuData.unshift({ type: "separator" });
            menuData.unshift({ label: Msgs.GENERAL.get("m.world"),
                               callback: targetChosen });
        }

        menuData.push({ type: "separator"});
        menuData.push({ label: Msgs.GENERAL.get("l.friends"),
                        children: friends });
        if (inRoom != null) {
            menuData.push({ label: Msgs.GENERAL.get("l.in_room"),
                            children: inRoom });
        }
        menuData.push({ label: Msgs.GENERAL.get("l.recent_people"),
                        children: recent });

        var menu :CommandMenu =
            CommandMenu.createMenu(_ctx.getRootPanel(), menuData);
        menu.popUp(_targetBtn);
    }

    /**
     * A callback that's called when a new chat target is selected.
     */
    protected function targetChosen (name :MemberName) :void
    {
        _target = name;
        updateTarget();
    }

    /** Our client-side context. */
    protected var _ctx :MsoyContext;

    /** Our location observer. */
    protected var _locObs :LocationAdapter;

    /** The button that may be clicked to choose a new chat target. */
    protected var _targetBtn :Button;

    /** Our actual currently-selected chat target, or null for 'world'. */
    protected var _target :MemberName;

    /** The place where the user may enter chat. */
    protected var _txt :TextInput;

//    /** The current index in the chat command history. */
//    protected var _histIdx :int = -1;

    /** Are we showing the text entry tip? */
    protected var _showingTip :Boolean = false;

    /** The preserved current line of text when traversing history or
     * carried between instances of ChatControl. */
    protected static var _curLine :String;
}
}
