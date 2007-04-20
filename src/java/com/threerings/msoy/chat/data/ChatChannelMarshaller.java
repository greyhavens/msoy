//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link ChatChannelService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ChatChannelMarshaller extends InvocationMarshaller
    implements ChatChannelService
{
    /** The method id used to dispatch {@link #createChannel} requests. */
    public static final int CREATE_CHANNEL = 1;

    // from interface ChatChannelService
    public void createChannel (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CREATE_CHANNEL, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #inviteToChannel} requests. */
    public static final int INVITE_TO_CHANNEL = 2;

    // from interface ChatChannelService
    public void inviteToChannel (Client arg1, MemberName arg2, ChatChannel arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, INVITE_TO_CHANNEL, new Object[] {
            arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #joinChannel} requests. */
    public static final int JOIN_CHANNEL = 3;

    // from interface ChatChannelService
    public void joinChannel (Client arg1, ChatChannel arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_CHANNEL, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #leaveChannel} requests. */
    public static final int LEAVE_CHANNEL = 4;

    // from interface ChatChannelService
    public void leaveChannel (Client arg1, ChatChannel arg2)
    {
        sendRequest(arg1, LEAVE_CHANNEL, new Object[] {
            arg2
        });
    }
}
