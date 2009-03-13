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
        _sent.put(memberId, key);

        Integer count = _counts.get(key);
        _counts.put(key, count == null ? 1 : count + 1);
    }

    @Override // from AggregatedResult
    public boolean putData (Map<String, Object> result)
    {
        result.put("dates_sent_by_member", _sent);
        result.put("sent_count_by_date", _counts);
        return false;
    }

    @Override // from AggregatedValue
    public void combine (FieldAggregatedResult other)
    {
        RetentionEmailResult value = (RetentionEmailResult)other;

        // take the most recent date for each member
        for (Map.Entry<Integer, Long> entry : value._sent.entrySet()) {
            Long mine = _sent.get(entry.getKey());
            if (mine == null || entry.getValue() > mine) {
                _sent.put(entry.getKey(), entry.getValue());
            }
        }

        // add counts together
        for (Map.Entry<Long, Integer> entry : value._counts.entrySet()) {
            Integer mine = _counts.get(entry.getKey());
            _counts.put(entry.getKey(), entry.getValue() + (mine == null ? 0 : mine));
        }
    }

    protected HashMap<Integer, Long> _sent = Maps.newHashMap();
    protected HashMap<Long, Integer> _counts = Maps.newHashMap();
}
