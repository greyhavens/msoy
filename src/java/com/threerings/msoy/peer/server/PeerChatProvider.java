//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;
import com.threerings.util.Name;

/**
 * Defines the server-side of the {@link PeerChatService}.
 */
public interface PeerChatProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerChatService#addUser} request.
     */
    void addUser (ClientObject caller, VizMemberName arg1, ChatChannel arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PeerChatService#forwardSpeak} request.
     */
    void forwardSpeak (ClientObject caller, Name arg1, ChatChannel arg2, String arg3, byte arg4, InvocationService.ConfirmListener arg5)
        throws InvocationException;

    /**
     * Handles a {@link PeerChatService#removeUser} request.
     */
    void removeUser (ClientObject caller, VizMemberName arg1, ChatChannel arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link PeerChatService#updateChannel} request.
     */
    void updateChannel (ClientObject caller, ChatChannel arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link PeerChatService#updateUser} request.
     */
    void updateUser (ClientObject caller, VizMemberName arg1, ChatChannel arg2, InvocationService.InvocationListener arg3)
        throws InvocationException;
}
