//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.msoy.world.client.WorldContext;

/**
 * Provides an implementation of the PartyContext.
 */
public class PartyContextImpl implements PartyContext
{
    public function PartyContextImpl (wctx :WorldContext, host :String, port :int)
    {
        _wctx = wctx;

//         // set up our credentials
//         var wcreds :MsoyCredentials = (wctx.getClient().getCredentials() as MsoyCredentials);
//         // if we are a guest and have an assigned member name, pass it along to the game server so
//         // that it will show us the same guest name that we had on the server
//         var name :MemberName = null;
//         if (_wctx.getMemberObject() != null && _wctx.getMemberObject().isGuest()) {
//             name = _wctx.getMemberObject().memberName;
//         }
//         var pcreds :MsoyGameCredentials = new MsoyGameCredentials(name);
//         pcreds.sessionToken = wcreds.sessionToken;
//         pcreds.visitorId = wcreds.visitorId;
//         _client = new Client(gcreds);
//         _client.addServiceGroup(MsoyCodes.PARTY_GROUP);
    }

    // from PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    // from PresentsContext
    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    protected var _wctx :WorldContext;
    protected var _client :Client;
}
}
