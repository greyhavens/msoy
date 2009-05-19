// $Id: GuestExperienceResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.io.WritableComparable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.result.StringInputNameResult;
import com.threerings.panopticon.aggregator.result.field.FieldResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.shared.util.DateFactory;

/**
 * Extracts a table of guest experiences before conversion, per entry vector / conversion status.
 */
public abstract class GuestExperienceResult extends
        FieldResult<WritableComparable<?>, GuestExperienceResult>
{
    @StringInputNameResult(inputs="AllGuestBehavior")
    public static class Conversion extends GuestExperienceResult {
        @Override protected String getEventsField () {
            return "events_till_conversion";
        }
    };

    @StringInputNameResult(inputs="AllGuestBehavior")
    public static class Retention extends GuestExperienceResult {
        @Override protected String getEventsField () {
            return "events_till_retention";
        }
    };

    public String vector;
    public String status;

    /** Map from experience name to the number of people who participated in it. */
    public Map<String, Integer> participation = Maps.newHashMap();

    /** Set of all trackers that belong to this group. */
    public Set<String> trackers = Sets.newHashSet();

    public GuestExperienceResult ()
    {
        Calendar c = DateFactory.newCalendar();
        c.add(Calendar.MONTH, -1);
        _oneMonthAgo = c.getTime();
    }

    @Override
    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        if (eventData.getDate("acct_start").before(_oneMonthAgo)) {
            return false;
        }

        this.vector = eventData.getString("acct_vector");
        this.status = eventData.getString("conv_status");

        this.trackers.add(eventData.getString("acct_tracker"));
        splitAndPopulate(this.participation, eventData.getString(getEventsField()));

        return true;
    }

    public void combine (GuestExperienceResult other)
    {
        if (this.vector == null) {
            this.vector = other.vector;
        } else {
            Preconditions.checkArgument(this.vector.equals(other.vector));
        }

        if (this.status == null) {
            this.status = other.status;
        } else {
            Preconditions.checkArgument(this.status.equals(other.status));
        }

        this.trackers.addAll(other.trackers);
        for (String experience : other.participation.keySet()) {
            int thisCount = this.participation.containsKey(experience) ?
                this.participation.get(experience) : 0;
            int otherCount = other.participation.containsKey(experience) ?
                other.participation.get(experience) : 0;
            this.participation.put(experience, thisCount + otherCount);
        }
    }

    protected abstract String getEventsField ();

    /** Splits up the field, and populates the data map with the results. */
    private static void splitAndPopulate (Map<String, Integer> data, String field)
    {
        // split me up
        String[] entries = field.split(" ");
        for (String entry : entries) {
            String[] tokens = entry.split(":");
            String key = tokens[0];
            try {
                int actionCount = Integer.parseInt(tokens[1]); // try parsing if possible
                int result = (actionCount > 0) ? 1 : 0;
                data.put(key, result);
            } catch (Exception e) {
                data.put(key, 0);
            }
        }
    }

    protected Date _oneMonthAgo;
}
