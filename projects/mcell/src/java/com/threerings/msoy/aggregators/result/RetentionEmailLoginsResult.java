package com.threerings.msoy.aggregators.result;

import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.msoy.aggregators.RetentionEmail;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.Result;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;

@Result(inputs="msoy.RetentionEmail,Login")
public class RetentionEmailLoginsResult extends FieldAggregatedResult
{
    /** The most recent login time for each member. */
    public Map<Integer, Long> logins = Maps.newHashMap();

    @Override // from FieldResult
    public boolean shouldInit (EventData eventData)
    {
        if (!super.shouldInit(eventData)) {
            return false;
        }

        String name = eventData.getEventName().getFullName();

        // TODO: rolling incrementals - we need to analyze and adjust all data in our window, but
        // leave previous data untouched - just use the last 14 days for now
        if (name.equals("Login") && !RetentionEmail.isRecentEnough(eventData, 14)) {
            return false;
        }

        return true;
    }

    @Override // from AggregatedValue
    public void combine (FieldAggregatedResult other)
    {
        RetentionEmailLoginsResult value = (RetentionEmailLoginsResult)other;

        // use the most recent login for each member
        for (Map.Entry<Integer, Long> entry : value.logins.entrySet()) {
            Long prior = logins.get(entry.getKey());
            if (prior == null || entry.getValue() > prior) {
                logins.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        logins.put(eventData.getInt("memberId"), eventData.getDate("timestamp").getTime());
    }
}
