//
// $Id$

package com.threerings.msoy.peer.server;

import javax.annotation.Generated;

import com.threerings.io.Streamable;

import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.peer.client.MsoyPeerService;

/**
 * Defines the server-side of the {@link MsoyPeerService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from MsoyPeerService.java.")
public interface MsoyPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyPeerService#forwardMemberObject} request.
     */
    void forwardMemberObject (ClientObject caller, MemberObject arg1, Streamable[] arg2);

    /**
     * Handles a {@link MsoyPeerService#reclaimItem} request.
     */
    void reclaimItem (ClientObject caller, int arg1, int arg2, ItemIdent arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link MsoyPeerService#transferRoomOwnership} request.
     */
    void transferRoomOwnership (ClientObject caller, int arg1, byte arg2, int arg3, Name arg4, boolean arg5, InvocationService.ConfirmListener arg6)
        throws InvocationException;
}
