// $Id: TimeSpentTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import com.threerings.panopticon.aggregator.ResultTransformer;
import com.threerings.panopticon.common.event.EventData;

public class TimeSpentTransformer implements ResultTransformer
{
    public boolean transform (EventData data)
    {
        // Data is represented as seconds, convert to minutes and create averages
        double totalActiveMinutes = data.getDouble("totalActiveMinutes") / 60.0;
        double totalIdleMinutes = data.getDouble("totalIdleMinutes") / 60.0;
        int totalSessions = data.getInt("totalSessions");

        data.getData().put("totalActiveMinutes", totalActiveMinutes);
        data.getData().put("totalIdleMinutes", totalIdleMinutes);
        data.getData().put("averageActiveMinutes", totalActiveMinutes / totalSessions);
        data.getData().put("averageIdleMinutes", totalIdleMinutes / totalSessions);

        return true;
    }
}
