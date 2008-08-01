// 
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;

import com.whirled.bureau.util.WhirledBureauContext;

public class ThaneAVRGameController
{
    public var log :Log = Log.getLog(this);

    public function init (
        ctx :WhirledBureauContext, gameObj :AVRGameObject, gameAgentObj :AVRGameAgentObject) :void
    {
        _gameObj = gameObj;
        _gameAgentObj = gameAgentObj;

        _backend = new ThaneAVRGameBackend();
    }

    /** Shuts down the AVRG controller. */
    public function shutdown () :void
    {
        backend.shutdown();

        _gameObj = null;
        _gameAgentObj = null;
    }

    public function get backend () :ThaneAVRGameBackend
    {
        return _backend;
    }

    public function agentReady () :void
    {
        log.info("Reporting agent ready " + _gameObj.which() + ".");
        _gameObj.manager.invoke("agentReady");
    }

    protected var _backend :ThaneAVRGameBackend;
    protected var _gameObj :AVRGameObject;
    protected var _gameAgentObj :AVRGameAgentObject;
}

}
