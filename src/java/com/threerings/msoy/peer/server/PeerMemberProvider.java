//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.peer.client.PeerMemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerMemberService}.
 */
public interface PeerMemberProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerMemberService#flowUpdated} request.
     */
    public void flowUpdated (ClientObject caller, int arg1, int arg2, int arg3);

    /**
     * Handles a {@link PeerMemberService#reportUnreadMail} request.
     */
    public void reportUnreadMail (ClientObject caller, int arg1, int arg2);
}
