// $Id: TruncateToIntervalTransformer.java 1377 2009-02-22 20:45:19Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.trans;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;

import com.threerings.panopticon.aggregator.PropertiesResultTransformer;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.util.DateFactory;
import com.threerings.panopticon.aggregator.util.PartialDate;

/**
 * Removes data before or after today (rounded down as desired).
 *
 * Optional config options:
 * <ul>
 *     <li><b>field</b>: name of the date field in each event; defaults to "date".</li>
 *     <li><b>interval</b>: how current time should be rounded down to yield comparison threshold;
 *         one of the {@link PartialDate} enums, defaults to DAY.</li>
 *     <li><b>test</b>: name of the test that checks whether each timestamp passes a comparison
 *         with the interval threshold; one of {@link IntervalCheck}, defaults to BEFORE.</li>
 *     <li><b>format</b>: date format string compatible with {@link SimpleDateFormat}, which will
 *         be used to decode string date fields; if absent, string dates will be ignored, and
 *         only Date dates will be processed.</li>
 * </ul>
 */
public class TruncateToIntervalTransformer
    implements PropertiesResultTransformer
{
    public static interface DateTest
    {
        public boolean check (Date timestamp, Date threshold);
    }

    public enum IntervalCheck
    {
        /** True for each timestamp that happens before the threshold. */
        BEFORE(new DateTest() {
            public boolean check (Date timestamp, Date threshold)
            {
                return timestamp.before(threshold);
            }
        }),

        /** True for each timestamp that happens after the threshold. */
        AFTER(new DateTest() {
            public boolean check (Date timestamp, Date threshold)
            {
                return timestamp.after(threshold);
            }
        });

        public final DateTest test;

        private IntervalCheck (DateTest test)
        {
            this.test = test;
        }
    }

    public void configure (final Configuration config)
        throws ConfigurationException
    {
        _interval = PartialDate.valueOf(
                config.getString("interval", PartialDate.DAY.name()));
        _intervalCheck = IntervalCheck.valueOf(
                config.getString("test", IntervalCheck.BEFORE.name()));
        _dateField = config.getString("field", "date");

        String formatString = config.getString("format");
        _format = (formatString != null) ? DateFactory.newSimpleDateFormat(formatString) : null;
    }

    public boolean transform (EventData data)
    {
        // get the timestamp the start of this interval.
        Date threshold = _interval.roundDown(System.currentTimeMillis()).getTime();

        // get the timestamp. any null timestamps (or those we can't convert) will skip this event
        Date timestamp = getTimestamp(data);
        if (timestamp == null) {
            return false;
        }

        // if the date in this data fails our check, remove this record.
        return _intervalCheck.test.check(timestamp, threshold);
    }

    private Date getTimestamp (EventData data)
    {
        // do we have the right field in the event?
        Object dateObj = data.get(_dateField);
        if (dateObj == null) {
            return null;
        }

        // if it's a date already, return it
        if (dateObj instanceof Date) {
            return (Date) dateObj;
        }

        // are we expected to convert non-date fields? if not, abort
        if (_format == null) {
            return null;
        }

        // try to convert it
        try {
            return _format.parse(dateObj.toString());
        } catch (ParseException pe) {
            return null;
        }
    }

    protected PartialDate _interval;
    protected IntervalCheck _intervalCheck;
    protected String _dateField;
    protected DateFormat _format;
}
