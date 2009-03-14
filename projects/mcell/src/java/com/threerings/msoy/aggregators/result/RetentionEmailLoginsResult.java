package com.threerings.msoy.aggregators.result;

import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.msoy.aggregators.RetentionEmail;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.Result;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;

@Result(inputs="Login")
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

        // we don't care about logins more than 14 days ago
        return RetentionEmail.isRecentEnough(eventData, 14);
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
