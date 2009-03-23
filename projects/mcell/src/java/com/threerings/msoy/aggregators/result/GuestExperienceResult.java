// $Id: GuestExperienceResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.PropertiesAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

/**
 * Extracts a table of guest experiences before conversion, per entry vector / conversion status.
 *
 * <ul>
 *      <li><b>events_field</b>: Input field that contains an event vector
 * </ul>
 *
 * @author Robert Zubek <robert@threerings.net>
 */
public class GuestExperienceResult
    implements PropertiesAggregatedResult<GuestExperienceResult>
{
    public void configure (Configuration config)
    {
        eventsField = config.getString("eventsField");
        Preconditions.checkNotNull(eventsField, "Missing configuration parameter: eventsField");
    }

    public boolean init (final EventData eventData)
    {

        final Map<String, Object> data = eventData.getData();

        this.vector = (String) data.get("acct_vector");
        this.status = (String) data.get("conv_status");

        this.trackers.add((String) data.get("acct_tracker"));
        splitAndPopulate(this.participation, (String) data.get(eventsField));

        return true;
    }

    public void combine (final GuestExperienceResult other)
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
            final int thisCount = this.participation.containsKey(experience) ? this.participation.get(experience) : 0;
            final int otherCount = other.participation.containsKey(experience) ? other.participation.get(experience) : 0;
            this.participation.put(experience, thisCount + otherCount);
        }
    }

    public boolean putData (Map<String, Object> result)
    {
        result.put("acct_vector", this.vector);
        result.put("conv_status", this.status);

        final int total = this.trackers.size();
        result.put("count", total);

        for (Map.Entry<String, Integer> entry : this.participation.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
            final double percentage = (total == 0) ? 0.0 : ((double) entry.getValue()) / total;
            final String pstr = format.format(percentage);
            result.put("p_" + entry.getKey(), pstr);
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    public void readFields (final DataInput in)
        throws IOException
    {
        this.vector = (String) HadoopSerializationUtil.readObject(in);
        this.status = (String) HadoopSerializationUtil.readObject(in);
        this.trackers = (Set<String>)HadoopSerializationUtil.readObject(in);
        this.participation = (Map<String, Integer>)HadoopSerializationUtil.readObject(in);
    }

    public void write (final DataOutput out)
        throws IOException
    {
        HadoopSerializationUtil.writeObject(out, this.vector);
        HadoopSerializationUtil.writeObject(out, this.status);
        HadoopSerializationUtil.writeObject(out, this.trackers);
        HadoopSerializationUtil.writeObject(out, this.participation);
    }

    /** Splits up the field, and populates the data map with the results. */
    private static void splitAndPopulate (final Map<String, Integer> data, String field)
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

    private static final DecimalFormat format = new DecimalFormat("#,##0.00%");

    private String vector;
    private String status;

    /** Map from experience name to the number of people who participated in it. */
    private Map<String, Integer> participation = Maps.newHashMap();

    /** Set of all trackers that belong to this group. */
    private Set<String> trackers = Sets.newHashSet();

    private String eventsField;

}
