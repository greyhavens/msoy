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
    void bootMember (Client client, int memberId, InvocationListener il);

    /** Requests to reassign leadership to another player. */
    void assignLeader (Client client, int memberId, InvocationListener il);

    /** Requests to update the party name. */
    void updateNameOrStatus (Client client, String s, boolean name, InvocationListener il);
}
