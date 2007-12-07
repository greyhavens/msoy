//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

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
        _notifyBtn.enabled = avail;
        _notifyBtn.toolTip = Msgs.GENERAL.get("i.notifications" + (avail ? "_avail" : ""));
    }

    /**
     * Sets whether the notification display is showing or not. Called by the NotificationDirector.
     */
    public function setNotificationsShowing (showing :Boolean) :void
    {
        _notifyBtn.selected = showing;
    }

    // from ControlBar
    override public function miniChanged () :void
    {
        _isEditing = (_isEditing && !_ctx.getTopPanel().isMinimized());
        super.miniChanged();
    }

    // from ControlBar
    override protected function createControls () :void
    {
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

        _notifyBtn = new CommandButton(MsoyController.POPUP_NOTIFICATIONS);
        _notifyBtn.toolTip = Msgs.GENERAL.get("i.notifications");
        _notifyBtn.styleName = "controlBarButtonNotify";
        _notifyBtn.enabled = false;
        _notifyBtn.toggle = true;

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
        addGroupChild(_notifyBtn, [ UI_STD, UI_GUEST ]);
        // TODO: snapshots are not functional; revisit
        if (_ctx.getTokens().isAdmin()) {
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

    /** Displays notification status. */
    protected var _notifyBtn :CommandButton;

    /** Hovering over this shows clickable components. */
    protected var _hotZoneBtn :CommandButton;

    /** Button for room snapshots. */
    protected var _snapBtn :CommandButton;
}
}
