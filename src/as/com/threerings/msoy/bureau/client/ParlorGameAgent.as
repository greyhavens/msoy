//
// $Id$

package com.threerings.msoy.bureau.client {

import com.whirled.bureau.client.GameAgentController;
import com.whirled.bureau.client.WhirledGameAgent;
import com.whirled.bureau.util.WhirledBureauContext;

import com.threerings.msoy.game.client.MsoyThaneGameController;
import com.threerings.msoy.game.data.ParlorGameObject;

/** Msoy-specific game agent. */
public class ParlorGameAgent extends WhirledGameAgent
{
    /** Creates a new agent */
    public function ParlorGameAgent (ctx :WhirledBureauContext)
    {
        super(ctx);
    }

    /** @inheritDoc */
    // from WhirledGameAgent
    override protected function createController () :GameAgentController
    {
        // create the msoy subclass
        var ctrl :MsoyThaneGameController = new MsoyThaneGameController();
        ctrl.init(_ctx, _gameObj as ParlorGameObject, this, gameAgentObj.config);
        return ctrl;
    }
}

}
