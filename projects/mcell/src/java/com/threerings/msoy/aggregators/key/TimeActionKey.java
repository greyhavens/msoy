// $Id: TimeActionKey.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.key;

import java.util.Date;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.key.PropertiesAggregatorKey;
import com.threerings.panopticon.reporter.aggregator.writable.MultiKeys;
import com.threerings.panopticon.shared.util.PartialDateType;
import com.threerings.panopticon.shared.util.TimeRange;

/**
 * Key that includes a date rounded down to a day, and another string column.
 * Both will be produced in the output.
 *
 * Optional parameters:
 * <ul>
 *   <li><b>timestampField</b>: name of the timestamp field, defaults to "timestamp".</li>
 *   <li><b>actionField</b>: name of the action field, defaults to "action".</li>
 *   <li><b>dateOutput</b>: name of the output field for the rounded date, defaults to "date".</li>
 *   <li><b>actionOutput</b>: name of the output field for the action, defaults to "action".</li>
 *   <li><b>rounding</b>: name of the rounding parameters, eg. "DAY" to round down to each day,
 *       "WEEK" to round down to the week. Must be one of the values of the
 *       {@link PartialDateType} enum. Default value is "DAY".
 * </ul>
 */
public class TimeActionKey
    implements PropertiesAggregatorKey<MultiKeys.DateStringKey>
{
    public void configure (final Configuration config)
        throws ConfigurationException
    {
        this.actionField = config.getString("actionField", "action");
        this.timestampField = config.getString("timestampField", "timestamp");

        this.actionOut = config.getString("actionOutput", "action");
        this.dateOut = config.getString("dateOutput", "date");

        this.rounding = PartialDateType.valueOf(config.getString("rounding", "DAY"));
    }

    public MultiKeys.DateStringKey create (final EventData eventData)
    {
        final String action = (String) eventData.getData().get(actionField);
        final Date time = (Date) eventData.getData().get(timestampField);
        final Date date = TimeRange.roundDown(time.getTime(), rounding).getTime();
        return new MultiKeys.DateStringKey(date, action);
    }

    public Class<MultiKeys.DateStringKey> getKeyClass ()
    {
        return MultiKeys.DateStringKey.class;
    }

    public void getOutputValues (final MultiKeys.DateStringKey key, final Map<String, Object> data)
    {
        data.put(dateOut, key.getFirstKey());
        data.put(actionOut, key.getSecondKey());
    }

    String actionField, timestampField;
    String actionOut, dateOut;
    PartialDateType rounding;
}
