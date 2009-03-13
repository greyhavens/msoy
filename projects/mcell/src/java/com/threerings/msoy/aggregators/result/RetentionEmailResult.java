//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.Result;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;

/**
 * Counts up all the members who were sent a retention email and outputs a map of the member id
 * to the date the email was sent.
 */
@Result(inputs="RetentionMailSent")
public class RetentionEmailResult extends FieldAggregatedResult
{
    /** The timestamp of the most recently sent email for each member id. */
    public HashMap<Integer, Long> sent = Maps.newHashMap();

    /** The total number of messages send on a given date. */
    public HashMap<Long, Integer> counts = Maps.newHashMap();

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(eventData.getDate("timestamp"));
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Long key = calendar.getTimeInMillis();

        Integer memberId = eventData.getInt("recipientId");
        sent.put(memberId, key);

        Integer count = counts.get(key);
        counts.put(key, count == null ? 1 : count + 1);
    }

    @Override // from AggregatedValue
    public void combine (FieldAggregatedResult other)
    {
        RetentionEmailResult value = (RetentionEmailResult)other;

        // take the most recent date for each member
        for (Map.Entry<Integer, Long> entry : value.sent.entrySet()) {
            Long mine = sent.get(entry.getKey());
            if (mine == null || entry.getValue() > mine) {
                sent.put(entry.getKey(), entry.getValue());
            }
        }

        // add counts together
        for (Map.Entry<Long, Integer> entry : value.counts.entrySet()) {
            Integer mine = counts.get(entry.getKey());
            counts.put(entry.getKey(), entry.getValue() + (mine == null ? 0 : mine));
        }
    }
}
