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
    }

    protected function populateProperties (props :Object) :void
    {
        props["player_sendMessage_v1"] = player_sendMessage_v1;
    }

    protected function player_sendMessage_v1 (targetId :int, name :String, value :Object) :void
    {
        var encoded :Object = ObjectMarshaller.encode(value, false);
        var targets :TypedArray = TypedArray.create(int);
        targets.push(targetId);
        _gameObj.messageService.sendPrivateMessage(
            _ctx.getClient(), name, encoded, targets, loggingInvocationListener("sendMessage"));
    }

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
