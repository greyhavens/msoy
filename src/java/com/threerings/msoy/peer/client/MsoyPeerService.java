//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.stats.data.StatSet;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;

/**
 * A service implemented by MetaSOY peer nodes.
 */
public interface MsoyPeerService extends InvocationService
{
    /**
     * Forwards a resolved member object to a server to which the member is about to connect.
     */
    public void forwardMemberObject (
        Client client, MemberObject memobj, String actorState, StatSet stats);

    /**
     * Boot a member from the peer node.
     */
    public void forwardBootMember (Client client, MemberName target);
}
