//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.peer.client.PeerChatService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PeerChatService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerChatMarshaller extends InvocationMarshaller
    implements PeerChatService
{
    /** The method id used to dispatch {@link #addUser} requests. */
    public static final int ADD_USER = 1;

    // from interface PeerChatService
    public void addUser (Client arg1, ChatterInfo arg2, ChatChannel arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, ADD_USER, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #forwardSpeak} requests. */
    public static final int FORWARD_SPEAK = 2;

    // from interface PeerChatService
    public void forwardSpeak (Client arg1, ChatterInfo arg2, ChatChannel arg3, String arg4, byte arg5, InvocationService.ConfirmListener arg6)
    {
        InvocationMarshaller.ConfirmMarshaller listener6 = new InvocationMarshaller.ConfirmMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, FORWARD_SPEAK, new Object[] {
            arg2, arg3, arg4, Byte.valueOf(arg5), listener6
        });
    }

    /** The method id used to dispatch {@link #removeUser} requests. */
    public static final int REMOVE_USER = 3;

    // from interface PeerChatService
    public void removeUser (Client arg1, ChatterInfo arg2, ChatChannel arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, REMOVE_USER, new Object[] {
            arg2, arg3, listener4
        });
    }
}
