//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;

import com.threerings.util.Name;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.PlayerObject;

public class AVRGameControlBackend extends ControlBackend
{
    public static const log :Log = Log.getLog(AVRGameControlBackend);

    public function AVRGameControlBackend (
        mctx :WorldContext, gctx :GameContext, gameObj :AVRGameObject, ctrl :AVRGameController)
    {
        _mctx = mctx;
        _gctx = gctx;
        _gameObj = gameObj;

        _gameObj.addListener(_stateListener);
        _gameObj.addListener(_messageListener);

        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerStateListener);
        _playerObj.addListener(_playerMessageListener);
    }

    // from ControlBackend
     override public function shutdown () :void
     {
         super.shutdown();
        
         _gameObj.removeListener(_stateListener);
         _playerObj.removeListener(_playerStateListener);
     }

    // from GameControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["getProperty_v1"] = getProperty_v1;
        o["setProperty_v1"] = setProperty_v1;
//        o["setPropertyAt_v1"] = setPropertyAt_v1;
        o["getPlayerProperty_v1"] = getPlayerProperty_v1;
        o["setPlayerProperty_v1"] = setPlayerProperty_v1;
//        o["setPlayerPropertyAt_v1"] = setPlayerPropertyAt_v1;
        o["sendMessage_v1"] = sendMessage_v1;
    }

    protected function getProperty_v1 (key :String) :Object
    {
        var entry :GameState = GameState(_gameObj.state.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function setProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
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
    
    protected function getPlayerProperty_v1 (key :String) :Object
    {
        var entry :GameState = GameState(_playerObj.gameState.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function setPlayerProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
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
        _gameObj.avrgService.sendMessage(_gctx.getClient(), key, value, playerId,
                                         loggingInvocationListener("sendMessage"));
        return true;
    }

     protected function callStateChanged (entry :GameState) :void
     {
         callUserCode("stateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
     }
    
     protected function callPlayerStateChanged (entry :GameState) :void
     {
         callUserCode("playerStateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
     }

    protected function loggingConfirmListener (svc :String) :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            Log.getLog(this).warning(
                "Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            Log.getLog(this).warning(
                "Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _stateListener :SetAdapter = new SetAdapter(
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
            if (AVRGameObject.USER_MESSAGE == event.getName()) {
                var args :Array = event.getArgs();
                var key :String = (args[0] as String);
                callUserCode("messageReceived_v1", key, ObjectMarshaller.decode(args[1]));
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
                callUserCode("messageReceived_v1", key, ObjectMarshaller.decode(args[1]));
            }
        });

}
}
