// $Id: MemberStatsResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.io.WritableComparable;

import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.shared.util.TimeRange;

public class MemberStatsResult implements AggregatedResult<WritableComparable<?>, MemberStatsResult>
{
    public void combine (MemberStatsResult result)
    {
        for (Entry<String, Sample> eventSample : result.serverSamples.entrySet()) {
            Sample prevSample = this.serverSamples.get(eventSample.getKey());
            if (prevSample == null) {
                this.serverSamples.put(eventSample.getKey(), eventSample.getValue());
            } else {
                this.serverSamples.put(eventSample.getKey(), new Sample(prevSample, eventSample.getValue()));
            }
        }
    }

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        String servername = (String)eventData.getData().get("serverName");
        int active = ((Number)eventData.getData().get("active")).intValue();
        int guests = ((Number)eventData.getData().get("guests")).intValue();
        int total = ((Number)eventData.getData().get("total")).intValue();
        serverSamples.put(servername, new Sample(1, active, guests, total));

        return true;
    }

    public boolean putData (Map<String, Object> result)
    {
        // Add the averages across servers together to get a total across the entire system.
        double avgActives = 0.0;
        double avgGuests = 0.0;
        double avgTotal = 0.0;
        int count = 0;
        for (Sample sample : serverSamples.values()) {
            count += sample.count;
            avgActives += (double)sample.totalActive / (double)sample.count;
            avgGuests += (double)sample.totalGuests / (double)sample.count;
            avgTotal += (double)sample.totalOverall / (double)sample.count;
        }

        Calendar c = TimeRange.roundDown(((Date)result.get("date")).getTime(), Calendar.DAY_OF_MONTH);
        result.put("day", c.getTime());
        result.put("active", avgActives);
        result.put("guests", avgGuests);
        result.put("total", avgTotal);
        result.put("sampleCount", count);
        return false;
    }

    public void readFields (DataInput in)
        throws IOException
    {
        serverSamples.clear();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            String key = in.readUTF();
            int count = in.readInt();
            long totalActive = in.readLong();
            long totalGuests = in.readLong();
            long totalOverall = in.readLong();
            serverSamples.put(key, new Sample(count, totalActive, totalGuests, totalOverall));
        }
    }

    public void write (DataOutput out)
        throws IOException
    {
        out.writeInt(serverSamples.size());
        for (Entry<String, Sample> entry : serverSamples.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().count);
            out.writeLong(entry.getValue().totalActive);
            out.writeLong(entry.getValue().totalGuests);
            out.writeLong(entry.getValue().totalOverall);
        }
    }

    private static class Sample
    {
        public int count;
        public long totalActive;
        public long totalGuests;
        public long totalOverall;

        public Sample (int count, long totalActive, long totalGuests, long totalOverall)
        {
            this.count = count;
            this.totalActive = totalActive;
            this.totalGuests = totalGuests;
            this.totalOverall = totalOverall;
        }

        public Sample (Sample sample1, Sample sample2)
        {
            this(sample1.count + sample2.count, sample1.totalActive + sample2.totalActive,
                sample1.totalGuests + sample2.totalGuests, sample1.totalOverall + sample2.totalOverall);
        }
    }

    private Map<String, Sample> serverSamples = new HashMap<String, Sample>();
}
