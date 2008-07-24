//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.bureau.client.Agent;
import com.threerings.bureau.data.AgentObject;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.whirled.bureau.client.WhirledBureauDirector;
import com.whirled.bureau.data.GameAgentObject;
import com.whirled.bureau.util.WhirledBureauContext;

/** Msoy-specific bureau director. */
public class MsoyBureauDirector extends WhirledBureauDirector
{
    /** Creates a new director. */
    public function MsoyBureauDirector (ctx :WhirledBureauContext)
    {
        super(ctx);
    }

    /** @inheritDoc */
    // from BureauDirector
    protected override function createAgent (agentObj :AgentObject) :Agent
    {
        if (agentObj is GameAgentObject) {
            // Create the msoy subclass
            return new MsoyGameAgent(_ctx as WhirledBureauContext);

        } else if (agentObj is AVRGameAgentObject) {
            // Create the AVR agent
            return new AVRGameAgent(_ctx as WhirledBureauContext);
        }

        throw new Error("Unknown type");
    }
}

}
