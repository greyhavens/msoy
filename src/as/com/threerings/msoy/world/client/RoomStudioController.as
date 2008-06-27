//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.system.Security;

import flash.utils.ByteArray;

import com.threerings.io.TypedArray;

import com.threerings.util.MethodQueue;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.UberClientModes;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomConfig;

public class RoomStudioController extends RoomController
{
    /**
     * Called by the view once it's on stage.
     */
    public function studioOnStage (uberMode :int) :void
    {
        if (uberMode == UberClientModes.AVATAR_VIEWER || uberMode == UberClientModes.DECOR_VIEWER) {
            _walkTarget.visible = false;
            _flyTarget.visible = false;
            _roomView.addChildAt(_flyTarget, _roomView.numChildren);
            _roomView.addChildAt(_walkTarget, _roomView.numChildren);
        
            _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
            _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
            _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
        }

        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse, false, int.MIN_VALUE);
        setControlledPanel(_studioView);

        initScene();
    }

    // allow other actor moves (pet)
    override public function requestMove (ident :ItemIdent, newLoc :MsoyLocation) :Boolean
    {
        // move it one frame later
        MethodQueue.callLater(_studioView.getPet().moveTo, [ newLoc, _scene ]);
        //_studioView.getPet().moveTo(newLoc, _scene);
        return true;
    }

    // handle control requests
    override public function requestControl (ident :ItemIdent) :void
    {
        MethodQueue.callLater(_studioView.dispatchEntityGotControl, [ ident ]);
    }

    // documentation inherited
    override public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var menuItems :Array = [];
        addSelfMenuItems(avatar, menuItems, true);
        if (menuItems.length == 0) {
            menuItems.push({ label: Msgs.GENERAL.get("l.noAvActions"), enabled: false });
        }
        popActorMenu(avatar, menuItems);
    }

    // documentation inherited
    override public function doAvatarAction (action :String) :void
    {
        _studioView.doAvatarAction(action);
    }

    // documentation inherited
    override public function doAvatarState (state :String) :void
    {
        _studioView.setAvatarState(state);
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _studioView = new RoomStudioView(ctx as StudioContext, this);
        _roomView = _studioView; // also copy it to that
        return _studioView;
    }

    /**
     * Initialize the scene.
     */
    protected function initScene () :void
    {
        FurniSprite.setLoadingWatcher(
            new PlaceLoadingDisplay(_wdctx.getTopPanel().getPlaceContainer()));

        // and.. initialize it!
        var model :MsoySceneModel = new MsoySceneModel();
        model.ownerType = MsoySceneModel.OWNER_TYPE_MEMBER;
        model.furnis = TypedArray.create(FurniData);

        var params :Object = MsoyParameters.get();
        var decor :Decor;
        if (UberClient.getMode() == UberClientModes.DECOR_VIEWER) {
            decor = new Decor();
            decor.type = int(params.decorType);
            decor.width = int(params.decorWidth);
            decor.height = int(params.decorHeight);
            decor.depth = int(params.decorDepth);
            decor.horizon = Number(params.decorHorizon);
            decor.hideWalls = ("true" == String(params.decorHideWalls));
            decor.offsetX = Number(params.decorOffsetX);
            decor.offsetY = Number(params.decorOffsetY);
            decor.furniMedia = new StudioMediaDesc(params.media as String);

        } else {
            decor = MsoySceneModel.defaultMsoySceneModelDecor();
            decor.furniMedia = null; // the view does some stuff to render a line drawing instead
        }

        model.decor = decor;
        _scene = new MsoyScene(model, _config);
        _studioView.setScene(_scene);
        _studioView.setBackground(model.decor);
    }

    override protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        _studioView.doAvatarMove(newLoc);
    }

    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        // TODO NOW
    }

    override protected function sendSpriteSignal2 (name :String, data :ByteArray) :void
    {
        // TODO NOW
    }

    protected var _studioView :RoomStudioView;
}
}
