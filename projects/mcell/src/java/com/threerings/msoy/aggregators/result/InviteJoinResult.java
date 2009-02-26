// $Id: InviteJoinResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Date;
import java.util.Map;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;
import com.threerings.panopticon.reporter.aggregator.result.JoinResult;

public class InviteJoinResult extends JoinResult
{
    public boolean putData (final Map<String, Object> result)
    {
        final EventData inviteSent = get(new EventName("InviteSent"));
        final EventData inviteViewed = get(new EventName("InviteViewed"));
        final EventData accountCreated = get(new EventName("AccountCreated"));

        // Don't try to do anything with the data if we have no invitation sent.
        if (inviteSent == null) {
            return false;
        }

        final Object timestamp = inviteSent.getData().get("timestamp");
        result.put("dateSent", (timestamp instanceof Date) ? (Date)timestamp : new Date(
            (Long)timestamp));
        result.put("inviterId", ((Number)inviteSent.getData().get("inviterId")).intValue());
        result.put("followed", inviteViewed != null);
        result.put("accepted", accountCreated != null);
        return false;
    }
}
