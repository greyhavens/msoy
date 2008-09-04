//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.spot.data.SceneLocation;

import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.bureau.util.MsoyBureauContext;

import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.MobInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesObject;

/**
 * Manages the connection to the server agent thane user code for an AVRG.
 */
public class ThaneAVRGameBackend
{
    public static const log :Log = Log.getLog(ThaneAVRGameBackend);

    /**
     * Constructs a new base avr game backend.
     */
    public function ThaneAVRGameBackend (
        ctx :MsoyBureauContext, gameObj :AVRGameObject, controller :ThaneAVRGameController)
    {
        _ctx = ctx;
        _gameObj = gameObj;
        _controller = controller;
    }

    /**
     * Determines if we are connected to the front end code.
     */
    public function isConnected () :Boolean
    {
        return _userFuncs != null;
    }

    /**
     * Retrieves the function pointer that will be called with the user properties when the front 
     * end connects.
     */
    public function getConnectListener () :Function
    {
        return handleUserCodeConnect;
    }

    /**
     * Cleans up our references when this agent is going away.
     */
    public function shutdown () :void
    {
        // shut down sub-backends, remove listeners
        _privateMessageAdapter.release();
    }

    /**
     * Informs the user code that the room is no longer accessible.
     */
    public function roomUnloaded (roomId :int) :void
    {
        callUserCode("roomUnloaded_v1", roomId);
    }

    /**
     * Informs the user code that a player has joined the game.
     */
    public function playerJoinedGame (memberId :int) :void
    {
        log.info("Player joined game: " + memberId);
        callUserCode("playerJoinedGame_v1", memberId);
    }

    /**
     * Informs the user code that a player has left the game.
     */
    public function playerLeftGame (memberId :int) :void
    {
        log.info("Player left game: " + memberId);
        callUserCode("playerLeftGame_v1", memberId);
    }

    /**
     * Informs the user code that a player has entered a room.
     */
    public function playerEnteredRoom (memberId :int, roomId :int) :void
    {
        log.info("Player entered room: " + memberId + ", " + roomId);
        callUserCode("enteredRoom_v1", memberId, roomId);
        callUserCode("playerEntered_v1", roomId, memberId);
    }

    /**
     * Informs the user code that a player has left a room.
     */
    public function playerLeftRoom (memberId :int, roomId :int) :void
    {
        log.info("Player left room: " + memberId + ", " + roomId);
        callUserCode("playerLeft_v1", roomId, memberId);
        callUserCode("leftRoom_v1", memberId, roomId);
    }

    /**
     * Creates an adapter that will dispatch to the player controls in the user code.
     */
    public function createPlayerNetAdapter (player :PlayerObject) :BackendNetAdapter
    {
        var adapter :BackendNetAdapter = new BackendNetAdapter(
            player, RoomPropertiesObject.USER_MESSAGE, _userFuncs, "player_propertyWasSet_v1", 
            null);
        adapter.setTargetId(player.getMemberId());
        return adapter;
    }

    /**
     * Creates an adapter that will dispatch avatar actions to the user code.
     */
    public function createAvatarAdapter (room :RoomObject) :BackendAvatarAdapter
    {
        var adapter :BackendAvatarAdapter = new BackendAvatarAdapter(
            _gameObj, room, _userFuncs, "playerMoved_v1", "actorStateSet_v1", 
            "actorAppearanceChanged_v1");
        return adapter;
    }

    /**
     * Informs the user code that a mob has been spawned and is ready to control.
     */
    public function mobSpawned (roomId :int, mobId :String) :void
    {
        callUserCode("mobSpawned_v1", roomId, mobId);
    }

    /**
     * Informs the user code that a mob has now been fully removed.
     */
    public function mobRemoved (roomId :int, mobId :String) :void
    {
        callUserCode("mobRemoved_v1", roomId, mobId);
    }

    /**
     * Informs the use code that a mob change has now taken effect.
     */
    public function mobChanged (roomId :int, mobId :String) :void
    {
        callUserCode("mobAppearanceChanged_v1", roomId, mobId);
    }

    protected function handleUserCodeConnect (evt :Object) :void
    {
        var props :Object = evt.props;

        if (_userFuncs != null) {
            props.alreadyConnected = true;
            log.warning("User code connected more than once. [backend=" + this + "].");
            return;
        }

        _userFuncs = props.userProps;

        var ourProps :Object = new Object();
        populateProperties(ourProps);
        props.hostProps = ourProps;

        _privateMessageAdapter = new BackendNetAdapter(
            _ctx.getClient().getClientObject(), 
            WhirledPlayerObject.getMessageName(_gameObj.getOid()), _userFuncs, null, 
            "game_messageReceived_v1");
    }

    protected function populateProperties (o :Object) :void
    {
        // .game
        o["game_getPlayerIds_v1"] = game_getPlayerIds_v1;
        o["game_sendMessage_v1"] = game_sendMessage_v1;
        o["isRoomLoaded_v1"] = isRoomLoaded_v1

        // .game.props
        o["game_getGameData_v1"] = game_getGameData_v1;
        o["game_setProperty_v1"] = game_setProperty_v1;

        // .getRoom()
        o["room_getPlayerIds_v1"] = room_getPlayerIds_v1;
        o["isPlayerHere_v1"] = isPlayerHere_v1;
        o["getAvatarInfo_v1"] = getAvatarInfo_v1;
        o["spawnMob_v1"] = spawnMob_v1;
        o["despawnMob_v1"] = despawnMob_v1;
        o["getSpawnedMobs_v1"] = getSpawnedMobs_v1;
        o["moveMob_v1"] = moveMob_v1;
        o["room_sendMessage_v1"] = room_sendMessage_v1;

        // .getRoom().props
        o["room_getGameData_v1"] = room_getGameData_v1;
        o["room_setProperty_v1"] = room_setProperty_v1;

        // .getPlayer()
        o["getRoomId_v1"] = getRoomId_v1;
        o["deactivateGame_v1"] = deactivateGame_v1;
        o["completeTask_v1"] = completeTask_v1;
        o["playAvatarAction_v1"] = playAvatarAction_v1;
        o["setAvatarState_v1"] = setAvatarState_v1;
        o["setAvatarMoveSpeed_v1"] = setAvatarMoveSpeed_v1;
        o["setAvatarLocation_v1"] = setAvatarLocation_v1;
        o["setAvatarOrientation_v1"] = setAvatarOrientation_v1;
        o["player_sendMessage_v1"] = player_sendMessage_v1;

        // .getPlayer().props
        o["player_getGameData_v1"] = player_getGameData_v1;
        o["player_setProperty_v1"] = player_setProperty_v1;
    }

    // -------------------- .game --------------------
    protected function game_getPlayerIds_v1 () :Array
    {
        return BackendUtils.getPlayerIds(_gameObj);
    }

    protected function game_sendMessage_v1 (name :String, value :Object) :void
    {
        BackendUtils.sendMessage(_gameObj.messageService, _ctx.getClient(), name, value, "game");
    }

    protected function isRoomLoaded_v1 (roomId :int) :Boolean
    {
        return _controller.getRoomProps(roomId) != null;
    }
    
    // -------------------- .game.props --------------------
    protected function game_getGameData_v1 (targetId :int) :Object
    {
        if (targetId != 0) {
            throw new Error("Internal error: unexpected target id");
        }
        return _gameObj.getUserProps();
    }

    protected function game_setProperty_v1 (
        targetId :int, name :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
        if (targetId != 0) {
            throw new Error("Internal error: unexpected target id");
        }
        BackendUtils.encodeAndSet(_ctx.getClient(), _gameObj, name, value, key, isArray, immediate);
    }
    
    // -------------------- .getRoom() --------------------
    protected function room_getPlayerIds_v1 (roomId :int) :Array
    {
        var playerIds :Array = [];
        for each (var pl :PlayerLocation in _gameObj.playerLocs) {
            if (pl.sceneId == roomId) {
                playerIds.push(pl.playerId);
            }
        }
        return playerIds;
    }

    protected function isPlayerHere_v1 (roomId :int, playerId :int) :Boolean
    {
        var playerRoomId :int = getPlayerRoomId(playerId);
        return playerRoomId != 0 && playerRoomId == roomId;
    }

    protected function getAvatarInfo_v1 (roomId :int, playerId :int) :Array
    {
        var roomObj :RoomObject = _controller.getRoom(getPlayerRoomId(playerId));
        if (roomObj == null) {
            log.debug("Room not found [playerId=" + playerId + "]");
            return null;
        }

        var actorInfo :ActorInfo;
        actorInfo = BackendUtils.resolvePlayerWorldInfo(_gameObj, roomObj, playerId) as ActorInfo;
        if (actorInfo == null) {
            log.debug("ActorInfo not found [playerId=" + playerId + "]");
            return null;
        }

        var loc :MsoyLocation;
        loc = (roomObj.occupantLocs.get(actorInfo.bodyOid) as SceneLocation).loc as MsoyLocation;
        if (loc == null) {
            log.debug(
                "Location not found [playerId=" + playerId + ", boyOid=" + actorInfo.bodyOid +
                ", sceneLoc=" + roomObj.occupantLocs.get(actorInfo.bodyOid) + "]");
            return null;
        }

        var data :Array = new Array(10);
        data[0] = actorInfo.username.toString();
        data[1] = actorInfo.getState();
        data[2] = loc.x;
        data[3] = loc.y;
        data[4] = loc.z;
        data[5] = loc.orient;
        data[6] = 1.0; // TODO moveSpeed
        data[7] = false; // TODO isMoving
        data[8] = actorInfo.status == OccupantInfo.IDLE;
        data[9] = null; // TODO: stageBounds
        return data;
    }

    protected function spawnMob_v1 (
        roomId :int, mobId :String, mobName :String, x :Number = 0, y :Number = 0, z :Number = 0) :void
    {
        if (StringUtil.isBlank(mobId)) {
            log.warning("Blank mobId in spawnMob");
            return;
        }

        if (StringUtil.isBlank(mobName)) {
            log.warning("Blank mobName in spawnMob");
            return;
        }

        var roomObj :RoomObject = _controller.getRoom(roomId, "for spawnMob");
        if (roomObj == null) {
            return;
        }

        var loc :MsoyLocation = new MsoyLocation(x, y, z);
        roomObj.roomService.spawnMob(  
            _controller.getRoomClient(roomId), _controller.getGameId(), mobId, mobName, loc,
            BackendUtils.loggingInvocationListener("spawnMob"));
    }

    protected function despawnMob_v1 (roomId :int, mobId :String) :void
    {
        if (StringUtil.isBlank(mobId)) {
            log.warning("Blank mobId in despawnMob");
            return;
        }

        var roomObj :RoomObject = _controller.getRoom(roomId, "for despawnMob");
        if (roomObj == null) {
            return;
        }

        roomObj.roomService.despawnMob(
            _controller.getRoomClient(roomId), _controller.getGameId(), mobId,
            BackendUtils.loggingInvocationListener("despawnMob"));
    }

    protected function getSpawnedMobs_v1 (roomId :int) :Array
    {
        return _controller.getMobIds(roomId);
    }

    protected function moveMob_v1 (roomId :int, id :String, x :Number, y :Number, z :Number) :void
    {
        var roomObj :RoomObject = _controller.getRoom(roomId, "for moveMob");
        if (roomObj == null) {
            return;
        }

        roomObj.roomService.moveMob(
            _controller.getRoomClient(roomId), _controller.getGameId(), id, 
            new MsoyLocation(x, y, z), BackendUtils.loggingConfirmListener("moveMob"));
    }

    protected function room_sendMessage_v1 (roomId :int, name :String, value :Object) :void
    {
        var roomProps :RoomPropertiesObject = _controller.getRoomProps(roomId);
        if (roomProps == null) {
            throw new Error("Room not loaded [roomId=" + roomId + "]");
        }
        BackendUtils.sendMessage(
            roomProps.messageService, _controller.getRoomClient(roomId), name, value, "room");
    }

    
    // -------------------- .getRoom().props --------------------
    protected function room_getGameData_v1 (roomId :int) :Object
    {
        var roomProps :RoomPropertiesObject = _controller.getRoomProps(roomId);
        if (roomProps == null) {
            // TODO: is there a more appropriate way to deal with room errors?
            throw new Error("Room not loaded [roomId=" + roomId + "]");
        }
        return roomProps.getUserProps();
    }

    protected function room_setProperty_v1 (
        roomId :int, name :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
        var roomProps :RoomPropertiesObject = _controller.getRoomProps(roomId);
        if (roomProps == null) {
            throw new Error("Room not loaded [roomId=" + roomId + "]");
        }
        BackendUtils.encodeAndSet(
            _controller.getRoomClient(roomId), roomProps, name, value, key, isArray, immediate);
    }

    // -------------------- .getPlayer() --------------------
    protected function getRoomId_v1 (playerId :int) :int
    {
        return getPlayerRoomId(playerId);
    }

    protected function deactivateGame_v1 (playerId :int) :void
    {
        _controller.deactivateGame(playerId);
    }

    protected function completeTask_v1 (playerId :int, taskId :String, payout :Number) :void
    {
    }

    protected function playAvatarAction_v1 (playerId :int, action :String) :void
    {
        resolveRoomAndClient(playerId, function (roomObj :RoomObject, client :Client) :void {
            BackendUtils.playAvatarAction(_gameObj, roomObj, client, playerId, action);
        });
    }

    protected function setAvatarState_v1 (playerId :int, state :String) :void
    {
        resolveRoomAndClient(playerId, function (roomObj :RoomObject, client :Client) :void {
            BackendUtils.setAvatarState(_gameObj, roomObj, client, playerId, state);
        });
    }

    protected function setAvatarMoveSpeed_v1 (playerId :int, pixelsPerSecond :Number) :void
    {
        resolveRoomAndClient(playerId, function (roomObj :RoomObject, client :Client) :void {
            BackendUtils.setAvatarMoveSpeed(_gameObj, roomObj, client, playerId, pixelsPerSecond);
        });
    }

    protected function setAvatarLocation_v1 (
        playerId :int, x :Number, y :Number, z: Number, orient :Number) :void
    {
        resolveRoomAndClient(playerId, function (roomObj :RoomObject, client :Client) :void {
            BackendUtils.setAvatarLocation(_gameObj, roomObj, client, playerId, x, y, z, orient);
        });
    }

    protected function setAvatarOrientation_v1 (playerId :int, orient :Number) :void
    {
        resolveRoomAndClient(playerId, function (roomObj :RoomObject, client :Client) :void {
            BackendUtils.setAvatarOrientation(_gameObj, roomObj, client, playerId, orient);
        });
    }

    protected function player_sendMessage_v1 (playerId :int, name :String, value :Object) :void
    {
        BackendUtils.sendPrivateMessage(
            _gameObj.messageService, _ctx.getClient(), playerId, name, value, "room");
    }

    // -------------------- .getPlayer().props --------------------
    protected function player_getGameData_v1 (playerId :int) :Object
    {
        var player :PlayerObject = _controller.getPlayer(playerId);
        if (player == null) {
            throw new Error("Player not found [playerId=" + playerId + "]");
        }
        return player.getUserProps();
    }

    protected function player_setProperty_v1 (
        playerId :int, name :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
        var player :PlayerObject = _controller.getPlayer(playerId);
        if (player == null) {
            throw new Error("Player not found [playerId=" + playerId + "]");
        }
        BackendUtils.encodeAndSet(
            _ctx.getClient(), player, name, value, key, isArray, immediate);
    }

    // -------------------- end of versioned methods --------------------

    protected function getPlayerRoomId (playerId :int) :int
    {
        var pl :PlayerLocation = _gameObj.playerLocs.get(playerId) as PlayerLocation;
        if (pl == null) {
            log.warning("Room not found for player [id=" + playerId + "]");
            return 0;
        }
        return pl.sceneId;
    }

    /**
     * Find the given player's room and the client session that is connected to the room and call
     * fn(roomObj, client) if found.
     */
    protected function resolveRoomAndClient (playerId :int, fn :Function) :void
    {
        var roomId :int = getPlayerRoomId(playerId);
        if (roomId == 0) {
            return;
        }

        // TODO: maybe expose ThaneAVRGameController::SceneBinding instead of always doing lookup 
        // twice
        var roomObj :RoomObject = _controller.getRoom(roomId, "");
        if (roomObj == null) {
            return;
        }

        fn(roomObj, _controller.getRoomClient(roomId));
    }

    /**
     * Call an exposed function in usercode.
     */
    protected function callUserCode (name :String, ... args) :*
    {
        if (_userFuncs != null) {
            try {
                var func :Function = (_userFuncs[name] as Function);
                if (func == null) {
                    log.warning("User code function " + name + " not found.");
                } else {
                    return func.apply(null, args);
                }
            } catch (err :Error) {
                log.warning("Error in user code: " + err);
                log.logStackTrace(err);
            }
        } else {
            log.warning("Calling user code " + name + " before connection.");
        }
        return undefined;
    }

    protected var _userFuncs :Object;
    protected var _ctx :MsoyBureauContext; // this is for the game server
    protected var _controller :ThaneAVRGameController;
    protected var _gameObj :AVRGameObject;
    protected var _privateMessageAdapter :BackendNetAdapter;
}

}
