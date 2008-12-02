//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.msoy.client.MsoyContext;

/**
 * Manages party stuff on the client.
 */
public class PartyDirector extends BasicDirector
{
    public function PartyDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _pbsvc = (client.requireService(PartyBoardService) as PartyBoardService);
    }

    protected var _mctx :MsoyContext;

    protected var _pbsvc :PartyBoardService;
}
}
