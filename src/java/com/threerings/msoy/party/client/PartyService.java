//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyService extends InvocationService
{
    void startParty (Client client, String name, ResultListener rl);

    void joinParty (Client client, int partyId, ResultListener rl);

    void leaveParty (Client client, ConfirmListener cl);
}
