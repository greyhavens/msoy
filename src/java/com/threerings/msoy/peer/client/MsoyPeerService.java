//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.io.Streamable;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * A service implemented by MetaSOY peer nodes.
 */
public interface MsoyPeerService extends InvocationService<ClientObject>
{
    /**
     * Forwards a resolved member object to a server to which the member is about to connect.
     */
    void forwardMemberObject (MemberObject memobj, Streamable[] locals);

    /**
     * Forwards a request to reclaim an item from out of a room.
     */
    void reclaimItem (
        int sceneId, int memberId, ItemIdent item, ConfirmListener listener);

    /**
     * Forwards a request to transfer room ownership.
     */
    void transferRoomOwnership (
        int sceneId, byte ownerType, int ownerId, Name ownerName,
        boolean lockToOwner, ConfirmListener listener);
}
