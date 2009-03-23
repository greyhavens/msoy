// $Id: ConversionFilter.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.panopticon.aggregator.PropertiesResultTransformer;
import com.threerings.panopticon.common.event.EventData;

/**
 * Used to filter out conversion noise from AB test and conversion tables.
 *
 * Given "converted" and "total" columns, removes all entries that only have a single
 * element, or whose conversion is zero.
 *
 * Required config options:
 * <ul>
 *      <li><b>convertedColumn</b>: name of the column with converted player count.</li>
 *      <li><b>totalColumn</b>: name of the column with total player count.</li>
 * </ul>
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class ConversionFilter implements PropertiesResultTransformer
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        this.convertedColumn = config.getString("convertedColumn");
        if (this.convertedColumn == null) {
            throw new ConfigurationException(
                "Aggregator transform property 'convertedColumn' is required.");
        }

        this.totalColumn = config.getString("totalColumn");
        if (this.totalColumn == null) {
            throw new ConfigurationException(
                "Aggregator transform property 'totalColumn' is required.");
        }
    }

    public boolean transform (EventData data)
    {
        final int converted = ((Number)data.get(convertedColumn)).intValue();
        final int total = ((Number)data.get(totalColumn)).intValue();

        return (converted > 0 && total > 1);
    }

    private String convertedColumn;
    private String totalColumn;
}
