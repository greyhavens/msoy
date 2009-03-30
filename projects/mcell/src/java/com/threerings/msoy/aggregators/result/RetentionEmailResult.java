//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Set;

import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;



/**
 * Counts up all the members who were sent a retention email and outputs a map of the member id
 * to the date the email was sent.
 */
@StringInputNameResult(inputs="RetentionMailSent") // original msoy event
public class RetentionEmailResult extends FieldAggregatedResult
{
    /** The members addressed. */
    public Set<Integer> sent = Sets.newHashSet();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        sent.add(eventData.getInt("recipientId"));
    }
}
