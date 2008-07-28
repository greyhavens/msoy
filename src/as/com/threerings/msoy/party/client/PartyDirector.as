//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SubscriberAdapter;

import com.threerings.crowd.client.LocationAdapter;

import com.threerings.msoy.chat.client.ReportingListener;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.party.data.PartyMarshaller;

/**
 * Handles client party management.
 */
public class PartyDirector extends BasicDirector
{
    public const log :Log = Log.getLog(PartyDirector);

    // Reference this class so it gets compiled in
    PartyMarshaller;

    public function PartyDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
    }

    public function joinParty () :void
    {
        // TODO: moar

        trace("Joining party...");
        _psvc.joinParty(_mctx.getClient(), 1, new ResultWrapper(joinPartyFailure, joinPartySuccess));
    }

    public function leaveParty () :void
    {
        // TODO: moar

        trace("Leaving party...");
        _psvc.leaveParty(_mctx.getClient(),  new ConfirmAdapter(leavePartyFailure, null)); // TODO
    }

    protected function joinPartySuccess (partyOid :int) :void
    {
        var success :Function = function (object :DObject) :void {
            trace("Suscription a go");
        };
        var failure :Function = function (oid :int, cause :ObjectAccessError) :void {
            log.warning("Party director unable to fetch party object [cause=" + cause + "].");
        };

        var client :Client = _mctx.getClient();
        client.getDObjectManager().subscribeToObject(partyOid, new SubscriberAdapter(success, failure));
    }

    protected function joinPartyFailure (cause :String) :void
    {
        log.warning("Failed to join party [cause=" + cause +"].");
    }

    protected function leavePartyFailure (cause :String) :void
    {
        log.warning("Failed to leave party [cause=" + cause +"].");
    }


    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        // TODO: There's an error in conjuring up the PartyService
        //_psvc = (client.requireService(PartyService) as PartyService);
    }

    protected var _mctx :MsoyContext;
    protected var _psvc :PartyService;
}
}
