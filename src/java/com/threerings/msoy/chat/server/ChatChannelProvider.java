//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ChatChannelService}.
 */
public interface ChatChannelProvider extends InvocationProvider
{
    /**
     * Handles a {@link ChatChannelService#createChannel} request.
     */
    public void createChannel (ClientObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ChatChannelService#inviteToChannel} request.
     */
    public void inviteToChannel (ClientObject caller, MemberName arg1, ChatChannel arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link ChatChannelService#joinChannel} request.
     */
    public void joinChannel (ClientObject caller, ChatChannel arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ChatChannelService#leaveChannel} request.
     */
    public void leaveChannel (ClientObject caller, ChatChannel arg1);
}
