//
// $Id$

package com.threerings.msoy.bureau.client {

import com.threerings.bureau.client.Agent;
import com.threerings.bureau.data.AgentObject;
import com.threerings.msoy.avrg.data.AVRGameAgentObject;
import com.threerings.msoy.bureau.util.MsoyBureauContext;
import com.whirled.bureau.client.WhirledBureauDirector;
import com.whirled.bureau.data.GameAgentObject;

/** Msoy-specific bureau director. */
public class MsoyBureauDirector extends WhirledBureauDirector
{
    /** Creates a new director. */
    public function MsoyBureauDirector (ctx :MsoyBureauContext)
    {
        super(ctx);
    }

    /** @inheritDoc */
    // from BureauDirector
    protected override function createAgent (agentObj :AgentObject) :Agent
    {
        if (agentObj is GameAgentObject) {
            // Create the msoy subclass
            return new MsoyGameAgent(_ctx as MsoyBureauContext);

        } else if (agentObj is AVRGameAgentObject) {
            // Create the AVR agent
            return new AVRGameAgent(_ctx as MsoyBureauContext);
        }

        throw new Error("Unknown type");
    }
}

}
