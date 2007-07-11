//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MsoyCodes;

/**
 * Does something extraordinary.
 */
public class NotifyMessage extends ChatMessage
{
    public function NotifyMessage (msg :String = null)
    {
        if (msg != null) {
            super(msg, MsoyCodes.NOTIFY_MSGS);
        }
    }
}
}
