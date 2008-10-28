//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.peer.client.MsoyPeerService;
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
    void forwardMemberObject (ClientObject caller, MemberObject arg1, Streamable[] arg2);
}
