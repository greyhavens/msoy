//
// $Id$

package com.threerings.msoy.chat.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Defintes chat channel related invocation services.
 */
public interface ChatChannelService extends InvocationService
{
    /**
     * Requests to join the specified channel. The oid of the channel chat object will be returned
     * if access is granted to the channel.
     */
    void joinChannel (Client client, ChatChannel channel, ResultListener listener);

    /**
     * Requests that the caller be removed from the specified chat channel. This does not actually
     * unsubscribe them from the chat channel object which means that a hacked client could "leave"
     * the channel and stick around listening to what's happening there, but solving that problem
     * is very complicated, particularly across server clusters. So we punt.
     */
    void leaveChannel (Client client, ChatChannel channel);

    /**
     * Invites the specified player to join the specified chat channel. If the invitation is
     * successfully delivered, confirmation is sent via the supplied listener.
     */
    public void inviteToChannel (Client client, MemberName invitee, ChatChannel channel,
                                 ConfirmListener listener);

    /**
     * Requests to create a private chat channel with the specified name. If the channel is
     * created, the oid of the channel chat object will be returned via the supplied listener.
     */
    void createChannel (Client client, String name, ResultListener listener);
}
