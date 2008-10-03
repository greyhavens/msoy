//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc. Please do not redistribute.

package com.threerings.msoy.bureau.client {

import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.io.TypedArray;

import com.threerings.util.Log;

import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.dobj.SubscriberAdapter;

import com.threerings.presents.util.SafeSubscriber;

import com.threerings.bureau.client.Agent;

import com.whirled.bureau.client.UserCode;

import com.threerings.msoy.bureau.util.MsoyBureauContext;

import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;

import com.threerings.msoy.avrg.client.ThaneAVRGameController;

/** The container for a user's avr game control code. */
public class AVRGameAgent extends Agent
{
    public static var log :Log = Log.getLog(AVRGameAgent);

    public function AVRGameAgent (ctx :MsoyBureauContext)
    {
        _ctx = ctx;

        _traceTimer.addEventListener(TimerEvent.TIMER, handleTimer);
    }

    // from Agent
    public override function start () :void
    {
        log.info("Starting agent", "agent", _agentObj.which());

        // subscribe to the game object
        var delegator :Subscriber = 
            new SubscriberAdapter(objectAvailable, requestFailed);

        log.info("Subscribing to game object", "oid", gameAgentObj.gameOid);

        _subscriber = new SafeSubscriber(gameAgentObj.gameOid, delegator);
        _subscriber.subscribe(_ctx.getDObjectManager());

        // download the code
        _ctx.getUserCodeLoader().load(_agentObj.code, _agentObj.className, gotUserCode);
    }

    // from Agent
    public override function stop () :void
    {
        log.info("Stopping agent", "agent", _agentObj.which());

        _subscriber.unsubscribe(_ctx.getDObjectManager());
        _subscriber = null;
        _gameObj = null;
        _agentObj = null;

        handleTimer(null);
        _traceTimer.stop();
        _traceTimer.removeEventListener(TimerEvent.TIMER, handleTimer);
        _traceTimer = null;

        if (_controller != null) {
            _controller.shutdown();
            _controller = null;
        }

        if (_userCode != null) {
            _userCode.release();
            _userCode = null;
        }
    }

    /**
     * Outputs a message to the user code's internal trace method. This is used to avoid
     * generating warnings and traces from backend and controller code.
     */
    public function outputToUserCode (msg :String, err :Error = null) :void
    {
        if (_userCode != null) {
            _userCode.outputTrace(msg, err);
        }
    }

    /** Access the agent object, casted to a game agent object. */
    protected function get gameAgentObj () :AVRGameAgentObject
    {
        return _agentObj as AVRGameAgentObject;
    }

    /**
     * Callback for when the request to subscribe to the game object finishes and the object is 
     * available.
     */
    protected function objectAvailable (gameObj :AVRGameObject) :void
    {
        log.info("Subscribed to game object", "gameObj", gameObj.which());
        _gameObj = gameObj;

        _controller = createController();
        _controller.init(_ctx, _gameObj, this, gameAgentObj);

        if (_userCode != null && _gameObj != null) {
            launchUserCode();
        }
    }

    /**
     * Callback for when the a request to subscribe to the game object fails.
     */
    protected function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Could not subscribe to game object", "oid", oid, cause);
    }

    /**
     * Callback for when the user code is available.
     */
    protected function gotUserCode (userCode :UserCode) :void
    {
        if (userCode == null) {
            log.warning("Unable to load user code", "agent", _agentObj);
            return;
        }

        _userCode = userCode;
        log.info("Loaded user code", "userCode", _userCode);

        if (_userCode != null && _gameObj != null) {
            launchUserCode();
        }
    }

    /**
     * Called once the game object and the user code (domain) are available.
     */
    protected function launchUserCode () :void
    {
        _userCode.connect(_controller.backend.getConnectListener(), relayTrace);
        
        if (!_controller.backend.isConnected()) {
            log.info("Could not connect to user code");
            return;
        }

        _controller.agentReady();
    }

    /**
     * Called whenever a trace() is sent back to us from a usercode Domain; we relay
     * it back to the server.
     */
    protected function relayTrace (trace :String) :void
    {
        _traceOutput.push(trace);
        _traceTimer.start();
    }

    protected function handleTimer (event :TimerEvent) :void
    {
        if (_traceOutput.length == 0) {
            _traceTimer.stop();

        } else {
            _gameObj.manager.invoke("agentTrace", _traceOutput);
            _traceOutput.length = 0;
        }
    }

    /**
     * Creates the controller for this agent. 
     */
    protected function createController () :ThaneAVRGameController
    {
        return new ThaneAVRGameController();
    }

    protected var _subscriber :SafeSubscriber;
    protected var _ctx :MsoyBureauContext;
    protected var _gameObj :AVRGameObject;
    protected var _userCode :UserCode;
    protected var _controller :ThaneAVRGameController;
    protected var _traceOutput :TypedArray = TypedArray.create(String);
    protected var _traceTimer :Timer = new Timer(1000);
}

}
