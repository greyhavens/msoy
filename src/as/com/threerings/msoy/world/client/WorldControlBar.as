//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UIState;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.ui.FloatingPanel;

/**
 * Configures the control bar with World-specific stuff.
 */
public class WorldControlBar extends ControlBar
{
    /** A button for room-related crap. */
    public var roomBtn :CommandButton;

    /** Hovering over this shows clickable components. */
    public var hotZoneBtn :CommandButton;

    /** A button for popping up the friends list. */
    public var friendsBtn :CommandButton;

    /** Brings up the recent places grid. */
    public var homePageGridBtn :CommandButton;

    /** Handles the two party-related popups. */
    public var partyBtn :CommandButton;

    public var foolsBtn :CommandButton;

    /**
     * Constructor.
     */
    public function WorldControlBar (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Lets us know we are in the A/B group that gets to see the home page grid.
     */
    public function showHomePageGrid () :void
    {
        if (homePageGridBtn == null) {
            homePageGridBtn = createButton("controlBarHomePageGridButton", "i.homePageGrid");
            homePageGridBtn.toggle = true;
            homePageGridBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
                return new HomePageDialog(_wctx);
            }, homePageGridBtn));
            addButton(homePageGridBtn, isNotInViewer, GLOBAL_PRIORITY);
            updateUI();
        }
        homePageGridBtn.activate();
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

        roomBtn = createButton("controlBarButtonRoom", "i.room");
        roomBtn.toggle = true;
        roomBtn.setCommand(WorldController.POP_ROOM_MENU, roomBtn);

        hotZoneBtn = createButton("controlBarHoverZone", "i.hover");
        hotZoneBtn.toggle = true;
        hotZoneBtn.setCallback(updateHot);
        hotZoneBtn.addEventListener(MouseEvent.ROLL_OVER, hotHandler);
        hotZoneBtn.addEventListener(MouseEvent.ROLL_OUT, hotHandler);

        friendsBtn = createButton("controlBarFriendButton", "i.friends");
        friendsBtn.toggle = true;
        friendsBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return new FriendsListPanel(_wctx);
        }, friendsBtn));

        partyBtn = createButton("controlBarPartyButton", "i.party");
        partyBtn.toggle = true;
        partyBtn.setCallback(FloatingPanel.createPopper(function () :FloatingPanel {
            return _wctx.getPartyDirector().createAppropriatePartyPanel();
        }, partyBtn));

        // if (Prefs.IS_APRIL_FOOLS) {
        //     foolsBtn = createButton("", "Toggle Foolishness");
        //     foolsBtn.toggle = true;
        //     foolsBtn.selected = Prefs.isAprilFoolsEnabled();
        //     foolsBtn.setCallback(Prefs.setAprilFoolsEnabled);
        // }
    }

    override protected function checkControls (... ignored) :void
    {
        const isLoggedOn :Boolean = _ctx.getClient().isLoggedOn();
        if (homePageGridBtn != null) {
            homePageGridBtn.enabled = isLoggedOn;
        }
        friendsBtn.enabled = isLoggedOn;
        partyBtn.enabled = isLoggedOn;

        super.checkControls();
    }

    // from ControlBar
    override protected function addControls () :void
    {
        super.addControls();
        var state :UIState = _ctx.getUIState();

        function isInRoom () :Boolean {
            return state.inRoom;
        }

        function showFriends () :Boolean {
            return isNotInViewer() && (state.inRoom || state.inAVRGame || !state.embedded);
        }

        function showParty () :Boolean {
            return isNotInViewer() && (state.inRoom || state.inAVRGame || !state.embedded);
        }

        addButton(friendsBtn, showFriends, GLOBAL_PRIORITY);
        if (!_ctx.getMsoyClient().getEmbedding().isMinimal()) {
            addButton(partyBtn, showParty, GLOBAL_PRIORITY + 1);
            addButton(roomBtn, isInRoom, PLACE_PRIORITY);
            addButton(hotZoneBtn, isInRoom, PLACE_PRIORITY);
        }

        if (foolsBtn != null && isNotInViewer()) {
            addButton(foolsBtn, isInRoom, VOLUME_PRIORITY);
        }
    }

    // from ControlBar
    override protected function isNotInViewer () :Boolean
    {
        return UberClient.isRegularClient();
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
