//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.events.Event;

import com.threerings.io.TypedArray;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;
import com.whirled.game.data.WhirledPlayerObject;

public class ThaneAVRGameBackend
{
    public static const log :Log = Log.getLog(ThaneAVRGameBackend);

    /**
     * Constructs a new base avr game backend.
     */
    public function ThaneAVRGameBackend (ctx :MsoyBureauContext, gameObj :AVRGameObject)
    {
        _ctx = ctx;
        _gameObj = gameObj;
        _ctx.getClient().getClientObject().addListener(_privateMessageListener);
    }

    public function isConnected () :Boolean
    {
        return _userFuncs != null;
    }

    public function getConnectListener () :Function
    {
        return handleUserCodeConnect;
    }

    public function shutdown () :void
    {
        // shut down sub-backends, remove listeners
        _ctx.getClient().getClientObject().removeListener(_privateMessageListener);
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

        BackendUtils.initPropertyChangeDispatch(_gameObj, _userFuncs, "game_propertyWasSet_v1");
    }

    protected function populateProperties (o :Object) :void
    {
        // .game
        o["game_getPlayerIds_v1"] = game_getPlayerIds_v1;
        o["game_sendMessage_v1"] = game_sendMessage_v1;

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
        return [];
    }

    protected function game_sendMessage_v1 (name :String, value :Object) :void
    {
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
        return [];
    }

    protected function isPlayerHere_v1 (roomId :int, playerId :int) :Boolean
    {
        return false;
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
    }

    
    // -------------------- .getRoom().props --------------------
    protected function room_getGameData_v1 (roomId :int) :Object
    {
        return {};
    }

    protected function room_setProperty_v1 (
        roomId :int, name :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
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
        return false;
    }

    protected function setAvatarOrientation_v1 (playerId :int, orient :Number) :Boolean
    {
        return false;
    }

    protected function player_sendMessage_v1 (playerId :int, name :String, value :Object) :void
    {
        var encoded :Object = ObjectMarshaller.encode(value, false);
        var targets :TypedArray = TypedArray.create(int);
        targets.push(playerId);
        _gameObj.messageService.sendPrivateMessage(
            _ctx.getClient(), name, encoded, targets, loggingInvocationListener("sendMessage"));
    }

    // -------------------- .getPlayer().props --------------------
    protected function player_getGameData_v1 (playerId :int) :Object
    {
        return {};
    }

    protected function player_setProperty_v1 (
        playerId :int, name :String, value :Object, key :Object, isArray :Boolean, 
        immediate :Boolean) :void
    {
    }

    // -------------------- end of versioned methods --------------------

    // internal utility method
    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    /**
     * Call an exposed function in usercode.
     */
    public function callUserCode (name :String, ... args) :*
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
        }
        return undefined;
    }

    protected var _userFuncs :Object;
    protected var _ctx :MsoyBureauContext;
    protected var _gameObj :AVRGameObject;
    protected var _privateMessageListener :MessageAdapter = new MessageAdapter(
        function (event: MessageEvent) :void {
            var name :String = event.getName();
            if (WhirledPlayerObject.isFromGame(name, _gameObj.getOid())) {
                var args :Array = event.getArgs();
                var mname :String = (args[0] as String);
                var data :Object = ObjectMarshaller.decode(args[1]);
                var senderId :int = (args[2] as int);
                callUserCode("game_messageReceived_v1", mname, data, senderId);
            }
        });
}

}
