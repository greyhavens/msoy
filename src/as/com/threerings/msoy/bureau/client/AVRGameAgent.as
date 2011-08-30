//
// $Id$

package com.threerings.msoy.bureau.client {

import com.whirled.bureau.client.BaseGameAgent;
import com.whirled.bureau.client.GameAgentController;

import com.threerings.msoy.avrg.client.ThaneAVRGameController;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.bureau.util.MsoyBureauContext;

/** The container for a user's avr game control code. */
public class AVRGameAgent extends BaseGameAgent
{
    public function AVRGameAgent (ctx :MsoyBureauContext)
    {
        super(ctx);
    }

    /** Access the agent object, casted to a game agent object. */
    protected function get gameAgentObj () :AVRGameAgentObject
    {
        return _agentObj as AVRGameAgentObject;
    }

    /** @inheritDoc */
    // from BaseGameAgent
    override protected function getGameOid () :int
    {
        return gameAgentObj.gameOid;
    }

    /** @inheritDoc */
    // from BaseGameAgent
    override protected function createController () :GameAgentController
    {
        var ctrl :ThaneAVRGameController = new ThaneAVRGameController();
        ctrl.init(_ctx as MsoyBureauContext, _gameObj as AVRGameObject, this, gameAgentObj);
        return ctrl;
    }
}
}
