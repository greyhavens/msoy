//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.crowd.chat.data.UserMessage;
import com.threerings.util.Name;

public class ChannelMessage extends UserMessage
{
    /** Creation timestamp on this message (set by server hosting the channel). */
    public Long creationTime;

    /** Constructor for deserialization. */
    public ChannelMessage ()
    {
    }

    /** Default constructor. */
    public ChannelMessage (Name speaker, String message, byte mode)
    {
        super(speaker, null, message, mode);
        creationTime = System.currentTimeMillis();
    }
}
