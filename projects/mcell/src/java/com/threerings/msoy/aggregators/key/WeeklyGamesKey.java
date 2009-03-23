// $Id: WeeklyGamesKey.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.key;

import java.util.Date;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.panopticon.aggregator.key.PropertiesAggregatorKey;
import com.threerings.panopticon.aggregator.writable.MultiKeys;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.shared.util.PartialDateType;
import com.threerings.panopticon.shared.util.TimeRange;

public class WeeklyGamesKey
    implements PropertiesAggregatorKey<MultiKeys.DateIntegerKey>
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        // Nothing needed
    }

    public MultiKeys.DateIntegerKey create (final EventData eventData)
    {
        final Object timestamp = eventData.getData().get("timestamp");
        final long time = (timestamp instanceof Date) ? ((Date)timestamp).getTime()
            : (Long)timestamp;
        final Date date = TimeRange.roundDown(time, PartialDateType.WEEK).getTime();

        final int bucket = Math.abs(((Number)eventData.getData().get("gameId")).intValue());
        return new MultiKeys.DateIntegerKey(date, bucket);
    }

    public Class<MultiKeys.DateIntegerKey> getKeyClass ()
    {
        return MultiKeys.DateIntegerKey.class;
    }

    public void getOutputValues (final MultiKeys.DateIntegerKey key, final Map<String, Object> data)
    {
        data.put("date", key.getFirstKey());
        data.put("gameId", key.getSecondKey());
    }
}
