//
// $Id$

package com.threerings.msoy.party.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface PartyService extends InvocationService
{
    /** Requests to leave the party. */
    void leaveParty (Client client, ConfirmListener cl);

    /** Requests to boot a player from the party. */
    void bootMember (Client client, int memberId, ConfirmListener cl);

    /** Requests to reassign leadership to another player. */
    void assignLeader (Client client, int memberId, ConfirmListener cl);
}
