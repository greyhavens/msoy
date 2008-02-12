//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.peer.client.MsoyPeerService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.stats.data.StatSet;

/**
 * Defines the server-side of the {@link MsoyPeerService}.
 */
public interface MsoyPeerProvider extends InvocationProvider
{
    /**
     * Handles a {@link MsoyPeerService#forwardMemberObject} request.
     */
    public void forwardMemberObject (ClientObject caller, MemberObject arg1, String arg2, StatSet arg3);
}
