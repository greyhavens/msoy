//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.msoy.game.client.MsoyThaneGameController;
import com.whirled.bureau.client.GameAgent;
import com.whirled.bureau.util.WhirledBureauContext;
import com.whirled.game.client.ThaneGameController;

/** Msoy-specific game agent. */
public class MsoyGameAgent extends GameAgent
{
    /** Creates a new agent */
    public function MsoyGameAgent (
        ctx :WhirledBureauContext)
    {
        super(ctx);
    }

    /** @inheritDoc */
    // from GameAgent
    override protected function createController () :ThaneGameController
    {
        // create the msoy subclass
        return new MsoyThaneGameController();
    }
}

}
