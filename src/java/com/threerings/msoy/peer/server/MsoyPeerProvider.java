//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.peer.client.MsoyPeerService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoyPeerService}.
 */
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
}
