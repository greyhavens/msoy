//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import flash.geom.Point;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.BubblePopup;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.room.client.RoomStudioView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.client.snapshot.SnapshotPanel;

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

    /**
     * This is needed by the world controller, so that it can make sure the grid is popped up
     * when the user enters their home.
     */
    public function get homePageGridBtn () :CommandButton
    {
        return _homePageGridBtn;
    }

    public function get friendsBtn () :CommandButton
    {
        return _friendsBtn;
    }

    public function get partyBtn () :CommandButton
    {
        return _partyBtn;
    }

    public function get hotZoneBtn () :CommandButton
    {
        return _hotZoneBtn;
    }

    /**
     * Shows a help bubble emphasizing the use of the share button.
     * TODO: make more generic
     */
    public function showShareButtonBubble () :void
    {
        // top center of the button (w/ vertical offset to clear the control bar)
        var gloc :Point = _shareBtn.localToGlobal(new Point(_shareBtn.width / 2, -7));
        BubblePopup.showHelpBubble(_ctx.getTopPanel().getPlaceContainer(),
            Msgs.WORLD.get("h.room_share"), gloc);
    }

    /**
     * This is needed by the room controller, so that it can properly know how to hover.
     */
    public function get hoverAll () :Boolean
    {
        return _hotZoneBtn.selected;
    }

    public function enableZoomControl (enabled :Boolean ) :void
    {
        _zoomBtn.enabled = enabled;
        _zoomBtn.toolTip = Msgs.GENERAL.get(enabled ? "i.zoom" : "i.zoom_disabled");
        updateZoomButton();
    }

    public function setMusicPlaying (playing :Boolean) :void
    {
        // pop down the dialog if no more music...
        if (!playing && _musicBtn.selected) {
            _musicBtn.activate();
        }

        FlexUtil.setVisible(_musicBtn, playing);
        _buttons.recheckButtons();
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

        _musicBtn = createButton("controlBarButtonMusic", "i.music");
        _musicBtn.toggle = true;
        FlexUtil.setVisible(_musicBtn, false);
        _musicBtn.setCallback(FloatingPanel.createPopper(function () :MusicDialog {
            return new MusicDialog(_wctx, _musicBtn.localToGlobal(new Point()));
        }, _musicBtn));

        _roomeditBtn = createButton("controlBarButtonEdit", "i.editScene");
        _roomeditBtn.setCommand(WorldController.ROOM_EDIT);
        _roomeditBtn.enabled = false;

        _zoomBtn = createButton("controlBarButtonZoom", "i.zoom");
        _zoomBtn.setCallback(handleToggleZoom);

        _hotZoneBtn = createButton("controlBarHoverZone", "i.hover");
        _hotZoneBtn.toggle = true;
        _hotZoneBtn.setCallback(updateHot);
        _hotZoneBtn.addEventListener(MouseEvent.ROLL_OVER, hotHandler);
        _hotZoneBtn.addEventListener(MouseEvent.ROLL_OUT, hotHandler);

        _snapBtn = createButton("controlBarButtonSnapshot", "i.snapshot");
        _snapBtn.toggle = true;
        _snapBtn.setCallback(FloatingPanel.createPopper(function () :SnapshotPanel {
            return new SnapshotPanel(_wctx);
        }, _snapBtn));

        _friendsBtn = createButton("controlBarFriendButton", "i.friends");
        _friendsBtn.toggle = true;
        _friendsBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new FriendsListPanel(_wctx);
        }, _friendsBtn));

        _homePageGridBtn = createButton("controlBarHomePageGridButton", "i.homePageGrid");
        _homePageGridBtn.toggle = true;
        _homePageGridBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new HomePageDialog(_wctx);
        }, _homePageGridBtn));

        _partyBtn = createButton("controlBarPartyButton", "i.party");
        _partyBtn.toggle = true;
        _partyBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return _wctx.getPartyDirector().createAppropriatePartyPanel();
        }, _partyBtn));
    }

    // from ControlBar
    override protected function addControls () :void
    {
        super.addControls(); 

        addButton(_musicBtn, [ UI_ROOM, UI_AVRGAME ], VOLUME_PRIORITY);
        addButton(_homePageGridBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY);
        addButton(_friendsBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY);

        addButton(_hotZoneBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(_zoomBtn, [ UI_ROOM, UI_VIEWER, UI_AVRGAME ], PLACE_PRIORITY);

        addButton(_snapBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(_roomeditBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);

        if (DeploymentConfig.devDeployment) {
            addButton(_partyBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY + 1);
        }
    }

    // from ControlBar
    override protected function getMode () :String
    {
        return UberClient.isRegularClient() ? super.getMode() : UI_VIEWER;
    }

    /**
     * Handle the zoom button.
     */
    protected function handleToggleZoom () :void
    {
        setZoom(1 - getZoom()); // toggle between 1 and 0
        updateZoomButton();
    }

    protected function updateZoomButton () :void
    {
        _zoomBtn.selected = (getZoom() == 0);
    }

    protected function getZoom () :Number
    {
        // in the "viewer", we don't save the zoom in preferences
        // and we use Math.round() to adapt older-style settings which could be .5634
        var studioView :RoomStudioView = _ctx.getPlaceView() as RoomStudioView;
        return (studioView != null) ? studioView.getZoom() : Math.round(Prefs.getZoom());
    }

    protected function setZoom (newZoom :Number) :void
    {
        // in the "viewer", we don't save the zoom in preferences
        var studioView :RoomStudioView = _ctx.getPlaceView() as RoomStudioView;
        if (studioView != null) {
            studioView.setZoom(newZoom);
        } else {
            Prefs.setZoom(newZoom);
        }
    }

    protected function updateHot (on :Boolean) :void
    {
        if (on != _hotOn) {
            _hotOn = on;
            var roomView :RoomView = _ctx.getPlaceView() as RoomView;
            if (roomView != null) {
                roomView.hoverAllFurni(on);
            }
        }
    }

    protected function hotHandler (event :MouseEvent) :void
    {
        if (!_hotZoneBtn.selected) {
            updateHot(event.type == MouseEvent.ROLL_OVER);
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

    protected var _hotOn :Boolean;

    /** Button to display info about the currently playing music. */
    protected var _musicBtn :CommandButton;

    /** Button for room snapshots. */
    protected var _snapBtn :CommandButton;

    /** A button for popping up the friends list. */
    protected var _friendsBtn :CommandButton;

    /** Brings up the recent places grid. */
    protected var _homePageGridBtn :CommandButton;

   /** Handles the two party-related popups. */
    protected var _partyBtn :CommandButton;
}
}
