//
// $Id$

package com.threerings.msoy.notify.data {

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.data.ChatChannel;

/**
 * Does something extraordinary.
 */
public class NotifyMessage extends ChatMessage
{
    public static const LOGON_LOCALTYPE :String = "notificationLogon";
    public static const LOGOFF_LOCALTYPE :String = "notificationLogoff";

    public function NotifyMessage (msg :String = null)
    {
        if (msg != null) {
            super(msg, MsoyCodes.NOTIFY_MSGS);
        }
    }

    /**
     * Returns true if the content of this NotifyMessage is related to the passed in chat message 
     * localtype.
     */
    public function isRelated (relatedLocaltype :String) :Boolean 
    {
        // right now this is only pertinant to logon and logoff messages
        if (ChatChannel.typeOf(relatedLocaltype) != ChatChannel.MEMBER_CHANNEL) {
            return false;
        }

        var parts :Array = localtype != null ? localtype.split(":") : [];
        if (parts.length < 2) {
            return false;
        }

        // if this is a logon or logoff, and the member id matches that of the relatedLocaltype,
        // we have a winner
        if ((parts[0] == LOGON_LOCALTYPE || parts[0] == LOGOFF_LOCALTYPE) && 
            ChatChannel.infoOf(relatedLocaltype) == parts[1]) {
            return true;
        }

        return false;
    }
}
}
