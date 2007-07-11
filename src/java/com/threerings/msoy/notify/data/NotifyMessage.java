//
// $Id$

package com.threerings.msoy.notify.data;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MsoyCodes;

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
        super(msg, MsoyCodes.NOTIFY_MSGS);
    }
}
