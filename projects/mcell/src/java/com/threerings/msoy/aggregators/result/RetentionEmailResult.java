//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.msoy.aggregators.RetentionEmail;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.Result;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;

/**
 * Counts up all the members who were sent a retention email and outputs a map of the member id
 * to the date the email was sent.
 */
@Result(inputs="RetentionMailSent") // original msoy event
public class RetentionEmailResult extends FieldAggregatedResult
{
    /** The members addressed on a given date. */
    public Map<Long, List<Integer>> sent = Maps.newHashMap();

    @Override // from FieldResult
    public boolean shouldInit (EventData eventData)
    {
        if (!super.shouldInit(eventData)) {
            return false;
        }

        // TODO: rolling incrementals - we need to analyze and adjust all data in our window, but
        // leave previous data untouched - just do 4 weeks for now
        return RetentionEmail.isRecentEnough(eventData, 28);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        sent.put(RetentionEmail.dayOfEvent(eventData),
            Collections.singletonList(eventData.getInt("recipientId")));
    }
}
