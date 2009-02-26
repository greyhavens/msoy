// $Id: CostInHundredsKey.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.key;

import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.key.PropertiesAggregatorKey;
import com.threerings.panopticon.reporter.aggregator.writable.Keys;

public class CostInHundredsKey
    implements PropertiesAggregatorKey<Keys.IntegerKey>
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        // Nothing needed
    }

    public Keys.IntegerKey create (final EventData eventData)
    {
        final int cost = ((Number)eventData.getData().get("flowCost")).intValue();
        final int range = ((Number)(Math.ceil(cost / 100.0) * 100.0)).intValue();

        return new Keys.IntegerKey(range);
    }

    public Class<Keys.IntegerKey> getKeyClass ()
    {
        return Keys.IntegerKey.class;
    }

    public void getOutputValues (final Keys.IntegerKey key, final Map<String, Object> data)
    {
        data.put("range", key.get());
    }

}
