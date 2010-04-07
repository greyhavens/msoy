// $Id: CohortKey.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.key;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.hadoop.io.WritableComparable;

import com.samskivert.util.Calendars;

import com.threerings.panopticon.aggregator.key.PropertiesAggregatorKey;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.aggregator.util.PartialDate;

public class CohortKey
    implements PropertiesAggregatorKey<CohortKey.Key>
{
    public static class Key implements WritableComparable<Key>
    {
        public Key (final Date date, final Date cohortDate)
        {
            this.date = date;
            this.cohortDate = cohortDate;
        }

        public Key ()
        {
            // Default, no-arg constructor
        }

        public void readFields (final DataInput in)
            throws IOException
        {
            date = new Date(in.readLong());
            cohortDate = new Date(in.readLong());
        }

        public void write (final DataOutput out)
            throws IOException
        {
            out.writeLong(date.getTime());
            out.writeLong(cohortDate.getTime());
        }

        public int compareTo (final Key o)
        {
            final Key other = o;
            if (other == null) {
                return 1;
            }
            int compare = this.date.compareTo(other.date);
            if (compare == 0) {
                compare = this.cohortDate.compareTo(other.cohortDate);
            }
            return compare;
        }

        public Date getDate ()
        {
            return date;
        }

        public Date getCohortDate ()
        {
            return cohortDate;
        }

        private Date date;
        private Date cohortDate;
    }

    public void configure (final Configuration config)
        throws ConfigurationException
    {
        // Nothing needed
    }

    public CohortKey.Key create (final EventData eventData)
    {
        final Object createdOn = eventData.getData().get("createdOn");
        if (!(createdOn instanceof Long)) {
            return null; // Ignore items without a "createdOn" field.
        }

        final Object timestamp = eventData.getData().get("timestamp");
        final long time = (timestamp instanceof Date) ? ((Date)timestamp).getTime()
            : (Long)timestamp;
        final Date date = PartialDate.WEEK.roundDown(time).getTime();
        Date cohortDate = PartialDate.WEEK.roundDown((Long)createdOn).getTime();
        if (cohortDate.before(startDate)) {
            cohortDate = startDate;
        }

        return new Key(date, cohortDate);
    }

    public Class<CohortKey.Key> getKeyClass ()
    {
        return Key.class;
    }

    public void getOutputValues (final CohortKey.Key key, final Map<String, Object> data)
    {
        data.put("date", key.getDate());
        data.put("cohort", key.getCohortDate());
    }

    static {
        Calendar c = Calendars.at(2008, Calendar.JANUARY, 1).asCalendar();
        startDate = PartialDate.WEEK.roundDown(c).getTime();
    }

    private static final Date startDate;
}
