//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.io.Streamable;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * A service implemented by MetaSOY peer nodes.
 */
public interface MsoyPeerService extends InvocationService
{
    /**
     * Forwards a resolved member object to a server to which the member is about to connect.
     */
    void forwardMemberObject (Client client, MemberObject memobj, Streamable[] locals);

    /**
     * Forwards a request to reclaim an item from out of a room.
     */
    void reclaimItem (
        Client client, int sceneId, int memberId, ItemIdent item, ConfirmListener listener);
}
