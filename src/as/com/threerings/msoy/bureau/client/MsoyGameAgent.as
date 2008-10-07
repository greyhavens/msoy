//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.msoy.game.client.MsoyThaneGameController;
import com.whirled.bureau.client.WhirledGameAgent;
import com.whirled.bureau.client.GameAgentController;
import com.whirled.bureau.util.WhirledBureauContext;
import com.whirled.game.data.WhirledGameObject;

/** Msoy-specific game agent. */
public class MsoyGameAgent extends WhirledGameAgent
{
    /** Creates a new agent */
    public function MsoyGameAgent (
        ctx :WhirledBureauContext)
    {
        super(ctx);
    }

    /** @inheritDoc */
    // from WhirledGameAgent
    override protected function createController () :GameAgentController
    {
        // create the msoy subclass
        var ctrl :MsoyThaneGameController = new MsoyThaneGameController();
        ctrl.init(_ctx, _gameObj as WhirledGameObject, this, gameAgentObj.config);
        return ctrl;
    }
}

}
