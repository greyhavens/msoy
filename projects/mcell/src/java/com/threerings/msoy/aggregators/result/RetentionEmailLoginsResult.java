package com.threerings.msoy.aggregators.result;

import java.util.Set;

import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;



@StringInputNameResult(inputs="Login")
public class RetentionEmailLoginsResult extends FieldAggregatedResult
{
    /** The member ids who have logged in. */
    public Set<Integer> memberIds = Sets.newHashSet();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailLoginsResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (EventData eventData)
    {
        memberIds.add(eventData.getInt("memberId"));
    }
}
