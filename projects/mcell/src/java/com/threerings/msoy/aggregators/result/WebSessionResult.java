// $Id: WebSessionResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.PropertiesAggregatedResult;
import com.threerings.panopticon.common.event.EventData;

/**
 * Counts the number of trackers involved in web sessions each day, split into three
 * categories: guests, players, and others. If a tracker was a guest but then registered,
 * they will be counted in both.
 */
public class WebSessionResult
    implements PropertiesAggregatedResult<WebSessionResult>
{
    public void configure (Configuration config)
    {
        detailsFields = config.getStringArray("details");
        totalField = config.getString("total");
    }

    public void combine (WebSessionResult result)
    {
        WebSessionResult other = result;
        this.groups.putAll(other.groups);
        this.all.addAll(other.all);
    }

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        Map<String, Object> data = eventData.getData();


        String tracker = (String) data.get("tracker");
        all.add(tracker);

        for (String field : detailsFields) {
            if (Boolean.TRUE.equals(data.get(field))) {
                groups.put(field, tracker);
            }
        }

        return true;
    }

    public boolean putData (Map<String, Object> result)
    {
        for (String fieldName : detailsFields) {
            Collection<String> trackers = this.groups.get(fieldName);
            result.put(fieldName, trackers != null ? trackers.size() : 0);
        }

        result.put(totalField, all.size());

        return false;
    }

    @SuppressWarnings("unchecked")
    public void readFields (DataInput in)
        throws IOException
    {
        this.groups = (Multimap<String, String>)HadoopSerializationUtil.readObject(in);
        this.all = (HashSet<String>)HadoopSerializationUtil.readObject(in);
    }

    public void write (DataOutput out)
        throws IOException
    {
        HadoopSerializationUtil.writeObject(out, groups);
        HadoopSerializationUtil.writeObject(out, all);
    }

    /** Mapping from field names to sets of trackers that match them that day. */
    private Multimap<String, String> groups = HashMultimap.create();

    /** Set of all trackers that day. */
    private HashSet<String> all = Sets.newHashSet();

    private String totalField;

    private String[] detailsFields;
}
