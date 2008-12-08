//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultAdapter;

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
        return new PartyBoardPanel(_mctx);

        PartyPanel; // Force linkage until we work this out
    }

    /**
     * Get the party board.
     */
    public function getPartyBoard (resultHandler :Function, query :String = null) :void
    {
        _pbsvc.getPartyBoard(_mctx.getClient(), query,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), resultHandler));
    }

    /**
     * Create a new party.
     */
    public function createParty (name :String, groupId :int) :void
    {
        _pbsvc.createParty(_mctx.getClient(), name, groupId,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), partyOidKnown));
    }

    /**
     * Join a party.
     */
    public function joinParty (id :int) :void
    {
        _pbsvc.joinParty(_mctx.getClient(), id,
            new ResultAdapter(_mctx.chatErrHandler(MsoyCodes.PARTY_MSGS), partyOidKnown));
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

    /**
     * A success handler for creating and joining parties.
     */
    protected function partyOidKnown (oid :int) :void
    {
        // TODO: subscribe to this party
        trace("We're in the party: the oid is : " + oid);
    }

    protected var _mctx :MsoyContext;

    protected var _pbsvc :PartyBoardService;

    protected var _partyObj :PartyObject; // TODO: Pull this out of a hat
}
}
