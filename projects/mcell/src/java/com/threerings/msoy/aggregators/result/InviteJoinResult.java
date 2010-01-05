// $Id: InviteJoinResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.Lists;

import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class InviteJoinResult extends FieldAggregatedResult<WritableComparable<?>>
{
    public List<Date> timestamps = Lists.newArrayList();
    public List<Integer> inviterId = Lists.newArrayList();
    public boolean viewed, accepted;

    @Override
    protected void doInit (WritableComparable<?> key, EventData data)
    {
        if (data.getEventName().getFullName().equals("InviteSent")) {
            Object timestamp = data.getData().get("timestamp");
            timestamps.add((timestamp instanceof Date) ? (Date)timestamp : new Date(
                (Long)timestamp));
            inviterId.add(((Number)data.getData().get("inviterId")).intValue());
        } else if (data.getEventName().getFullName().equals("InviteViewed")) {
            viewed = true;
        } else {
            accepted = true;
        }
    }

    @Override
    public boolean putData (Map<String, Object> result)
    {
        // Don't try to do anything with the data if we have no invitation sent.
        if (timestamps.isEmpty()) {
            return false;
        }

        result.put("dateSent", timestamps.get(0));
        result.put("inviterId", inviterId.get(0));
        result.put("followed", viewed);
        result.put("accepted", accepted);
        return false;
    }
}
