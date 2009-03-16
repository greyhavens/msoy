//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.util.Set;

import com.google.common.collect.Sets;

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
