//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelMarshaller;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link ChatChannelProvider}.
 */
public class ChatChannelDispatcher extends InvocationDispatcher<ChatChannelMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public ChatChannelDispatcher (ChatChannelProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public ChatChannelMarshaller createMarshaller ()
    {
        return new ChatChannelMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case ChatChannelMarshaller.CREATE_CHANNEL:
            ((ChatChannelProvider)provider).createChannel(
                source, (String)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case ChatChannelMarshaller.INVITE_TO_CHANNEL:
            ((ChatChannelProvider)provider).inviteToChannel(
                source, (MemberName)args[0], (ChatChannel)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case ChatChannelMarshaller.JOIN_CHANNEL:
            ((ChatChannelProvider)provider).joinChannel(
                source, (ChatChannel)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case ChatChannelMarshaller.LEAVE_CHANNEL:
            ((ChatChannelProvider)provider).leaveChannel(
                source, (ChatChannel)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
