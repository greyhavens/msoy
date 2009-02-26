// $Id: DataSkippingTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.google.common.base.Preconditions;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.util.ThreadHelper.SafeSimpleDateFormatProvider;
import com.threerings.panopticon.reporter.aggregator.PropertiesResultTransformer;

/**
 * Skips results that fall within specific date ranges.
 * This can be used to take out booched data points.
 *
 * <p> Ranges are specified as intervals of type [start, end), represented as two arrays
 * of dates with an equal number of elements. Start and end dates should be in the ISO-8601
 * extended format, eg. "2000-01-01", with assumed local time zone.
 *
 * <p> The following configuration properties must be specified:
 *
 * <ul>
 *      <li><b>timestampField</b>: Field(s) to use for timestamps matching (eg. "date").
 *          Supported field types: Date, which will be used as-is; a long integer,
 *          which will be used as a milliseconds-since-epoch value in UTC; a Number,
 *          which will be cast to a long.</li>
 *      <li><b>startTimes</b>: List of beginnings of dates.</li>
 *      <li><b>endTimes</b>: List of ends of dates.</li>
 * </ul>
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class DataSkippingTransformer
    implements PropertiesResultTransformer
{
    public void configure(final Configuration config) throws ConfigurationException
    {
        this.timestampField = config.getString("timestampField");
        String[] startTimeStrings = config.getStringArray("startTimes");
        String[] endTimeStrings = config.getStringArray("endTimes");

        Preconditions.checkNotNull(
            this.timestampField, "Missing property: timestampField");
        Preconditions.checkArgument(
            startTimeStrings.length > 0 && startTimeStrings.length == endTimeStrings.length,
            "Invalid start/end time property values!");

        int count = startTimeStrings.length;
        DateFormat format = formatProvider.get();

        this.startTimes = new long[count];
        this.endTimes = new long[count];
        try {
            for (int ii = 0; ii < startTimeStrings.length; ii++) {
                this.startTimes[ii] = format.parse(startTimeStrings[ii]).getTime();
                this.endTimes[ii] = format.parse(endTimeStrings[ii]).getTime();
            }
        } catch (ParseException pe) {
            throw new ConfigurationException("Failed to parse start/end time string", pe);
        }
    }

    public boolean transform (EventData data)
    {
        long timestamp = 0L;
        Object dateObject = data.get(timestampField);
        if (dateObject instanceof Number) {
            timestamp = ((Number) dateObject).longValue();
        } else if (dateObject instanceof Date) {
            timestamp = ((Date) dateObject).getTime();
        } else {
            throw new IllegalStateException(
                "Unsupported date object type: " + dateObject.getClass());
        }

        for (int ii = 0; ii < this.startTimes.length; ii++) {
            if (startTimes[ii] <= timestamp && timestamp < endTimes[ii]) {
                return false; // skip this one
            }
        }

        return true;
    }

    private String timestampField;
    private long[] startTimes, endTimes;

    private static final SafeSimpleDateFormatProvider formatProvider =
        new SafeSimpleDateFormatProvider("yyyy-MM-dd");
}
