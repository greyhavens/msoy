//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.crowd.chat.data.ChatMessage;

/**
 * Does something extraordinary.
 */
public class NotifyMessage extends ChatMessage
{
    public NotifyMessage ()
    {
    }

    public NotifyMessage (String msg)
    {
        super(msg, "notify"); // TODO: constant for message bundle name
    }
}
