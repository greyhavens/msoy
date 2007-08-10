//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatterInfo;
import com.threerings.msoy.data.MemberObject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import static com.threerings.msoy.Log.log;

/**
 */
public class HostedSpeakHandler extends ChannelSpeakHandler
{
    public HostedSpeakHandler (HostedWrapper wrapper, ChatChannelManager mgr)
    {
        super(wrapper);
        _mgr = mgr;
    }

    // from interface SpeakProvider
    public void speak (ClientObject caller, String message, byte mode)
    {
        if (validateSpeaker(caller, mode)) {
            ChatterInfo chatter = new ChatterInfo((MemberObject)caller);
            ChatChannel channel = _ch.getChannel();
            try {
                _mgr.forwardSpeak((ClientObject)null, chatter, channel, message, mode,
                                  new ReportListener(chatter));
            } catch (InvocationException ex) {
                log.warning("Channel host failed during a chat speak [chatter=" + chatter +
                            ", channel=" + _ch.getChannel() + ", error=" + ex.getMessage() + "].");
            }
        }
    }

    /** Chat manager. */
    protected ChatChannelManager _mgr;
}

