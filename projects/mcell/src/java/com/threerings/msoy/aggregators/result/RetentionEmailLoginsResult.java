package com.threerings.msoy.aggregators.result;

import java.util.Set;

import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;



@StringInputNameResult(inputs="Login")
public class RetentionEmailLoginsResult extends FieldAggregatedResult<WritableComparable<?>>
{
    /** The member ids who have logged in. */
    public Set<Integer> memberIds = Sets.newHashSet();

    public static boolean checkInputs (EventData eventData)
    {
        return checkInputs(RetentionEmailLoginsResult.class, eventData);
    }

    @Override // from FieldResult
    protected void doInit (WritableComparable<?> key, EventData eventData)
    {
        memberIds.add(eventData.getInt("memberId"));
    }
}
