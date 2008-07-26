//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.VizMemberName;

import com.threerings.msoy.peer.data.MsoyNodeObject;
import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.chat.data.ChatChannel;

import static com.threerings.msoy.Log.log;

/**
 */
public class SubscriptionSpeakHandler extends ChannelSpeakHandler
{
    public SubscriptionSpeakHandler (MsoyPeerManager peerMan, SubscriptionWrapper wrapper)
    {
        super(wrapper);
        _peerMan = peerMan;
    }

    // from interface SpeakProvider
    public void speak (ClientObject caller, String message, byte mode)
    {
        if (validateSpeaker(caller, mode)) {
            VizMemberName chatter = ((MemberObject)caller).memberName;
            ChatChannel channel = _ch.getChannel();
            MsoyNodeObject host = _peerMan.getChannelHost(channel);
            if (host != null) {
                host.peerChatService.forwardSpeak(
                    _peerMan.getPeerClient(host.nodeName),
                    chatter, channel, message, mode, new ReportListener(chatter));
            } else {
                log.info("Dropping channel message, no host [speaker=" + chatter +
                         ", channel=" + channel + "].");
            }
        }
    }

    protected MsoyPeerManager _peerMan;
}
