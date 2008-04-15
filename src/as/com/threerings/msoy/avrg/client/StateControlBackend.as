//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.dobj.*;

import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomPropertyEntry;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.PlayerObject;

public class StateControlBackend
{
    public static const log :Log = Log.getLog(StateControlBackend);

    public function StateControlBackend (
        wctx :WorldContext, gctx :GameContext, backend :AVRGameBackend, gameObj :AVRGameObject)
    {
        _gctx = gctx;
        _wctx = wctx;
        _backend = backend;

        _gameObj = gameObj;
        _gameObj.addListener(_gameListener);
        _gameObj.addListener(_messageListener);

        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerStateListener);
        _playerObj.addListener(_playerMessageListener);
    }

    public function shutdown () :void
    {
         _gameObj.removeListener(_gameListener);
         _playerObj.removeListener(_playerStateListener);
    }

    public function populateSubProperties (o :Object) :void
    {
        // StateControl (sub)
        o["getProperty_v1"] = getProperty_v1;
        o["getProperties_v1"] = getProperties_v1;
        o["setProperty_v1"] = setProperty_v1;
        o["getRoomProperty_v1"] = getRoomProperty_v1;
        o["setRoomProperty_v1"] = setRoomProperty_v1;
        o["getRoomProperties_v1"] = getRoomProperties_v1;
//        o["setPropertyAt_v1"] = setPropertyAt_v1;
        o["getPlayerProperty_v1"] = getPlayerProperty_v1;
        o["setPlayerProperty_v1"] = setPlayerProperty_v1;
//        o["setPlayerPropertyAt_v1"] = setPlayerPropertyAt_v1;
        o["sendMessage_v1"] = sendMessage_v1;
    }

    protected function getProperty_v1 (key :String) :Object
    {
        if (key == null) {
            return null;
        }
        var entry :GameState = GameState(_gameObj.state.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function getProperties_v1 () :Object
    {
        var props :Object = { };
        for each (var entry :GameState in _gameObj.state.toArray()) {
            if (entry.value != null) {
                props[entry.key] = ObjectMarshaller.decode(entry.value);
            }
        }
        return props;
    }

    protected function setProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
        if (key == null) {
            return false;
        }

        var wgsvc :AVRGameService = _gameObj.avrgService;
        if (value == null) {
            wgsvc.deleteProperty(_gctx.getClient(), key,
                                 loggingConfirmListener("deleteProperty"));

        } else {
            wgsvc.setProperty(_gctx.getClient(), key,
                              ObjectMarshaller.validateAndEncode(value), persistent,
                              loggingConfirmListener("setProperty"));

        }
        return true;
    }

    protected function getRoomProperties_v1 () :Object
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            return null;
        }
        var props :Object = { };
        var roomObj :RoomObject = view.getRoomObject();
        for each (var entry :RoomPropertyEntry in roomObj.roomProperties.toArray()) {
            if (entry.value != null) {
                props[entry.key] = ObjectMarshaller.decode(entry.value);
            }
        }
        return props;
    }

    protected function getRoomProperty_v1 (key :String) :Object
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (key != null && view != null) {
            var roomObj :RoomObject = view.getRoomObject();
            var entry :RoomPropertyEntry = roomObj.roomProperties.get(key) as RoomPropertyEntry;
            if (entry != null) {
                return ObjectMarshaller.decode(entry.value);
            }
        }
        return null;
    }

    protected function setRoomProperty_v1 (key :String, value :Object) :Boolean
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (key != null && view != null) {
            return view.getRoomController().setRoomProperty(key, value);
        }
        return false;
    }
   
    protected function getPlayerProperty_v1 (key :String) :Object
    {
        if (key == null) {
            return null;
        }
        var entry :GameState = GameState(_playerObj.gameState.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function setPlayerProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
        if (key == null) {
            return false;
        }

        var wgsvc :AVRGameService = _gameObj.avrgService;
        if (value == null) {
            wgsvc.deletePlayerProperty(_gctx.getClient(), key,
                                       loggingConfirmListener("deletePlayerProperty"));

        } else {
            wgsvc.setPlayerProperty(_gctx.getClient(), key,
                                    ObjectMarshaller.validateAndEncode(value), persistent,
                                    loggingConfirmListener("setPlayerProperty"));
        }
        return true;
    }

    protected function sendMessage_v1 (key :String, value :Object, playerId :int) :Boolean
    {
        if (key == null) {
            return false;
        }
        _gameObj.avrgService.sendMessage(_gctx.getClient(), key,
                                         ObjectMarshaller.validateAndEncode(value),
                                         playerId, loggingInvocationListener("sendMessage"));
        return true;
    }

    protected function callStateChanged (entry :GameState) :void
    {
        _backend.callUserCode(
            "stateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
    }
    
    protected function callPlayerStateChanged (entry :GameState) :void
    {
        _backend.callUserCode(
            "playerStateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
    }

    protected function loggingConfirmListener (svc :String, processed :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        }, processed);
    }

    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _backend :AVRGameBackend;
    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _gameListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == AVRGameObject.STATE) {
                callStateChanged(event.getEntry() as GameState);
            }
        },
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == AVRGameObject.STATE) {
                callStateChanged(event.getEntry() as GameState);
            }
        });

    protected var _messageListener :MessageListener = new MessageAdapter(
        function (event :MessageEvent) :void {
            var name :String = event.getName();
            var args :Array = event.getArgs();
            var key :String = (args[0] as String);
            if (AVRGameObject.USER_MESSAGE == name) {
                _backend.callUserCode(
                    "messageReceived_v1", key, ObjectMarshaller.decode(args[1]));

            } else if (AVRGameObject.TICKER == name) {
                _backend.callUserCode("messageReceived_v1", key, (args[1] as int));
            }
        });

    protected var _playerStateListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == PlayerObject.GAME_STATE) {
                callPlayerStateChanged(event.getEntry() as GameState);
            }
        },
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == PlayerObject.GAME_STATE) {
                callPlayerStateChanged(event.getEntry() as GameState);
            }
        });

    protected var _playerMessageListener :MessageListener = new MessageAdapter(
        function (event :MessageEvent) :void {
            if (AVRGameObject.USER_MESSAGE + ":" + _gameObj.getOid() == event.getName()) {
                var args :Array = event.getArgs();
                var key :String = (args[0] as String);
                _backend.callUserCode("messageReceived_v1", key, ObjectMarshaller.decode(args[1]));
            }
        });
}
}
