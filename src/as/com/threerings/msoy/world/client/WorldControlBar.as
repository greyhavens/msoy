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

import mx.core.UIComponent;
import mx.core.ScrollPolicy;

import mx.containers.HBox;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.util.Log;
import com.threerings.util.MultiLoader;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.SliderPopup;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.notify.client.NotificationDisplay;
import com.threerings.msoy.party.client.PartyPopup;

import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomStudioView;
import com.threerings.msoy.room.client.snapshot.SnapshotPanel;
import com.threerings.msoy.room.data.RoomObject;

/**
 * Configures the control bar with World-specific stuff.
 */
public class WorldControlBar extends ControlBar
{
    public function WorldControlBar (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * This is needed by the room controller, so that it can enable/disable the edit button.
     */
    public function get roomEditBtn () :CommandButton
    {
        return _roomeditBtn;
    }

    public function enableZoomControl (enabled :Boolean ) :void
    {
        _zoomBtn.enabled = enabled;
        _zoomBtn.toolTip = Msgs.GENERAL.get(enabled ? "i.zoom" : "i.zoom_disabled");
    }

    public function setNotificationDisplay (notificationDisplay :NotificationDisplay) :void
    {
        _notificationDisplay = notificationDisplay;
        setupControls();
        updateUI();
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

    // from Container
    override public function setActualSize (uw :Number, uh :Number) :void
    {
        super.setActualSize(uw, uh);

        checkNotificationDisplay();

        if (_notificationDisplay != null && _notificationDisplay.visible) {
            callLater(_notificationDisplay.updatePopupLocation);
        }
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

        _roomeditBtn = new CommandButton();
        _roomeditBtn.toolTip = Msgs.GENERAL.get("i.editScene");
        _roomeditBtn.setCommand(WorldController.ROOM_EDIT);
        _roomeditBtn.styleName = "controlBarButtonEdit";
        _roomeditBtn.enabled = false;

        _zoomBtn = new CommandButton();
        _zoomBtn.styleName = "controlBarButtonZoom";
        _zoomBtn.toolTip = Msgs.GENERAL.get("i.zoom");
        _zoomBtn.setCallback(handlePopZoom);

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
        _snapBtn.setCallback(FloatingPanel.createPopper(function () :SnapshotPanel {
            return new SnapshotPanel(_wctx);
        }, _snapBtn));
        _snapBtn.styleName = "controlBarButtonSnapshot";
        _snapBtn.enabled = true;

        _friendsBtn = new CommandButton();
        _friendsBtn.toolTip = Msgs.GENERAL.get("i.friends");
        _friendsBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new FriendsListPanel(_wctx);
        }, _friendsBtn));
        _friendsBtn.styleName = "controlBarFriendButton";
        _friendsBtn.enabled = true;
        _friendsBtn.focusEnabled = false;

        // Not ready for consumption
        if (_ctx.getTokens().isAdmin()) {
            _partyBtn = new CommandButton();
            _partyBtn.toolTip = Msgs.GENERAL.get("i.party");
            _partyBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
                return new PartyPopup(_wctx);
            }, _partyBtn));
            _partyBtn.styleName = "controlBarPartyButton";
            _partyBtn.enabled = true;
            _partyBtn.focusEnabled = false;
        }
    }

    // from ControlBar
    override protected function addControlButtons () :void
    {
        super.addControlButtons(); 

        addGroupChild(_hotZoneBtn, [ UI_ROOM ]);
        addGroupChild(_zoomBtn, [ UI_ROOM, UI_VIEWER ]);

        addGroupChild(_snapBtn, [ UI_ROOM ]);
        addGroupChild(_roomeditBtn, [ UI_ROOM ]);

        // TODO: notifications are global, yes? They should be in ControlBar
        if (_notificationDisplay != null) {
            addGroupChild(_notificationDisplay, [ UI_BASE, UI_ROOM, UI_GAME ], LAST_PRIORITY);
        }

        // TODO: enable friends for guests, even if it just goads them into signup
        // TODO: friends are global, yes? They should be in ControlBar
        if (_friendsBtn != null && _isMember) {
            addGroupChild(_friendsBtn, [ UI_BASE, UI_ROOM, UI_GAME ], BUTTON_PRIORITY - 1);
        }
    }

    // from ControlBar
    override protected function getMode () :String
    {
        return UberClient.isRegularClient() ? super.getMode() : UI_VIEWER;
    }

    // from ControlBar
    override protected function updateGroup (groupName :String, value :Boolean) :void
    {
        super.updateGroup(groupName, value);

        if (groupName == UI_MINI && value) {
            checkNotificationDisplay();
        }
    }

    protected function checkNotificationDisplay () :void
    {
        if (getMode() != UI_MINI || _notificationDisplay == null) {
            return;
        }

        callLater(function () :void {
            // if we're mini, make sure we have room for the display
            if (!_notificationDisplay.visible && _rightSpacer.width > _notificationDisplay.width) {
                FlexUtil.setVisible(_notificationDisplay, true);
            } else if (_notificationDisplay.visible && _rightSpacer.width <= 0) {
                FlexUtil.setVisible(_notificationDisplay, false);
            }
        });
    }

//    protected function maybeDisplayChatTip () :void
//    {
//        // if we've already shown the tip, have no chat control or they have been a member for a
//        // while, don't show the chat tip
//        var mobj :MemberObject = _wctx.getMemberObject();
//        if (_chatTip != null || _chatControl == null || mobj.level >= CHAT_TIP_GRADUATE_LEVEL ||
//            Prefs.getSlidingChatHistory()) {
//            return;
//        }
//
//        // create, position and add our chat tip sprite
//        _chatTip = (new CHAT_TIP() as DisplayObject);
//        _chatTip.x = 5;
//        _chatTip.y = _ctx.getTopPanel().getPlaceContainer().height - _chatTip.height - 5;
//        fadeIn(_chatTip);
//        _ctx.getTopPanel().getPlaceContainer().addOverlay(_chatTip, PlaceBox.LAYER_TRANSIENT);
//
//        // when they click or type in the chat entry, we want to remove the sprite
//        var onAction :Function = function (... ignored) :void {
//            _chatControl.chatInput.removeEventListener(KeyboardEvent.KEY_DOWN, onAction);
//            _chatControl.chatInput.removeEventListener(MouseEvent.MOUSE_DOWN, onAction);
//            fadeOutAndRemove(_chatTip);
//        };
//        _chatControl.chatInput.addEventListener(KeyboardEvent.KEY_DOWN, onAction);
//        _chatControl.chatInput.addEventListener(MouseEvent.MOUSE_DOWN, onAction);
//
//        // or clear it out if they haven't already after ten seconds
//        new Timer(10000, 1).addEventListener(TimerEvent.TIMER, onAction);
//    }

    protected function maybeDisplayAvatarIntro () :void
    {
        // if we have already shown the intro, they are a guest, are not wearing the tofu avatar,
        // or have ever worn any non-tofu avatar, don't show the avatar intro
        var mobj :MemberObject = _wctx.getMemberObject();
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
                _wctx.getWorldController().handleViewAvatarCatalog();
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

    /**
     * Handle the zoom button popup.
     */
    protected function handlePopZoom () :void
    {   
        SliderPopup.toggle(_zoomBtn, getZoom(), setZoom);
    }       

    protected function getZoom () :Number
    {
        // in the "viewer", we don't save the zoom in preferences
        var studioView :RoomStudioView = _ctx.getTopPanel().getPlaceView() as RoomStudioView;
        return (studioView != null) ? studioView.getZoom() : Prefs.getZoom();
    }

    protected function setZoom (newZoom :Number) :void
    {
        // in the "viewer", we don't save the zoom in preferences
        var studioView :RoomStudioView = _ctx.getTopPanel().getPlaceView() as RoomStudioView;
        if (studioView != null) {
            studioView.setZoom(newZoom);
        } else {
            Prefs.setZoom(newZoom);
        }
    }

    /** Our context, cast as a WorldContext. */
    protected var _wctx :WorldContext;

    /** Handles room zooming. */
    protected var _zoomBtn :CommandButton;

    /** Button for editing the current scene. */
    protected var _roomeditBtn :CommandButton;

    /** Hovering over this shows clickable components. */
    protected var _hotZoneBtn :CommandButton;

    /** Button for room snapshots. */
    protected var _snapBtn :CommandButton;

    /** A button for popping up the friends list. */
    protected var _friendsBtn :CommandButton;

    protected var _partyBtn :CommandButton;

//    /** A tip shown when we first enter a room. */
//    protected var _chatTip :DisplayObject;

    /** An introduction to avatars shown to brand new players. */
    protected var _avatarIntro :DisplayObjectContainer;

    /** The little gray area that displays incoming notifications. */
    protected var _notificationDisplay :NotificationDisplay;

    /** We stop showing the "type here to chat" tip after the user reaches level 5. */
    protected static const CHAT_TIP_GRADUATE_LEVEL :int = 5;

//    [Embed(source="../../../../../../../rsrc/media/chat_tip.swf")]
//    protected static const CHAT_TIP :Class;

    [Embed(source="../../../../../../../rsrc/media/avatar_intro.swf",
           mimeType="application/octet-stream")]
    protected static const AVATAR_INTRO :Class;
}
}
