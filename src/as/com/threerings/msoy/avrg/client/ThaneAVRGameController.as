// 
// $Id$

package com.threerings.msoy.avrg.client {

import com.whirled.bureau.util.WhirledBureauContext;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;

public class ThaneAVRGameController
{
    public function init (
        ctx :WhirledBureauContext, gameObj :AVRGameObject, gameAgentObj :AVRGameAgentObject) :void
    {
        _backend = new ThaneAVRGameBackend();
    }

    public function shutdown () :void
    {
    }

    public function get backend () :ThaneAVRGameBackend
    {
        return _backend;
    }

    public function agentReady () :void
    {
        // TODO: let the server know we are ready to rock n' roll
    }

    protected var _backend :ThaneAVRGameBackend;
}

}
