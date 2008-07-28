//
// $Id$
//
// Copyright (c) 2007 Three Rings Design, Inc. Please do not redistribute.

package com.threerings.msoy.bureau.client {

import com.threerings.bureau.Log;
import com.threerings.bureau.client.Agent;
import com.threerings.crowd.data.ManagerCaller;
import com.threerings.msoy.avrg.client.ThaneAVRGameController;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.dobj.SubscriberAdapter;
import com.threerings.presents.util.SafeSubscriber;
import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.StringUtil;
import com.whirled.bureau.client.UserCode;
import com.threerings.util.MethodQueue;

/** The container for a user's avr game control code. */
public class AVRGameAgent extends Agent
{
    public function AVRGameAgent (ctx :MsoyBureauContext)
    {
        _ctx = ctx;
    }

    // from Agent
    public override function start () :void
    {
        Log.info("Starting agent " + _agentObj);

        // TODO: remove
        MethodQueue.callLater(function () :void {
            trace("Successful MethodQueue call");
        });

        // subscribe to the game object
        var delegator :Subscriber = 
            new SubscriberAdapter(objectAvailable, requestFailed);

        Log.info("Subscribing to game object " + gameAgentObj.gameOid);

        _subscriber = new SafeSubscriber(gameAgentObj.gameOid, delegator);
        _subscriber.subscribe(_ctx.getDObjectManager());

        // download the code
        _ctx.getUserCodeLoader().load(_agentObj.code, _agentObj.className, gotUserCode);
        
        var iter :Iterator = gameAgentObj.scenes.iterator();
        while (iter.hasNext()) {
            openScene(iter.next() as SceneInfo);
        }

        gameAgentObj.addListener(new SetAdapter(entryAdded, entryUpdated, entryRemoved));
    }

    // from Agent
    public override function stop () :void
    {
        Log.info("Stopping agent " + _agentObj);

        for each (var scene :Scene in _scenes.values()) {
            closeScene(scene.info);
        }

        _subscriber.unsubscribe(_ctx.getDObjectManager());
        _subscriber = null;
        _gameObj = null;
        _agentObj = null;

        if (_controller != null) {
            _controller.shutdown();
            _controller = null;
        }

        if (_userCode != null) {
            _userCode.release();
            _userCode = null;
        }
    }

    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == AVRGameAgentObject.SCENES) {
            openScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == AVRGameAgentObject.SCENES) {
            closeScene(event.getOldEntry() as SceneInfo);
            openScene(event.getEntry() as SceneInfo);
        }
    }

    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == AVRGameAgentObject.SCENES) {
            closeScene(event.getOldEntry() as SceneInfo);
        }
    }

    protected function openScene (sceneInfo :SceneInfo) :void
    {
        var scene :Scene = _scenes.get(sceneInfo.sceneId) as Scene;
        if (scene != null) {
            Log.warning("Request to reopen a scene " + sceneInfo);
            return;
        }

        _scenes.put(sceneInfo.sceneId, scene = new Scene());
        scene.info = sceneInfo;

        function gotWindow (wnd :Window) :void {
            //var service :ThaneWorldService = 
            //    wnd.requireService(ThaneWorldService) as ThaneWorldService;
            if (_scenes.get(sceneInfo.sceneId) == scene) {
                Log.info("Opened window to " + sceneInfo);
                scene.window = wnd;

            } else {
                Log.warning("Window opened after closure of " + sceneInfo);
                _ctx.getWindowDirector().closeWindow(wnd);
            }
        }

        function failed (cause :String) :void {
            Log.warning("Failed to open " + sceneInfo);
        }

        if (_ctx.getWindowDirector() != null) {
            Log.info("Attempting to open " + sceneInfo);
            _ctx.getWindowDirector().openWindow(
                sceneInfo.hostname, sceneInfo.port, new ResultAdapter(failed, gotWindow));

        } else {
            Log.info("Would open " + sceneInfo);
        }
    }

    protected function closeScene (sceneInfo :SceneInfo) :void
    {
        var scene :Scene = _scenes.get(sceneInfo.sceneId) as Scene;
        if (scene == null) {
            Log.warning("Closing unopened scene " + sceneInfo);
            return;
        }

        if (scene.window == null) {
            Log.warning("No window for closing scene " + sceneInfo);

        } else {
            Log.warning("Closing window for scene " + sceneInfo);
            _ctx.getWindowDirector().closeWindow(scene.window);
            scene.window = null;
        }

        _scenes.remove(sceneInfo.sceneId);
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
        Log.info("Subscribed to game object " + gameObj);
        _gameObj = gameObj;

        _controller = createController();
        _controller.init(_ctx, _gameObj, gameAgentObj);

        if (_userCode != null && _gameObj != null) {
            launchUserCode();
        }
    }

    /**
     * Callback for when the a request to subscribe to the game object fails.
     */
    protected function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        Log.warning("Could not subscribe to game object [oid=" + oid + "]");
        Log.logStackTrace(cause);
    }

    /**
     * Callback for when the user code is available.
     */
    protected function gotUserCode (userCode :UserCode) :void
    {
        if (userCode == null) {
            Log.warning("Unable to load user code [agent: " + _agentObj + "]");
            return;
        }

        _userCode = userCode;
        Log.info("Loaded user code " + _userCode);

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
            Log.info("Could not connect to user code");
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
        // TODO: call the trace method on the server, the manager caller technique only works if 
        // the game is a place obj and the server-side manager is a place manager etc.
        //_manager.invoke("agentTrace", trace);
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
    protected var _scenes :HashMap = new HashMap();
}

}

import com.threerings.msoy.avrg.data.SceneInfo;
import com.threerings.msoy.bureau.client.Window;

class Scene
{
    public var info :SceneInfo;
    public var window :Window;
}
