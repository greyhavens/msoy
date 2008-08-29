//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;

import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.bureau.util.MsoyBureauContext;

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
        // info.name = data[ix ++];
        // info.state = data[ix ++];
        // info.x = data[ix ++];
        // info.y = data[ix ++];
        // info.z = data[ix ++];
        // info.orientation = data[ix ++];
        // info.moveSpeed = data[ix ++];
        // info.isMoving = data[ix ++];
        // info.isIdle = data[ix ++];
        // info.stageBounds = data[ix ++];
        return [null, null, null, null, null, null, null, null, null, null];
    }

    protected function spawnMob_v1 (roomId :int, id :String, name :String) :Boolean
    {
        return false;
    }

    protected function despawnMob_v1 (roomId :int, id :String) :Boolean
    {
        return false;
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
        return 0;
    }

    protected function deactivateGame_v1 (playerId :int) :Boolean
    {
        return false;
    }

    protected function completeTask_v1 (playerId :int, taskId :String, payout :Number) :Boolean
    {
        return false;
    }

    protected function playAvatarAction_v1 (playerId :int, action :String) :Boolean
    {
        return false;
    }

    protected function setAvatarState_v1 (playerId :int, state :String) :Boolean
    {
        return false;
    }

    protected function setAvatarMoveSpeed_v1 (playerId :int, pixelsPerSecond :Number) :Boolean
    {
        return false;
    }

    protected function setAvatarLocation_v1 (
        playerId :int, x :Number, y :Number, z: Number, orient :Number) :Boolean
    {
        var roomId :int = getPlayerRoomId(playerId);
        if (roomId == 0) {
            return false;
        }

        // TODO: maybe expose ThaneAVRGameController::SceneBinding instead of always doing lookup 
        // twice
        var roomObj :RoomObject = _controller.getRoom(roomId);
        if (roomObj == null) {
            log.warning("Room object is null [roomId=" + roomId + "]");
            return;
        }

        BackendUtils.setAvatarLocation(
            _gameObj, roomObj, _controller.getRoomClient(roomId), playerId, x, y, z, orient);
        return true;
    }

    protected function setAvatarOrientation_v1 (playerId :int, orient :Number) :Boolean
    {
        return false;
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
