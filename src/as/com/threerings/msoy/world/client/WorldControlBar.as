//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;

import flash.events.MouseEvent;

import flash.text.TextField;

import caurina.transitions.Tweener;

import com.threerings.flex.CommandButton;

import com.threerings.util.MultiLoader;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.room.client.RoomStudioView;
import com.threerings.msoy.room.client.RoomView;
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

    // from ControlBar
    override public function locationDidChange (place :PlaceObject) :void
    {
        super.locationDidChange(place);

        _hotZoneBtn.selected = false;
        updateHot(false);

        // if we just moved into a room...
        if (place is RoomObject && !_wctx.getGameDirector().isGaming()) {
            // maybe show the avatar introduction
            maybeDisplayAvatarIntro();
        }
    }

    // from ControlBar
    override protected function createControls () :void
    {
        super.createControls();

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
    }

    // from ControlBar
    override protected function addControls () :void
    {
        super.addControls(); 

        if (DeploymentConfig.devDeployment) {
            // TODO: can this work in UI_GAME too?
            addButton(_homePageGridBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        }

        addButton(_hotZoneBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(_zoomBtn, [ UI_ROOM, UI_VIEWER, UI_AVRGAME ], PLACE_PRIORITY);

        addButton(_snapBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);
        addButton(_roomeditBtn, [ UI_ROOM, UI_AVRGAME ], PLACE_PRIORITY);

        // TODO: enable friends for guests, even if it just goads them into signup
        // TODO: friends are global, yes? They should be in ControlBar
        if (_friendsBtn != null && _isMember) {
            addButton(_friendsBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], GLOBAL_PRIORITY);
        }
    }

    // from ControlBar
    override protected function getMode () :String
    {
        return UberClient.isRegularClient() ? super.getMode() : UI_VIEWER;
    }

    // TODO: move this crap somewhere else
    protected function maybeDisplayAvatarIntro () :void
    {
        // if we have already shown the intro, they are a guest, are not wearing the tofu avatar,
        // or have ever worn any non-tofu avatar, don't show the avatar intro
        var mobj :MemberObject = _wctx.getMemberObject();
        if (_avatarIntro != null || mobj.isGuest() || mobj.avatar != null ||
                mobj.avatarCache.size() > 0) {
            return;
        }

        MultiLoader.getContents(DeploymentConfig.serverURL + "rsrc/avatar_intro.swf",
            function (result :DisplayObjectContainer) :void {
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
                _wctx.getWorldController().handleViewShop(Item.AVATAR);
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
        Tweener.addTween(thing, { alpha: 1, time: .75, transition: "linear" });
    }

    protected function fadeOutAndRemove (thing :DisplayObject) :void
    {
        Tweener.addTween(thing, { alpha: 0, time: .75, transition: "linear",
            onComplete: _ctx.getTopPanel().getPlaceContainer().removeOverlay,
            onCompleteParams: [ thing ] });
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
        var studioView :RoomStudioView = _ctx.getTopPanel().getPlaceView() as RoomStudioView;
        return (studioView != null) ? studioView.getZoom() : Math.round(Prefs.getZoom());
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

    protected function updateHot (on :Boolean) :void
    {
        if (on != _hotOn) {
            _hotOn = on;
            var roomView :RoomView = _ctx.getTopPanel().getPlaceView() as RoomView;
            if (roomView != null) {
                roomView.hoverAllFurni(on);
            }
        }
    }

    protected function hotHandler (event :MouseEvent) :void {
        if (!_hotZoneBtn.selected) updateHot(event.type == MouseEvent.ROLL_OVER);
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

    /** Button for room snapshots. */
    protected var _snapBtn :CommandButton;

    /** A button for popping up the friends list. */
    protected var _friendsBtn :CommandButton;

    /** Brings up the recent places grid. */
    protected var _homePageGridBtn :CommandButton;

    /** An introduction to avatars shown to brand new players. */
    protected var _avatarIntro :DisplayObjectContainer;
}
}
