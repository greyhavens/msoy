//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.MemberObject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link MsoyPeerService}.
 */
public interface MsoyPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyPeerService#forwardMemberObject} request.
     */
    public void forwardMemberObject (ClientObject caller, MemberObject arg1, String[] arg2, Object[] arg3);
}
