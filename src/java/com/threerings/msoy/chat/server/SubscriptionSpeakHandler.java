//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.presents.data.ClientObject;

import static com.threerings.msoy.Log.log;

/**
 */
public class SubscriptionSpeakHandler extends ChannelSpeakHandler
{
    public SubscriptionSpeakHandler (SubscriptionWrapper wrapper)
    {
        super(wrapper);
    }

    // from interface SpeakProvider
    public void speak (ClientObject caller, String message, byte mode)
    {
        if (validateSpeaker(caller, mode)) {
            ChatterInfo chatter = new ChatterInfo((MemberObject)caller);
            ChatChannel channel = _ch.getChannel();
            MsoyNodeObject host = MsoyServer.peerMan.getChannelHost(channel);
            host.peerChatService.forwardSpeak(
                MsoyServer.peerMan.getPeerClient(host.nodeName),
                chatter, channel, message, mode, new ReportListener(chatter));
        }
    }
}

