// $Id: IdleActiveResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.WritableComparable;

import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class IdleActiveResult implements AggregatedResult<WritableComparable<?>, IdleActiveResult>
{
    public void combine (IdleActiveResult result)
    {
        this.activePlayers.addAll(result.activePlayers);
        this.idlePlayers.addAll(result.idlePlayers);
        this.activeGuests.addAll(result.activeGuests);
        this.idleGuests.addAll(result.idleGuests);
    }

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        int memberId = ((Number) eventData.getData().get("memberId")).intValue();
        if (memberId == 0) {
            return false; // this is a whirled page lurker - ignore
        }

        if (memberId > 0) {
            activePlayers.add(((Number) eventData.getData().get("totalActive")).intValue());
            idlePlayers.add(((Number) eventData.getData().get("totalIdle")).intValue());
        } else {
            activeGuests.add(((Number) eventData.getData().get("totalActive")).intValue());
            idleGuests.add(((Number) eventData.getData().get("totalIdle")).intValue());
        }
        return true;
    }

    public boolean putData (Map<String, Object> result)
    {
        result.put("GIdle", avg(idleGuests));
        result.put("GActive", avg(activeGuests));
        result.put("PIdle", avg(idlePlayers));
        result.put("PActive", avg(activePlayers));
        result.put("MedGuIdle", med(idleGuests));
        result.put("MedGActive", med(activeGuests));
        result.put("MedPIdle", med(idlePlayers));
        result.put("MedPActive", med(activePlayers));
        return false;
    }

    public void readFields (DataInput in)
        throws IOException
    {
        readList(in, activePlayers);
        readList(in, idlePlayers);
        readList(in, activeGuests);
        readList(in, idleGuests);
    }

    public void write (DataOutput out)
        throws IOException
    {
        writeList(out, activePlayers);
        writeList(out, idlePlayers);
        writeList(out, activeGuests);
        writeList(out, idleGuests);
    }

    private void writeList (DataOutput out, List<Integer> list)
        throws IOException
    {
        out.writeInt(list.size());
        for (int value : list) {
            out.writeInt(value);
        }
    }

    private void readList (DataInput in, List<Integer> list)
        throws IOException
    {
        list.clear();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            list.add(in.readInt());
        }
    }

    private int avg (List<Integer> list)
    {
        int total = 0;
        int size = list.size();
        if (size == 0) {
            return 0;
        }

        for (Integer ii : list) {
            total += ii;
        }
        return total / size;
    }

    private int med (List<Integer> list)
    {
        int size = list.size();
        if (size == 0) {
            return 0;
        }

        int[] array = new int[size];
        for (int ii = 0; ii < array.length; ii++) {
            array[ii] = list.get(ii).intValue();
        }
        Arrays.sort(array);
        int pos = array.length / 2;
        return array[pos];
    }

    private List<Integer> activePlayers = new ArrayList<Integer>();
    private List<Integer> idlePlayers = new ArrayList<Integer>();
    private List<Integer> activeGuests = new ArrayList<Integer>();
    private List<Integer> idleGuests = new ArrayList<Integer>();

}
