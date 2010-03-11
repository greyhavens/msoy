// $Id: LoginCountResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.TreeSet;

import org.apache.hadoop.io.WritableComparable;

import com.google.common.collect.Sets;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;

public class LoginCountResult implements AggregatedResult<WritableComparable<?>, LoginCountResult>
{
    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        EventName name = eventData.getEventName();

        // register trackers both from LOGIN and VISITOR_INFO_CREATED events, coping with
        // old events where tracker was called sessionToken
        if (eventData.containsKey("tracker")) {
            _uniqueVisitors.add((String)eventData.get("tracker"));
        } else if (eventData.containsKey("sessionToken")) {
            _uniqueVisitors.add((String)eventData.get("sessionToken"));
        }

        // the real juice is in the LOGIN event though
        if (LOGIN.equals(name)) {
            int memberId = eventData.getInt("memberId");
            if (eventData.getDefaultBoolean("isGuest", false)) {
                _uniqueGuests.add(memberId);
            } else {
                _uniquePlayers.add(memberId);
            }
        }
        return true;
    }

    public void combine (LoginCountResult result)
    {
        _uniqueVisitors.addAll(result._uniqueVisitors);
        _uniqueGuests.addAll(result._uniqueGuests);
        _uniquePlayers.addAll(result._uniquePlayers);
    }

    public boolean putData (Map<String, Object> result)
    {
        result.put("uniquePlayers", _uniquePlayers.size());
        result.put("uniqueGuests", _uniqueGuests.size());
        result.put("total", Sets.union(_uniqueGuests, _uniquePlayers).size());
        result.put("uniqueVisitors", _uniqueVisitors.size());
        return false;
    }

    @SuppressWarnings("unchecked")
    public void readFields (final DataInput in)
        throws IOException
    {
        _uniquePlayers = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
        _uniqueGuests = (TreeSet<Integer>)HadoopSerializationUtil.readObject(in);
        _uniqueVisitors = (TreeSet<String>)HadoopSerializationUtil.readObject(in);
    }

    public void write (final DataOutput out)
        throws IOException
    {
        HadoopSerializationUtil.writeObject(out, _uniquePlayers);
        HadoopSerializationUtil.writeObject(out, _uniqueGuests);
        HadoopSerializationUtil.writeObject(out, _uniqueVisitors);
    }

    protected TreeSet<Integer> _uniquePlayers = new TreeSet<Integer>();
    protected TreeSet<Integer> _uniqueGuests = new TreeSet<Integer>();
    protected TreeSet<String> _uniqueVisitors = new TreeSet<String>();

    protected final static EventName VISITOR_INFO_CREATED = new EventName("VisitorInfoCreated");
    protected final static EventName LOGIN = new EventName("Login");
}
