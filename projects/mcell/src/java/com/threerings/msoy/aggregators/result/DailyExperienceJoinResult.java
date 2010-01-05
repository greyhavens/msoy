// $Id: DailyExperienceJoinResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Map;

import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.Maps;

import com.threerings.panopticon.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

/**
 * Joins two tables: one that counts up all unique trackers by day, and another that
 * counts up all unique trackers' experiences by day. After joining, we calculate
 * daily percentages for each experience count, divided by that day's tracker count.
 */
public class DailyExperienceJoinResult extends FieldAggregatedResult<WritableComparable<?>>
{
    public int total;

    public Map<String, Integer> actions = Maps.newHashMap();


    @Override
    protected void doInit (WritableComparable<?> key, EventData data)
    {
        if(data.getEventName().getFullName().equals("DailyExperienceUniqueTrackerCounts")) {
            total = ((Number) data.getData().get("total")).intValue();
        } else {
            for (Map.Entry<String, Object> entry : data.getData().entrySet()) {
                final String column = entry.getKey();
                final Object value = entry.getValue();
                if (!column.equals("date") && !column.equals("total")) {
                    // copy this column over to the results
                    actions.put(column, (Integer)value);
                }
            }
        }
    }

    @Override
    public boolean putData (final Map<String, Object> result)
    {
        if (total == 0 || actions.isEmpty()) {
            return false; // something's not right!
        }

        result.put("total", total);
        result.putAll(actions);
        for (Map.Entry<String, Integer> entry : actions.entrySet()) {
            result.put("p_" + entry.getKey(), entry.getValue().doubleValue() / total);
        }

        return false;
    }
}
