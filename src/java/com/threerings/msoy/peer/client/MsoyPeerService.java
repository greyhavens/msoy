//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.io.Streamable;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.MemberObject;

/**
 * A service implemented by MetaSOY peer nodes.
 */
public interface MsoyPeerService extends InvocationService
{
    /**
     * Forwards a resolved member object to a server to which the member is about to connect.
     */
    public void forwardMemberObject (Client client, MemberObject memobj, Streamable[] locals);
}
