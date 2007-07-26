//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerChatService}.
 */
public interface PeerChatProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerChatService#addUser} request.
     */
    public void addUser (ClientObject caller, ChatterInfo arg1, ChatChannel arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PeerChatService#removeUser} request.
     */
    public void removeUser (ClientObject caller, ChatterInfo arg1, ChatChannel arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;
}
