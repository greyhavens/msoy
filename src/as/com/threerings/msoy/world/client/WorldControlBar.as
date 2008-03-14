//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import com.threerings.flex.CommandButton;
import com.threerings.util.Log;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

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

        // if we just moved into a room, we may want to display our "click here to chat" tip
        if (place is RoomObject && _chatTip == null && _chatControl != null &&
            (_ctx as WorldContext).getMemberObject().level < CHAT_TIP_GRADUATE_LEVEL) {
            _chatTip = (new CHAT_TIP() as DisplayObject);
            _chatTip.x = x + 5;
            _chatTip.y = y - _chatTip.height - 5;
            stage.addChild(_chatTip);

            // when they click or type in the chat entry, we want to clear out the tip
            var onAction :Function = function (event :*) :void {
                _chatControl.chatInput.removeEventListener(KeyboardEvent.KEY_DOWN, onAction);
                _chatControl.chatInput.removeEventListener(MouseEvent.MOUSE_DOWN, onAction);
                // can't use fancy Dissolve effect here so we use the poor man's dissolve
                _chatTip.addEventListener(Event.ENTER_FRAME, function (event :Event) :void {
                    if (_chatTip.alpha == 0) {
                        if (_chatTip.parent != null) {
                            _chatTip.parent.removeChild(_chatTip);
                        }
                        _chatTip.removeEventListener(Event.ENTER_FRAME, arguments.callee);
                    } else {
                        _chatTip.alpha -= 0.05;
                    }
                });
            };
            _chatControl.chatInput.addEventListener(KeyboardEvent.KEY_DOWN, onAction);
            _chatControl.chatInput.addEventListener(MouseEvent.MOUSE_DOWN, onAction);
        }
    }

    // from ControlBar
    override public function setLocation (name :String, inRoom :Boolean, canGoBack :Boolean) :void
    {
        super.setLocation(name, inRoom, canGoBack);
        // _hotZoneBtn.enabled = inRoom; // TODO: allow this disabled somehow
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
            var roomView :RoomView = _ctx.getTopPanel().getPlaceView() as RoomView;
            if (roomView != null) {
                roomView.getRoomController().hoverAllFurni(event.type == MouseEvent.ROLL_OVER);
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

    /** We stop showing the "type here to chat" tip after the user reaches level 5. */
    protected static const CHAT_TIP_GRADUATE_LEVEL :int = 5;

    [Embed(source="../../../../../../../rsrc/media/chat_tip.swf")]
    protected static const CHAT_TIP :Class;
}
}
