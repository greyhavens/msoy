//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.party.data.PartyBoardMarshaller;
import com.threerings.msoy.party.data.PartyMarshaller;
import com.threerings.msoy.party.data.PartyObject;
import com.threerings.msoy.party.data.PartyCodes;

/**
 * Manages party stuff on the client.
 */
public class PartyDirector extends BasicDirector
{
    // reference the PartyBoardMarshaller class
    PartyBoardMarshaller;

    public function PartyDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    /**
     * Create either a party board popup, or a party popup if we're already in a party.
     */
    public function createAppropriatePartyPanel () :FloatingPanel
    {
        // TODO: if the user is already partying, return a board for their party
        return new PartyBoardPanel(_mctx, _pbsvc);

        PartyPanel; // Force linkage until we work this out
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);

        client.addServiceGroup(MsoyCodes.MEMBER_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _pbsvc = (client.requireService(PartyBoardService) as PartyBoardService);
    }

    protected var _mctx :MsoyContext;

    protected var _pbsvc :PartyBoardService;

    protected var _partyObj :PartyObject; // TODO: Pull this out of a hat
}
}
