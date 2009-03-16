// $Id: LoginCountResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.collect.Sets;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

public class LoginCountResult implements AggregatedResult<LoginCountResult>
{
    public void combine (final LoginCountResult result)
    {
        final LoginCountResult myResult = result;
        uniqueGuests.addAll(myResult.uniqueGuests);
        uniquePlayers.addAll(myResult.uniquePlayers);
    }

    public boolean init (final EventData eventData)
    {
        final Boolean isGuest = (Boolean) eventData.getData().get("isGuest");
        final int memberId = ((Number)eventData.getData().get("memberId")).intValue();
        if (memberId < 0 || (isGuest != null && isGuest.booleanValue())) {
            uniqueGuests.add(memberId);
        } else {
            uniquePlayers.add(memberId);
        }
        return true;
    }

    public boolean putData (final Map<String, Object> result)
    {
        result.put("uniquePlayers", uniquePlayers.size());
        result.put("uniqueGuests", uniqueGuests.size());
        result.put("total", Sets.union(uniqueGuests, uniquePlayers).size());
        return false;
    }

    @SuppressWarnings("unchecked")
    public void readFields (final DataInput in)
        throws IOException
    {
        uniquePlayers = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
        uniqueGuests = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
    }

    public void write (final DataOutput out)
        throws IOException
    {
        HadoopSerializationUtil.writeObject(out, uniquePlayers);
        HadoopSerializationUtil.writeObject(out, uniqueGuests);
    }

    private TreeSet<Integer> uniquePlayers = new TreeSet<Integer>();
    private TreeSet<Integer> uniqueGuests = new TreeSet<Integer>();
}
