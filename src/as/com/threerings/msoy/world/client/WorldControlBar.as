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
    /** Handles room zooming. */
    public var zoomBtn :CommandButton;

    /** Button for editing the current scene. */
    public var roomEditBtn :CommandButton;

    /** Hovering over this shows clickable components. */
    public var hotZoneBtn :CommandButton;

    /** Button to display info about the currently playing music. */
    public var musicBtn :CommandButton;

    /** Button for room snapshots. */
    public var snapBtn :CommandButton;

    /** A button for popping up the friends list. */
    public var friendsBtn :CommandButton;

    /** Brings up the recent places grid. */
    public var homePageGridBtn :CommandButton;

   /** Handles the two party-related popups. */
    public var partyBtn :CommandButton;

    /**
     * Constructor.
     */
    public function WorldControlBar (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Shows a help bubble emphasizing the use of the share button.
     * TODO: make more generic
     */
    public function showShareButtonBubble () :void
    {
        // top center of the button (w/ vertical offset to clear the control bar)
        var gloc :Point = shareBtn.localToGlobal(new Point(shareBtn.width / 2, -7));
        BubblePopup.showHelpBubble(_ctx.getTopPanel().getPlaceContainer(),
            Msgs.WORLD.get("h.room_share"), gloc);
    }

    public function enableZoomControl (enabled :Boolean ) :void
    {
        zoomBtn.enabled = enabled;
        zoomBtn.toolTip = Msgs.GENERAL.get(enabled ? "i.zoom" : "i.zoom_disabled");
        updateZoomButton();
    }

    public function setMusicPlaying (playing :Boolean) :void
    {
        // pop down the dialog if no more music...
        if (!playing && musicBtn.selected) {
            musicBtn.activate();
        }

        FlexUtil.setVisible(musicBtn, playing);
        _buttons.recheckButtons();
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

        musicBtn = createButton("controlBarButtonMusic", "i.music");
        musicBtn.toggle = true;
        FlexUtil.setVisible(musicBtn, false);
        musicBtn.setCallback(FloatingPanel.createPopper(function () :MusicDialog {
            return new MusicDialog(_wctx, musicBtn.localToGlobal(new Point()));
        }, musicBtn));

        roomEditBtn = createButton("controlBarButtonEdit", "i.editScene");
        roomEditBtn.setCommand(WorldController.ROOM_EDIT);
        roomEditBtn.enabled = false;

        zoomBtn = createButton("controlBarButtonZoom", "i.zoom");
        zoomBtn.setCallback(handleToggleZoom);

        hotZoneBtn = createButton("controlBarHoverZone", "i.hover");
        hotZoneBtn.toggle = true;
        hotZoneBtn.setCallback(updateHot);
        hotZoneBtn.addEventListener(MouseEvent.ROLL_OVER, hotHandler);
        hotZoneBtn.addEventListener(MouseEvent.ROLL_OUT, hotHandler);

        snapBtn = createButton("controlBarButtonSnapshot", "i.snapshot");
        snapBtn.toggle = true;
        snapBtn.setCallback(FloatingPanel.createPopper(function () :SnapshotPanel {
            return new SnapshotPanel(_wctx);
        }, snapBtn));

        friendsBtn = createButton("controlBarFriendButton", "i.friends");
        friendsBtn.toggle = true;
        friendsBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new FriendsListPanel(_wctx);
        }, friendsBtn));

        homePageGridBtn = createButton("controlBarHomePageGridButton", "i.homePageGrid");
        homePageGridBtn.toggle = true;
        homePageGridBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new HomePageDialog(_wctx);
        }, homePageGridBtn));

        partyBtn = createButton("controlBarPartyButton", "i.party");
        partyBtn.toggle = true;
        partyBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return _wctx.getPartyDirector().createAppropriatePartyPanel();
        }, partyBtn));
    }

    // from ControlBar
    override protected function addControls () :void
    {
        super.addControls(); 

        addButton(musicBtn, [ UI_ROOM, UI_AVRGAME ], VOLUME_PRIORITY);
        addButton(homePageGridBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY);
        addButton(friendsBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY);

        addButton(hotZoneBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(zoomBtn, [ UI_ROOM, UI_VIEWER, UI_AVRGAME ], PLACE_PRIORITY);

        addButton(snapBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(roomEditBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);

        if (DeploymentConfig.devDeployment) {
            addButton(partyBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY + 1);
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
        zoomBtn.selected = (getZoom() == 0);
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
        if (!hotZoneBtn.selected) {
            updateHot(event.type == MouseEvent.ROLL_OVER);
        }
    }

    /** Our context, cast as a WorldContext. */
    protected var _wctx :WorldContext;

    protected var _hotOn :Boolean;
}
}
