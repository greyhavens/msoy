//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.utils.ByteArray;
import flash.utils.Dictionary;

import com.threerings.io.TypedArray;

import com.threerings.util.HashMap;
import com.threerings.util.MethodQueue;
import com.threerings.util.ObjectMarshaller;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.PlaceLoadingDisplay;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.UberClientModes;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

public class RoomStudioController extends RoomController
{
    /**
     * Called by the view once it's on stage.
     */
    public function studioOnStage (uberMode :int) :void
    {
        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.addChildAt(_flyTarget, _roomView.numChildren);
        _roomView.addChildAt(_walkTarget, _roomView.numChildren);
    
        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse, false, int.MIN_VALUE);
        setControlledPanel(_studioView);

        initScene();
    }

    // allow other actor moves
    override public function requestMove (ident :ItemIdent, newLoc :MsoyLocation) :Boolean
    {
        // move it one frame later
        MethodQueue.callLater(_studioView.doEntityMove, [ ident, newLoc ]);
        return true;
    }

    // documentation inherited
    override public function getMemories (ident :ItemIdent) :Object
    {
        var mems :Object = {};
        var dict :Dictionary = _memories.get(ident) as Dictionary;
        for (var key :String in dict) {
            mems[key] = ObjectMarshaller.decode(dict[key]);
        }
        return mems;
    }

    // documentation inherited
    override public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        var dict :Dictionary = _memories.get(ident) as Dictionary;
        return (dict == null) ? null : ObjectMarshaller.decode(dict[key]);
    }

    // documentation inherited
    override public function canManageRoom () :Boolean
    {
        // Pretend we have rights to this room
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
        model.entrance = new MsoyLocation(.5, 0, 0);

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
        _studioView.setScene(new MsoyScene(model, _config));
        _studioView.setBackground(model.decor);
    }

    // documentation inherited
    override protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        _studioView.doAvatarMove(newLoc);
    }

    // documentation inherited
    override protected function setActorState2 (
        ident :ItemIdent, actorOid :int, state :String) :void
    {
        _studioView.setActorState(ident, state);
    }

    // documentation inherited
    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        MethodQueue.callLater(_studioView.dispatchSpriteMessage, [ ident, name, data, isAction ]);
    }

    // documentation inherited
    override protected function sendSpriteSignal2 (name :String, data :ByteArray) :void
    {
        MethodQueue.callLater(_studioView.dispatchSpriteSignal, [ name, data ]);
    }

    // documentation inherited
    override protected function sendPetChatMessage2 (msg :String, info :ActorInfo) :void
    {
        // TODO?
    }

    // documentation inherited
    override protected function updateMemory2 (
        ident :ItemIdent, key :String, data :ByteArray, callback :Function) :void
    {
        var dict :Dictionary = _memories.get(ident) as Dictionary;
        if (dict == null) {
            dict = new Dictionary();
            _memories.put(ident, dict);
        }
        if (data == null) {
            delete dict[key];
        } else {
            dict[key] = data;
        }

        MethodQueue.callLater(_studioView.dispatchMemoryChanged, [ ident, key, data ]);
        MethodQueue.callLater(callback, [ true ]);
    }

    protected var _studioView :RoomStudioView;

    /** Maps ItemIdent -> (Dictionary[key] -> encoded value) */
    protected var _memories :HashMap = new HashMap();
}
}
