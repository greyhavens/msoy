//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

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

import com.threerings.msoy.utils.Base64Decoder;
import com.threerings.msoy.utils.Base64Encoder;

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

        _throttleChecker.start();

        try {
            if (ExternalInterface.available) {
                ExternalInterface.addCallback("getStudioMemories", getStudioMemories);
            }
        } catch (e :Error) {}

        initScene();
    }

    // allow other actor moves
    override public function requestMove (ident :ItemIdent, newLoc :MsoyLocation) :Boolean
    {
        // move it one frame later
        throttle(ident, MethodQueue.callLater, _studioView.doEntityMove, [ ident, newLoc ]);
        return true;
    }

    // documentation inherited
    override public function getMemories (ident :ItemIdent) :Object
    {
        var mems :Object = {};
        getMemoryMap(ident).forEach(function (key :String, value :ByteArray) :void {
            mems[key] = ObjectMarshaller.decode(value);
        });
        return mems;
    }

    // documentation inherited
    override public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        return ObjectMarshaller.decode(getMemoryMap(ident).get(key));
    }

    // documentation inherited
    override public function canManageRoom (
        memberId :int = 0, allowSupport :Boolean = true) :Boolean
    {
        // Pretend we have rights to this room
        return true;
    }

    // handle control requests
    override public function requestControl (ident :ItemIdent) :void
    {
        dispatchEntityGotControl(ident);
    }

    // documentation inherited
    override public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var menuItems :Array = [];
        addSelfMenuItems(avatar, menuItems, true);
        if (menuItems.length == 0) {
            menuItems.push({ label: Msgs.STUDIO.get("l.no_actions"), enabled: false });
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
            decor.hideWalls = ("true" == String(params.decorHideWalls));
            decor.width = int(params.decorWidth);
            decor.height = int(params.decorHeight);
            decor.depth = int(params.decorDepth);
            decor.horizon = Number(params.decorHorizon);
            decor.actorScale = Number(params.decorActorScale);
            decor.furniScale = Number(params.decorFurniScale);
            decor.setFurniMedia(new StudioMediaDesc(params.media as String));

        } else {
            decor = MsoySceneModel.defaultMsoySceneModelDecor();
            decor.setFurniMedia(null); // the view does some stuff to render a line drawing instead
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
        throttle(ident, _studioView.setActorState, ident, state);
    }

    // documentation inherited
    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        throttle(ident, MethodQueue.callLater,
            _studioView.dispatchSpriteMessage, [ ident, name, data, isAction ]);
    }

    // documentation inherited
    override protected function sendSpriteSignal2 (
        ident :ItemIdent, name :String, data :ByteArray) :void
    {
        throttle(ident, MethodQueue.callLater, _studioView.dispatchSpriteSignal, [ name, data ]);
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
        throttle(ident, updateMemory3, ident, key, data, callback);
    }

    /**
     * 3rd step to update memory: after throttling.
     */
    protected function updateMemory3 (
        ident :ItemIdent, key :String, data :ByteArray, callback :Function) :void
    {
        var map :HashMap = getMemoryMap(ident);
        if (data == null) {
            map.remove(key);
        } else {
            map.put(key, data);
        }

        MethodQueue.callLater(_studioView.dispatchMemoryChanged, [ ident, key, data ]);
        if (callback != null) {
            MethodQueue.callLater(callback, [ true ]);
        }
    }

    /**
     * Get the memories of the _testingSprite, for use when purchasing a configurable item.
     */
    protected function getStudioMemories () :String
    {
        var spr :MsoySprite = _studioView.getTestingSprite();
        if (spr == null) {
            return null;
        }
        var map :HashMap = getMemoryMap(spr.getItemIdent());
        if (map == null || map.isEmpty()) {
            return null;
        }

        // Perhaps this is risky, but I can't see how it's any riskier than anything else-
        // Encode the memories directly into the database representation. Easy.
        var bytes :ByteArray = new ByteArray();
        bytes.writeShort(map.size());
        map.forEach(function (key :String, value :ByteArray) :void {
            bytes.writeUTF(key);
            bytes.writeShort(value.length);
            bytes.writeBytes(value);
        });
        var encoder :Base64Encoder = new Base64Encoder();
        encoder.encodeBytes(bytes);
        return encoder.flush();
    }

    /**
     * Takes care of initializing any memory sent down from the server.
     */
    protected function getMemoryMap (ident :ItemIdent) :HashMap
    {
        var map :HashMap;
        if (_memories == null) { // see if we need to initialize
            _memories = new HashMap();
            // first things first- we need to set up memories from the server
            try {
                map = getMemoryMap(_studioView.getTestingSprite().getItemIdent());
                var encoded :String = MsoyParameters.get()["mems"];
                if (encoded != null) {
                    var decoder :Base64Decoder = new Base64Decoder();
                    decoder.decode(encoded);
                    var bytes :ByteArray = decoder.flush();
                    var count :int = bytes.readShort();
                    for (; count >= 0; count--) {
                        var key :String = bytes.readUTF();
                        var length :int = bytes.readShort();
                        var value :ByteArray = new ByteArray();
                        bytes.readBytes(value, 0, length);
                        map.put(key, value);
                    }
                }
            } catch (e :Error) {
                log.warning("Unable to decode memories", e);
            }
        }

        // after that, just return a map, always
        map = _memories.get(ident) as HashMap;
        if (map == null) {
            map = new HashMap();
            _memories.put(ident, map);
        }
        return map;
    }

    protected var _studioView :RoomStudioView;

    /** Maps ItemIdent -> HashMap<String, ByteArray> */
    protected var _memories :HashMap;
}
}
