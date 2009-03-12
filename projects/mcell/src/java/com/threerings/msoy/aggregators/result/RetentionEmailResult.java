//
// $Id$

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.common.event.EventName;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

/**
 * Counts up all the members who were sent a retention email and outputs a map of the member id
 * to the date the email was sent.
 */
public class RetentionEmailResult
    implements AggregatedResult<RetentionEmailResult>
{

    @Override // from AggregatedResult
    public boolean init (EventData eventData)
    {
        if (!EVENT_NAME.equals(eventData.getEventName())) {
            return false;
        }

        Date date = eventData.getDate("timestamp");
        Integer memberId = eventData.getInt("recipientId");
        _sent.put(memberId, date);
        return true;
    }

    @Override // from AggregatedResult
    public boolean putData (Map<String, Object> result)
    {
        result.put("dates_sent_by_member", _sent);
        return false;
    }

    @Override // from AggregatedValue
    public void combine (RetentionEmailResult value)
    {
        // just take the most recent date for each member
        for (Map.Entry<Integer, Date> entry : value._sent.entrySet()) {
            Date mine = _sent.get(entry.getKey());
            if (mine == null || entry.getValue().after(mine)) {
                _sent.put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override // from Writable
    public void readFields (DataInput in)
        throws IOException
    {
        _sent.clear();
        int size = in.readInt();
        for (int ii = 0; ii < size; ++ii) {
            int id = in.readInt();
            long date = in.readLong();
            _sent.put(id, new Date(date));
        }
    }

    @Override // from Writable
    public void write (DataOutput out)
        throws IOException
    {
        out.writeInt(_sent.size());
        for (Map.Entry<Integer, Date> e : _sent.entrySet()) {
            out.writeInt(e.getKey());
            out.writeLong(e.getValue().getTime());
        }
    }

    protected HashMap<Integer, Date> _sent = Maps.newHashMap();

    protected final static EventName EVENT_NAME = new EventName("RetentionMailSent");
}
