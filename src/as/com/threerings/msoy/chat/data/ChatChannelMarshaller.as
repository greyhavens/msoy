//
// $Id$

package com.threerings.msoy.chat.data {

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;

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
    public static const CREATE_CHANNEL :int = 1;

    // from interface ChatChannelService
    public function createChannel (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, CREATE_CHANNEL, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #inviteToChannel} requests. */
    public static const INVITE_TO_CHANNEL :int = 2;

    // from interface ChatChannelService
    public function inviteToChannel (arg1 :Client, arg2 :MemberName, arg3 :ChatChannel, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, INVITE_TO_CHANNEL, [
            arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch {@link #joinChannel} requests. */
    public static const JOIN_CHANNEL :int = 3;

    // from interface ChatChannelService
    public function joinChannel (arg1 :Client, arg2 :ChatChannel, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, JOIN_CHANNEL, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch {@link #leaveChannel} requests. */
    public static const LEAVE_CHANNEL :int = 4;

    // from interface ChatChannelService
    public function leaveChannel (arg1 :Client, arg2 :ChatChannel) :void
    {
        sendRequest(arg1, LEAVE_CHANNEL, [
            arg2
        ]);
    }
}
}
