//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.crowd.chat.data.ChatMessage;

/**
 * Does something extraordinary.
 */
public class NotifyMessage extends ChatMessage
{
    public function NotifyMessage (msg :String = null)
    {
        if (msg != null) {
            super(msg, "notify"); // TODO: constant for bundle name
        }
    }
}
}
