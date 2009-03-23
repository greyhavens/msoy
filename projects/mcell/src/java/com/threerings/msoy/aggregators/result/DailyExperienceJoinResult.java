// $Id: DailyExperienceJoinResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Map;

import com.threerings.panopticon.aggregator.result.JoinResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;

/**
 * Joins two tables: one that counts up all unique trackers by day, and another that
 * counts up all unique trackers' experiences by day. After joining, we calculate
 * daily percentages for each experience count, divided by that day's tracker count.
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class DailyExperienceJoinResult extends JoinResult
{
    public boolean putData (final Map<String, Object> result)
    {
        final EventData totalEvent = get(new EventName("DailyExperienceUniqueTrackerCounts"));
        final EventData pivotEvent = get(new EventName("DailyExperienceTrackerPivot"));

        if (totalEvent == null || pivotEvent == null) {
            return false; // something's not right!
        }

        final int total = ((Number) totalEvent.getData().get("total")).intValue();
        result.put("total", total);

        for (Map.Entry<String, Object> pivotEntry : pivotEvent.getData().entrySet()) {
            final String column = pivotEntry.getKey();
            final Object value = pivotEntry.getValue();
            if (! column.equals("date") && ! column.equals("total")) {
                // copy this column over to the results
                result.put(column, value);
                // try to insert a percentage as well
                if (value instanceof Integer) {
                    final String percentColumn = "p_" + column;
                    final double percentValue = ((Integer) value).doubleValue() / total;
                    result.put(percentColumn, percentValue);
                }
            }
        }
        return false;
    }
}
