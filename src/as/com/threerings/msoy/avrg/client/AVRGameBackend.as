//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.display.DisplayObject;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MethodQueue;
import com.threerings.util.StringUtil;

import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.MobSprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomMetrics;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesObject;

import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGameBackend extends ControlBackend
{
    public static const log :Log = Log.getLog(AVRGameBackend);

    /** 
     * Magic number for specifying the server agent.
     */
    public static const SERVER_AGENT_ID :int = int.MIN_VALUE;

    public function AVRGameBackend (
        wctx :WorldContext, gctx :GameContext, ctrl :AVRGameController, gameObj :AVRGameObject)
    {
        _wctx = wctx;
        _gctx = gctx;
        _ctrl = ctrl;
        _gameObj = gameObj;

        _playerObj = _gctx.getPlayerObject();
    }

    // from ControlBackend
    override public function shutdown () :void
    {
        if (_gameNetAdapter != null) {
            _gameNetAdapter.release();
        }

        if (_playerNetAdapter != null) {
            _playerNetAdapter.release();
        }

        if (_roomPropsNetAdapter != null) {
            _roomPropsNetAdapter.release();
        }

        super.shutdown();
    }

    public function isConnected () :Boolean
    {
        return _props != null;
    }

    public function panelResized () :void
    {
        MethodQueue.callLater(callUserCode, [ "panelResized_v1" ]);
    }

    public function requestMobSprite (id :String) :DisplayObject
    {
        return callUserCode("requestMobSprite_v1", id) as DisplayObject;
    }

    public function mobRemoved (id :String) :void
    {
        callUserCode("mobRemoved_v1", id);
    }

    public function mobAppearanceChanged (
        id :String, locArray :Array, orient :Number, moving :Boolean, idle :Boolean) :void
    {
        callUserCode("mobAppearanceChanged_v1", id, locArray, orient, moving, idle);
    }

    public function hitTestPoint (x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        return callUserCode("hitTestPoint_v1", x, y, shapeFlag) as Boolean;
    }

    public function isPlaying () :Boolean
    {
        return _wctx.getGameDirector().getGameId() == _ctrl.getGameId();
    }

    public function playerLeftRoom (roomId :int) :void
    {
        callUserCode("leftRoom_v1", roomId);

        if (_roomPropsNetAdapter != null) {
            _roomPropsNetAdapter.release();
            _roomPropsNetAdapter = null;
        }

        if (_avatarAdapter != null) {
            _avatarAdapter.release();
            _avatarAdapter = null;
        }
    }

    public function playerEnteredRoom (roomId :int, roomProps :RoomPropertiesObject) :void
    {
        if (_props == null) {
            return;
        }

        _roomPropsNetAdapter = new BackendNetAdapter(
            roomProps, RoomPropertiesObject.USER_MESSAGE, 
            _props, "room_propertyWasSet_v1", "room_messageReceived_v1");

        _avatarAdapter = new BackendAvatarAdapter(
            _gameObj, _ctrl.getRoom(), _props, "playerMoved_v1", "actorStateSet_v1", 
            "actorAppearanceChanged_v1");

        callUserCode("enteredRoom_v1", roomId);
    }

    public function roomOccupantAdded (memberId :int) :void
    {
        callUserCode("playerEntered_v1", memberId);
    }

    public function roomOccupantRemoved (memberId :int) :void
    {
        callUserCode("playerLeft_v1", memberId);
    }

    public function coinsAwarded (amount :int) :void
    {
        callUserCode("coinsAwarded_v1", amount);
    }

    public function playerMoved (memberId :int) :void
    {
        callUserCode("playerMoved_v1", memberId);
    }

    public function gotControl () :void
    {
    }

    override protected function handleUserCodeConnect (evt :Object) :void
    {
        super.handleUserCodeConnect(evt);

        MethodQueue.callLater(_ctrl.backendConnected);
    }

    override public function callUserCode (name :String, ... args) :*
    {
        if (_props == null) {
            log.warning("Calling user code " + name + " before connection.");

        } else if (_props[name] == null) {
            log.warning("User code " + name + " not found.");
        }

        args.unshift(name);
        return super.callUserCode.apply(null, args);
    }

    override protected function setUserProperties (o :Object) :void
    {
        super.setUserProperties(o);

        _gameNetAdapter = new BackendNetAdapter(
            _gameObj, AVRGameObject.USER_MESSAGE, o, "game_propertyWasSet_v1", 
            "game_messageReceived_v1");

        _playerNetAdapter = new BackendNetAdapter(
            _playerObj, WhirledPlayerObject.getMessageName(_gameObj.getOid()), o,
            "player_propertyWasSet_v1", "player_messageReceived_v1");
    }

    // from ControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        // GameSubControl
        o["game_getGameData_v1"] = game_getGameData_v1;
        o["game_getPlayerIds_v1"] = game_getPlayerIds_v1;

        // RoomSubControl
        o["room_getGameData_v1"] = room_getGameData_v1;
        o["room_getPlayerIds_v1"] = room_getPlayerIds_v1;
        o["room_getRoomId_v1"] = room_getRoomId_v1;
        o["isPlayerHere_v1"] = isPlayerHere_v1;
        o["getAvatarInfo_v1"] = getAvatarInfo_v1;

        // PlayerSubControl
        o["player_getGameData_v1"] = player_getGameData_v1;
        o["player_setProperty_v1"] = player_setProperty_v1;
        o["player_getRoomId_v1"] = player_getRoomId_v1;
        o["getPlayerId_v1"] = getPlayerId_v1;
        o["deactivateGame_v1"] = deactivateGame_v1;
        o["completeTask_v1"] = completeTask_v1;
        o["playAvatarAction_v1"] = playAvatarAction_v1;
        o["setAvatarState_v1"] = setAvatarState_v1;
        o["setAvatarMoveSpeed_v1"] = setAvatarMoveSpeed_v1;
        o["setAvatarLocation_v1"] = setAvatarLocation_v1;
        o["setAvatarOrientation_v1"] = setAvatarOrientation_v1;

        // LocalSubControl
        o["localChat_v1"] = localChat_v1;
        o["getStageSize_v1"] = getStageSize_v1;
        o["getRoomBounds_v1"] = getRoomBounds_v1;
        o["stageToRoom_v1"] = stageToRoom_v1;
        o["roomToStage_v1"] = roomToStage_v1;
        o["locationToRoom_v1"] = locationToRoom_v1;

        // AgentSubControl
        o["agent_sendMessage_v1"] = agent_sendMessage_v1;

        // TODO: MobControl helpers
        o["setMobDecoration_v1"] = setMobDecoration_v1;
        o["setMobHotSpot_v1"] = setMobHotSpot_v1;
    }

    // GameSubControl
    protected function game_getGameData_v1 (targetId :int) :Object
    {
        return _gameObj.getUserProps();
    }

    // GameSubControl
    protected function game_getPlayerIds_v1 () :Array
    {
        if (!isPlaying()) {
            return null;
        }
        
        return BackendUtils.getPlayerIds(_gameObj, 0);
    }

    // RoomSubControl
    protected function room_getGameData_v1 (targetId :int) :Object
    {
        validateRoomTargetId(targetId);
        var roomProps :RoomPropertiesObject = _ctrl.getRoomProperties();
        if (roomProps == null) {
            log.warning("Room properties not found [roomObj=" + _ctrl.getRoom() + "]");
            return { };
        }
        return roomProps.getUserProps();
    }

    // RoomSubControl
    protected function room_getRoomId_v1 (targetId :int /* ignored */) :int
    {
        validateRoomTargetId(targetId);
        // TODO: this should guarantee to only return a non-zero value after the room entry event 
        // has been sent
        return getRoomId();
    }

    // RoomSubControl
    protected function room_getPlayerIds_v1 (targetId :int /* ignored */) :Array
    {
        validateRoomTargetId(targetId);
        if (!isPlaying() || _ctrl.getRoom() == null) {
            return null;
        }
        
        return BackendUtils.getPlayerIds(_gameObj, getRoomId());
    }

    // RoomSubControl
    protected function isPlayerHere_v1 (targetId :int /* ignored */, playerId :int) :Boolean
    {
        validateRoomTargetId(targetId);
        if (isPlaying() && _ctrl.getRoom() != null) {
            var fakeName :MemberName = new MemberName("", playerId);
            return (_gameObj.getOccupantInfo(fakeName) != null &&
                    _ctrl.getRoom().getOccupantInfo(fakeName) != null);
        }
        return false;
    }

    // RoomSubControl
    protected function getAvatarInfo_v1 (targetId :int /* ignored */, playerId :int) :Object
    {
        validateRoomTargetId(targetId);
        var sprite :MemberSprite = getAvatarSprite(playerId);
        if (sprite != null) {
            return [
                sprite.getActorInfo().username.toString(),
                sprite.getState(),
                sprite.getLocation().x,
                sprite.getLocation().y,
                sprite.getLocation().z,
                sprite.getLocation().orient,
                sprite.getMoveSpeed(),
                sprite.isMoving(),
                sprite.isIdle(),
                sprite.getBounds(sprite.stage)
            ];
        }
        return null;
    }

    // PlayerSubControl
    protected function player_getGameData_v1 (targetId :int) :Object
    {
        validatePlayerTargetId(targetId);
        return _playerObj.getUserProps();
    }

    // PlayerSubControl
    protected function player_setProperty_v1 (
        targetId :int, propName :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
        validatePlayerTargetId(targetId);
        if (!isPlaying()) {
            return;
        }

        BackendUtils.encodeAndSet(
            _gctx.getClient(), _playerObj, propName, value, key, isArray, immediate);
    }

    // PlayerSubControl
    protected function getPlayerId_v1 (targetId :int /* ignored */) :int
    {
        validatePlayerTargetId(targetId);
        return _wctx.getMemberObject().getMemberId();
    }

    // PlayerSubControl
    protected function deactivateGame_v1 (targetId :int /* ignored */) :void
    {
        validatePlayerTargetId(targetId);
        if (!isPlaying()) {
            return;
        }
        _wctx.getGameDirector().leaveAVRGame();
    }

    // PlayerSubControl
    protected function player_getRoomId_v1 (targetId :int /* ignored */) :int
    {
        validatePlayerTargetId(targetId);
        // TODO: this should guarantee to only return a non-zero value after the room entry event 
        // has been sent
        return getRoomId();
    }

    // PlayerSubControl
    protected function completeTask_v1 (
        targetId :int /* ignored */, taskId :String, payout :Number) :void
    {
        validatePlayerTargetId(targetId);
        if (StringUtil.isBlank(taskId) || !isPlaying()) {
            return;
        }

        _gameObj.avrgService.completeTask(
            _gctx.getClient(), 0, taskId, Math.max(0, Math.min(1, payout)),
            BackendUtils.loggingConfirmListener("completeTask"));
    }

    // PlayerSubControl
    protected function playAvatarAction_v1 (targetId :int /* ignored */, action :String) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.sendMessage(action, null, true);
        } else {
            BackendUtils.playAvatarAction(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), getMemberId(), action);
        }
    }

    // PlayerSubControl
    protected function setAvatarState_v1 (targetId :int /* ignored */, state :String) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setState(state);
        } else {
            BackendUtils.setAvatarState(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), getMemberId(), state);
        }
    }

    // PlayerSubControl
    protected function setAvatarMoveSpeed_v1 (
        targetId :int /* ignored */, pixelsPerSecond :Number) :void
    {
        validatePlayerTargetId(targetId);

        // todo make the below dispatch to all clients
        return;

        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setMoveSpeedFromUser(pixelsPerSecond);
        } else {
            BackendUtils.setAvatarMoveSpeed(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), getMemberId(), pixelsPerSecond);
        }
    }

    // PlayerSubControl
    protected function setAvatarLocation_v1 (
        targetId :int /* ignored */, x :Number, y :Number, z: Number, orient :Number) :void
    {
        validatePlayerTargetId(targetId);
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setLocationFromUser(x, y, z, orient);
        } else {
            BackendUtils.setAvatarLocation(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), getMemberId(), x, y, z, orient);
        }
    }

    // PlayerSubControl
    protected function setAvatarOrientation_v1 (
        targetId :int /* ignored */, orient :Number) :void
    {
        validatePlayerTargetId(targetId);

        // todo make the below dispatch to all clients
        return;

        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setOrientationFromUser(orient);
        } else {
            BackendUtils.setAvatarOrientation(
                _gameObj, _ctrl.getRoom(), _wctx.getClient(), getMemberId(), orient);
        }
    }

    // LocalSubControl
    protected function localChat_v1 (msg :String) :void
    {
        validateChat(msg);
        // The sendChat() messages will end up being routed through this method on each client.
        displayInfo(null, MessageBundle.taint(msg), ChatCodes.PLACE_CHAT_TYPE);
    }

    // LocalSubControl
    protected function getStageSize_v1 (full :Boolean) :Rectangle
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            if (full) {
                var bounds :Rectangle = _wctx.getTopPanel().getPlaceViewBounds();
                return new Rectangle(0, 0, bounds.width, bounds.height);
            }
            return view.getScrollSize();
        }
        return null;
    }

    // RoomSubControl
    protected function getRoomBounds_v1 (targetId :int /* ignored */) :Rectangle
    {
        validateRoomTargetId(targetId);
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            var metrics :RoomMetrics = view.layout.metrics;
            return new Rectangle(0, 0, metrics.sceneWidth, metrics.sceneHeight);
        }
        return null;
    }

    // LocalSubControl
    protected function stageToRoom_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            p = view.globalToLocal(p);
            p.x -= view.getScrollOffset();
            return p;
        }
        return null;
    }

    // LocalSubControl
    protected function roomToStage_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.localToGlobal(new Point(p.x + view.getScrollOffset(), p.y));
        }
        return null;
    }

    // LocalSubControl
    protected function locationToRoom_v1 (x :Number, y :Number, z :Number)  :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.layout.locationToPoint(new MsoyLocation(x, y, z));
        }

        return null;
    }

    // AgentSubControl
    protected function agent_sendMessage_v1 (name :String, value :Object) :void
    {
        BackendUtils.sendPrivateMessage(
            _gameObj.messageService, _gctx.getClient(), SERVER_AGENT_ID, name, value, "game");
    }

    // MobControl
    protected function setMobDecoration_v1 (
        mobId :String, decoration :DisplayObject, add :Boolean) :void
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                if (add) {
                    sprite.addDecoration(decoration);
                } else {
                    sprite.removeDecoration(decoration);
                }
            }
        }
    }

    // MobControl
    protected function setMobHotSpot_v1 (
        mobId :String, x :Number, y :Number, height :Number = NaN) :void
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                sprite.setHotSpot(x, y, height);
            }
        }
    }

    // TODO: not used?
    protected function getPlayerName_v1 (playerId :int) :String
    {
        var occInfo :OccupantInfo = _ctrl.getRoom().getOccupantInfo(new MemberName("", playerId));
        if (occInfo != null && occInfo is MemberInfo && occInfo.username != null) {
            return occInfo.username.toString();
        }
        return null;
    }

    /**
     * Displays an info message to the player. Default does nothing, so subclasses should 
     * override.
     * TODO: candidate for code sharing with BaseGameBackend?
     */
    protected function displayInfo (bundle :String, msg :String, localType :String = null) :void
    {
        _wctx.getChatDirector().displayInfo(bundle, msg, localType);
    }

    // internal utility method
    protected function getRoomId () :int
    {
        var scene :Scene = _wctx.getSceneDirector().getScene();
        return scene != null ? scene.getId() : 0;
    }

    // internal utility method
    protected function getMySprite () :MemberSprite
    {
        if (isPlaying()) {
            var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
            if (view != null) {
                return view.getMyAvatar();
            }
        }
        return null;
    }

    // internal utility method
    protected function getMobSprite (mobId :String) :MobSprite
    {
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        return (view == null) ? null : view.getMob(_ctrl.getGameId(), mobId);
    }

    // internal utility method
    protected function getAvatarSprite (playerId :int) :MemberSprite
    {
        if (!isPlaying()) {
            log.debug("getAvatarSprite(" + playerId + ") while !isPlaying()");
            return null;
        }
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        if (view == null) {
            log.debug("getAvatarSprite(" + playerId + ") without RoomView");
            return null;
        }
        var roomObj :RoomObject = _ctrl.getRoom();
        if (roomObj == null) {
            log.debug("getAvatarSprite(" + playerId + ") without RoomObject");
            return null;
        }
        var occInfo :OccupantInfo = _gameObj.getOccupantInfo(new MemberName("", playerId));
        if (occInfo == null) {
            log.debug("getAvatarSprite(" + playerId + ") player not in game");
            return null;
        }
        occInfo = roomObj.getOccupantInfo(occInfo.username);
        if (occInfo == null) {
            log.debug("getAvatarSprite(" + playerId + ") player not in room");
            return null;
        }
        var sprite :OccupantSprite = view.getOccupant(occInfo.bodyOid);
        if (sprite == null) {
            log.debug("getAvatarSprite(" + playerId + ") sprite not found");
            return null;
        }
        return sprite as MemberSprite;
    }

    /**
     * Verify that the supplied chat message is valid.
     */
    protected function validateChat (msg :String) :void
    {
        if (StringUtil.isBlank(msg)) {
            throw new ArgumentError("Empty chat may not be displayed.");
        }
    }

    protected function validatePlayerTargetId (targetId :int) :void
    {
        if (targetId != 0) {
            throw new Error("Unexpected player target [id=" + targetId + "]");
        }
    }

    protected function validateRoomTargetId (targetId :int) :void
    {
        if (targetId != 0) {
            throw new Error("Unexpected room target [id=" + targetId + "]");
        }
    }

    protected function getMemberId () :int
    {
        return _wctx.getMemberObject().getMemberId();
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;

    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _gameNetAdapter :BackendNetAdapter;
    protected var _playerNetAdapter :BackendNetAdapter;
    protected var _roomPropsNetAdapter :BackendNetAdapter;
    protected var _avatarAdapter :BackendAvatarAdapter;
}
}
