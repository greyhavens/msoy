//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.chat.server.ChatChannelManager;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Performs chat channel subscriptions across peers in a cluster configuration.
 * Proxy peers cannot modify channels hosted on another machine; instead, they should
 * use this service to request modifications on the hosting peer.
 */
public interface PeerChatService extends InvocationService
{
    /**
     * Asks the host to add a new user to the channel.
     */
    public void addUser (Client client, ChatterInfo chatter, ChatChannel channel,
                         PeerChatService.ConfirmListener listener);

    /**
     * Asks the host to remove an existing user from the channel.
     */
    public void removeUser (Client client, ChatterInfo chatter, ChatChannel channel,
                            PeerChatService.ConfirmListener listener);
}
