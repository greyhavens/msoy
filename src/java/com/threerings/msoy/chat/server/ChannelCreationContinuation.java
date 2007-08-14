//
// $Id$

package com.threerings.msoy.chat.server;

import com.threerings.msoy.chat.client.ChatChannelService;
import com.threerings.msoy.chat.data.ChatChannel;
import com.threerings.msoy.chat.data.ChatChannelCodes;
import com.threerings.msoy.data.MemberObject;

import static com.threerings.msoy.Log.log;

/**
 * Represents the rest of processing to be performed after the channel was successfully
 * created, whether by hosting a brand new copy, or by subscribing to one already hosted somewhere.
 */
public abstract class ChannelCreationContinuation 
{
    public ChannelCreationContinuation (final MemberObject user, final ChatChannel channel,
                                        final ChatChannelService.ResultListener listener)
    {
        _user = user;
        _channel = channel;
        _listener = listener;
    }

    /** Called when channel creation succeeds, with a wrapper containing the new channel. */
    public abstract void creationSucceeded (ChannelWrapper channel);

    /** Called when channel creation fails. The string should be considered as advisory only,
     *  it will show up in debug output, but will not be forwarded to the client. */
    public void creationFailed (String cause)
    {
        log.warning("Chat channel finalizer failed [user=" + _user.who() +
                    ", channel=" + _channel + ", cause=" + cause + "].");
        _listener.requestFailed(ChatChannelCodes.E_INTERNAL_ERROR);
    }

    /** User subscribing to the channel. */
    protected MemberObject _user;
    /** Information about the channel being created. */
    protected ChatChannel _channel;
    /** Listener which needs to be notified after channel creation succeeds or fails. */
    protected ChatChannelService.ResultListener _listener;
};
