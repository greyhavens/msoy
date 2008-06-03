//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TimerEvent;
import flash.text.TextField;
import flash.utils.Timer;

import com.threerings.flex.CommandButton;
import com.threerings.util.Log;
import com.threerings.util.MultiLoader;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.world.data.RoomObject;

/**
 * Configures the control bar with World-specific stuff.
 */
public class WorldControlBar extends ControlBar
{
    /**
     * This is needed by the room controller, so that it can enable/disable the edit button.
     */
    public function get roomEditBtn () :CommandButton
    {
        return _roomeditBtn;
    }

    /**
     * Receives notification whether scene editing is possible for this scene.
     */
    public function set sceneEditPossible (value :Boolean) :void
    {
        var editButtons :Array = [ _roomeditBtn, _snapBtn ];
        editButtons.forEach(function (button :CommandButton, i :*, a :*) :void {
            if (button != null) {
                button.enabled = value;
            }
        });

        // TODO: testing only (robert)
        if (_ctx.getTokens().isAdmin() && _snapBtn != null) {
            _snapBtn.enabled = value;
        }
    }

    /**
     * Sets whether or not there are notifications available for review.
     */
    public function setNotificationsAvailable (avail :Boolean) :void
    {
        // NOOP - notifications do not persist: TODO: fully rip out the old notification system
    }

    /**
     * Sets whether the notification display is showing or not. Called by the NotificationDirector.
     */
    public function setNotificationsShowing (showing :Boolean) :void
    {
        // NOOP - see setNotificationsAvailable
    }

    // from ControlBar
    override public function miniChanged () :void
    {
        _isEditing = (_isEditing && !_ctx.getTopPanel().isMinimized());
        super.miniChanged();
    }

    // from ControlBar
    override public function locationDidChange (place :PlaceObject) :void
    {
        super.locationDidChange(place);

        // if we just moved into a room...
        if (place is RoomObject) {
            // we may want to display our "click here to chat" tip
            // TODO: this thing is broken again and doesn't go away when it should - or ever, in
            // some cases.  It can be re-enabled when we get a chance to get it working.
            //maybeDisplayChatTip();
            // possibly also show the avatar introduction
            maybeDisplayAvatarIntro();
        }
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

        _roomeditBtn = new CommandButton();
        _roomeditBtn.toolTip = Msgs.GENERAL.get("i.editScene");
        _roomeditBtn.setCommand(MsoyController.ROOM_EDIT);
        _roomeditBtn.styleName = "controlBarButtonEdit";
        _roomeditBtn.enabled = false;

        _hotZoneBtn = new CommandButton();
        _hotZoneBtn.toolTip = Msgs.GENERAL.get("i.hover");
        _hotZoneBtn.styleName = "controlBarHoverZone";
        _hotZoneBtn.enabled = false;
        _hotZoneBtn.focusEnabled = false;
        var hotHandler :Function = function (event :MouseEvent) :void {
            var roomView :RoomObjectView = _ctx.getTopPanel().getPlaceView() as RoomObjectView;
            if (roomView != null) {
                roomView.getRoomObjectController().hoverAllFurni(
                    event.type == MouseEvent.ROLL_OVER);
            }
        };
        _hotZoneBtn.addEventListener(MouseEvent.ROLL_OVER, hotHandler);
        _hotZoneBtn.addEventListener(MouseEvent.ROLL_OUT, hotHandler);

        _snapBtn = new CommandButton();
        _snapBtn.toolTip = Msgs.GENERAL.get("i.snapshot");
        _snapBtn.setCommand(MsoyController.SNAPSHOT);
        _snapBtn.styleName = "controlBarButtonSnapshot";
        _snapBtn.enabled = true;
    }

    // from ControlBar
    override protected function checkControls () :void
    {
        super.checkControls();
        _isEditing = false;
    }

    // from ControlBar
    override protected function addControlButtons () :void
    {
        addGroupChild(_roomeditBtn, [ UI_STD ]);
        addGroupChild(_hotZoneBtn, [ UI_STD, UI_GUEST ]);
        // TODO: snapshots are not functional; revisit
        if (_ctx.getTokens() != null && _ctx.getTokens().isAdmin()) {
            addGroupChild(_snapBtn, [ UI_STD ]);
        }
    }

    // from ControlBar
    override protected function getMode () :String
    {
        var mode :String = super.getMode();
        return (mode == UI_STD && _isEditing) ? UI_EDIT : mode;
    }

    protected function maybeDisplayChatTip () :void
    {
        // if we've already shown the tip, have no chat control or they have been a member for a
        // while, don't show the chat tip
        var mobj :MemberObject = (_ctx as WorldContext).getMemberObject();
        if (_chatTip != null || _chatControl == null || mobj.level >= CHAT_TIP_GRADUATE_LEVEL ||
            Prefs.getSlidingChatHistory()) {
            return;
        }

        // create, position and add our chat tip sprite
        _chatTip = (new CHAT_TIP() as DisplayObject);
        _chatTip.x = 5;
        _chatTip.y = _ctx.getTopPanel().getPlaceContainer().height - _chatTip.height - 5;
        fadeIn(_chatTip);
        _ctx.getTopPanel().getPlaceContainer().addOverlay(_chatTip, PlaceBox.LAYER_TRANSIENT);

        // when they click or type in the chat entry, we want to remove the sprite
        var onAction :Function = function (... ignored) :void {
            _chatControl.chatInput.removeEventListener(KeyboardEvent.KEY_DOWN, onAction);
            _chatControl.chatInput.removeEventListener(MouseEvent.MOUSE_DOWN, onAction);
            fadeOutAndRemove(_chatTip);
        };
        _chatControl.chatInput.addEventListener(KeyboardEvent.KEY_DOWN, onAction);
        _chatControl.chatInput.addEventListener(MouseEvent.MOUSE_DOWN, onAction);

        // or clear it out if they haven't already after ten seconds
        new Timer(10000, 1).addEventListener(TimerEvent.TIMER, onAction);
    }

    protected function maybeDisplayAvatarIntro () :void
    {
        // if we have already shown the intro, they are a guest, are not wearing the tofu avatar,
        // or have ever worn any non-tofu avatar, don't show the avatar intro
        var mobj :MemberObject = (_ctx as WorldContext).getMemberObject();
        if (_avatarIntro != null || mobj.isGuest() || mobj.avatar != null ||
            mobj.avatarCache.size() > 0) {
            return;
        }

        MultiLoader.getContents(AVATAR_INTRO, function (result :DisplayObjectContainer) :void {
            _avatarIntro = result;
            _avatarIntro.x = 15;

            var title :TextField = (_avatarIntro.getChildByName("txt_welcome") as TextField);
            title.text = Msgs.GENERAL.get("t.avatar_intro");

            var info :TextField = (_avatarIntro.getChildByName("txt_description") as TextField);
            info.text = Msgs.GENERAL.get("m.avatar_intro");

            var close :SimpleButton = (_avatarIntro.getChildByName("btn_nothanks") as SimpleButton);
            close.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                fadeOutAndRemove(_avatarIntro);
            });

            var go :SimpleButton = (_avatarIntro.getChildByName("btn_gotoshop") as SimpleButton);
            go.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                (_ctx as WorldContext).getWorldController().handleViewAvatarCatalog();
                fadeOutAndRemove(_avatarIntro);
            });

            fadeIn(_avatarIntro);
            _ctx.getTopPanel().getPlaceContainer().addOverlay(
                _avatarIntro, PlaceBox.LAYER_TRANSIENT);
        });
    }

    protected function fadeIn (thing :DisplayObject) :void
    {
        thing.alpha = 0;
        thing.addEventListener(Event.ENTER_FRAME, function (event :Event) :void {
            if (thing.alpha >= 1) {
                thing.removeEventListener(Event.ENTER_FRAME, arguments.callee);
            } else {
                thing.alpha += 0.05;
            }
        });
    }

    protected function fadeOutAndRemove (thing :DisplayObject) :void
    {
        // can't use a fancy Dissolve effect on non-Flex components, so we roll our own
        thing.addEventListener(Event.ENTER_FRAME, function (event :Event) :void {
            if (thing.alpha <= 0) {
                if (thing.parent != null) {
                    _ctx.getTopPanel().getPlaceContainer().removeOverlay(thing);
                }
                thing.removeEventListener(Event.ENTER_FRAME, arguments.callee);
            } else {
                thing.alpha -= 0.05;
            }
        });
    }

    /** Are we in room editing mode? */
    protected var _isEditing :Boolean;

    /** Button for editing the current scene. */
    protected var _roomeditBtn :CommandButton;

    /** Hovering over this shows clickable components. */
    protected var _hotZoneBtn :CommandButton;

    /** Button for room snapshots. */
    protected var _snapBtn :CommandButton;

    /** A tip shown when we first enter a room. */
    protected var _chatTip :DisplayObject;

    /** An introduction to avatars shown to brand new players. */
    protected var _avatarIntro :DisplayObjectContainer;

    /** We stop showing the "type here to chat" tip after the user reaches level 5. */
    protected static const CHAT_TIP_GRADUATE_LEVEL :int = 5;

    [Embed(source="../../../../../../../rsrc/media/chat_tip.swf")]
    protected static const CHAT_TIP :Class;

    [Embed(source="../../../../../../../rsrc/media/avatar_intro.swf",
           mimeType="application/octet-stream")]
    protected static const AVATAR_INTRO :Class;
}
}
