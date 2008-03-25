//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.util.Name;

/**
 * A custom chat message used on chat channels.
 */
public class ChannelMessage extends UserMessage
{
    /** Creation timestamp on this message (set by server hosting the channel). */
    public Long creationTime;

    /** The server stores which ChatChannel this message is for so it can be logged. */
    public transient ChatChannel channel;

    /** Constructor for deserialization. */
    public ChannelMessage ()
    {
    }

    /** Default constructor. */
    public ChannelMessage (Name speaker, String message, byte mode, ChatChannel channel)
    {
        super(speaker, null, message, mode);
        creationTime = System.currentTimeMillis();
        this.channel = channel;
    }
}
