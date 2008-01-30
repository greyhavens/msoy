//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.util.Name;

/**
 * Performs chat channel subscriptions across peers in a cluster configuration.
 * Proxy peers cannot modify channels hosted on another machine; instead, they should
 * use this service to request modifications on the hosting peer.
 */
public interface PeerChatService extends InvocationService
{
    /**
     * Asks the host to forward the specified chat message on the channel's speak object.
     */
    public void forwardSpeak (Client client, Name chatter, ChatChannel channel,
                              String message, byte mode, PeerChatService.ConfirmListener listener);
        
    /**
     * Asks the host to add a new user to the channel.
     */
    public void addUser (Client client, VizMemberName chatter, ChatChannel channel,
                         PeerChatService.ConfirmListener listener);

    /**
     * Asks the host to remove an existing user from the channel.
     */
    public void removeUser (Client client, VizMemberName chatter, ChatChannel channel,
                            PeerChatService.ConfirmListener listener);
}
