// $Id: MultiServerAverageResult.java 1372 2009-02-21 19:53:20Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldAggregatedResult;
import com.threerings.panopticon.reporter.aggregator.result.field.FieldKey;

public class MultiServerAverageResult extends FieldAggregatedResult
{
    public List<Sample> samples = Lists.newArrayList();

    public void doInit (EventData eventData)
    {
        Sample sample = new Sample();
        sample.init(eventData);
        samples.add(sample);
    }

    public boolean putData (final Map<String, Object> result)
    {
        // sort our samples by time
        List<Sample> sorted = Lists.sortedCopy(samples);

        // per-server time series
        Map<String, TreeSet<Sample>> timeSeries = toServerTimeSeries(samples);

        int count = 0;
        double low = Double.POSITIVE_INFINITY;
        double high = Double.NEGATIVE_INFINITY;
        double total = 0;

        // iterate through all the samples from oldest to newest
        for (final Sample sample : sorted) {
            double populationAtTimestamp = getPopulationAt(timeSeries, sample);

            total += populationAtTimestamp;
            low = Math.min(populationAtTimestamp, low);
            high = Math.max(populationAtTimestamp, high);
            count++;
        }

        result.put("mean", total / count);
        result.put("low", low);
        result.put("high", high);
        return false;
    }

    public static class Sample extends FieldKey
    {
        public Date timestamp;
        public String server;
        public int value;

        @Override
        public void init (EventData eventData)
        {
            Object dataTimestamp = eventData.get("timestamp");
            if (dataTimestamp instanceof Long) {
                timestamp = new Date((Long)dataTimestamp);
            } else {
                timestamp = (Date)dataTimestamp;
            }
            value = ((Number)eventData.get("total")).intValue();
            server = eventData.getString("serverName");
        }
    }

    private static Map<String, TreeSet<Sample>> toServerTimeSeries (List<Sample> samples)
    {
        Multimap<String, Sample> acc = Multimaps.newArrayListMultimap();
        for (Sample sample : samples) {
            acc.put(sample.server, sample);
        }

        Map<String, TreeSet<Sample>> results = Maps.newTreeMap();
        for (Entry<String, Collection<Sample>> entry : acc.asMap().entrySet()) {
            results.put(entry.getKey(), Sets.newTreeSet(entry.getValue()));
        }

        return results;
    }

    /**
     * Returns the total population across all the time series at the given point in time,
     * interpolating as necessary.
     */
    private static double getPopulationAt (Map<String, TreeSet<Sample>> allSeries, Sample sample)
    {
        double total = 0;
        for (TreeSet<Sample> singleSeries : allSeries.values()) {
            total += getPopulationAt(singleSeries, sample);
        }

        return total;
    }

    /**
     * Returns the population from the time series at the given point in time,
     * interpolating as necessary.
     */
    private static double getPopulationAt (TreeSet<Sample> series, Sample sample)
    {
        // get the data point at or after the sample
        SortedSet<Sample> tail = series.tailSet(sample);
        Sample next = tail.isEmpty() ? null : tail.first();

        // did we hit the one we want?
        if (sample.equals(next)) {
            return sample.value;
        }

        // get the data point right before the sample
        SortedSet<Sample> head = series.headSet(sample);
        Sample previous = head.isEmpty() ? null : head.last();

        // if we have both, interpolate their values
        if (next != null && previous != null) {
            double p = ((double)(next.timestamp.getTime() - sample.timestamp.getTime())) /
                (next.timestamp.getTime() - previous.timestamp.getTime());

            return p * previous.value + (1 - p) * next.value;
        }

        // this means we're off the series, and one of the data points is missing.
        if (next != null) {
            return estimatedValue(next, sample);
        }
        if (previous != null) {
            return estimatedValue(previous, sample);
        }

        // we have no data at all!
        return 0;
    }

    /** Returns an estimated value, based on the nearest data point to the given sample. */
    private static double estimatedValue (Sample nearest, Sample sample)
    {
        long timeDelta = Math.abs(nearest.timestamp.getTime() - sample.timestamp.getTime());
        if (timeDelta < MAX_DELAY) {
            return nearest.value; // just use the nearest neighbor
        }

        // we're off the charts; the server has long been stopped - or hasn't started yet
        return 0;
    }

    /**
     * Max delay between individual log actions; it's a small multiplier of Msoy's
     * {@link MsoyAdminManager.STATS_DELAY}. Current value is 30 minutes,
     * since STATS_DELAY is 10 minutes.
     */
    private final static long MAX_DELAY = 1000 * 60 * 30;
}
