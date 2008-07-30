//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.peer.data.PeerChatMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.util.Name;

/**
 * Dispatches requests to the {@link PeerChatProvider}.
 */
public class PeerChatDispatcher extends InvocationDispatcher<PeerChatMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerChatDispatcher (PeerChatProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public PeerChatMarshaller createMarshaller ()
    {
        return new PeerChatMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerChatMarshaller.ADD_USER:
            ((PeerChatProvider)provider).addUser(
                source, (VizMemberName)args[0], (ChatChannel)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case PeerChatMarshaller.FORWARD_SPEAK:
            ((PeerChatProvider)provider).forwardSpeak(
                source, (Name)args[0], (ChatChannel)args[1], (String)args[2], ((Byte)args[3]).byteValue(), (InvocationService.ConfirmListener)args[4]
            );
            return;

        case PeerChatMarshaller.REMOVE_USER:
            ((PeerChatProvider)provider).removeUser(
                source, (VizMemberName)args[0], (ChatChannel)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case PeerChatMarshaller.UPDATE_CHANNEL:
            ((PeerChatProvider)provider).updateChannel(
                source, (ChatChannel)args[0], (InvocationService.InvocationListener)args[1]
            );
            return;

        case PeerChatMarshaller.UPDATE_USER:
            ((PeerChatProvider)provider).updateUser(
                source, (VizMemberName)args[0], (ChatChannel)args[1], (InvocationService.InvocationListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
