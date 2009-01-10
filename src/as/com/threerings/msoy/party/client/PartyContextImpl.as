//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.party.data.PartierObject;
import com.threerings.msoy.party.data.PartyCredentials;

/**
 * Provides an implementation of the PartyContext.
 */
public class PartyContextImpl implements PartyContext
{
    public function PartyContextImpl (wctx :WorldContext)
    {
        _wctx = wctx;
        _client = new Client(null);
    }

    /**
     * Configures our client with the supplied party hostname and port and logs on.
     */
    public function connect (partyId :int, hostname :String, port :int) :void
    {
        // if we are a guest and have an assigned member name, pass it along to the game server so
        // that it will show us the same guest name that we had on the server
        var name :MemberName = _wctx.getMyName();
        if (name != null && !name.isGuest()) {
            name = null;
        }
        var pcreds :PartyCredentials = new PartyCredentials(name);
        pcreds.sessionToken = (_wctx.getClient().getCredentials() as MsoyCredentials).sessionToken;
        pcreds.partyId = partyId;

        // configure our client and logon
        _client.addServiceGroup(MsoyCodes.PARTY_GROUP);
        _client.setVersion(DeploymentConfig.version);
        _client.setServer(hostname, [ port ]);
        _client.setCredentials(pcreds);
        _client.logon();
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

    // from PartyContext
    public function getMsoyContext () :MsoyContext
    {
        return _wctx;
    }

    // from PartyContext
    public function getPartierObject () :PartierObject
    {
        return (_client.getClientObject() as PartierObject);
    }

    protected var _wctx :WorldContext;
    protected var _client :Client;
}
}
