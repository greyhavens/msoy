package com.threerings.msoy.aggregators.result;

import java.util.Set;

import com.google.common.collect.Sets;

import com.threerings.panopticon.common.event.EventData;

import com.threerings.panopticon.reporter.aggregator.result.Result;

import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;

@Result(inputs="Login")
public class RetentionEmailLoginsResult extends FieldAggregatedResult
{
    /** The member ids who have logged in. */
    public Set<Integer> logins = Sets.newHashSet();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailLoginsResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        logins.add(eventData.getInt("memberId"));
    }
}
