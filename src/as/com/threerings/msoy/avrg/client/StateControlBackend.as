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

import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.client.WorldContext;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertyEntry;

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
        _gameObj.addListener(_messageListener);

        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerMessageListener);
    }

    public function shutdown () :void
    {
    }

    public function populateSubProperties (o :Object) :void
    {
        // StateControl (sub)
        o["getRoomProperty_v1"] = getRoomProperty_v1;
        o["setRoomProperty_v1"] = setRoomProperty_v1;
        o["getRoomProperties_v1"] = getRoomProperties_v1;
    }

    protected function getRoomProperties_v1 () :Object
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        return (view == null) ? {} : view.getRoomProperties();
    }

    protected function getRoomProperty_v1 (key :String) :Object
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        return (view == null) ? null : view.getRoomProperty(key);
    }

    protected function setRoomProperty_v1 (key :String, value :Object) :Boolean
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (key != null && view != null) {
            return view.getRoomController().setRoomProperty(key, value);
        }
        return false;
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
