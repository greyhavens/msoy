// $Id: CohortReturnResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class CohortReturnResult implements AggregatedResult<CohortReturnResult>
{
    public void combine (final CohortReturnResult result)
    {
        this.memberIds.addAll(result.memberIds);
    }

    public boolean init (final EventData eventData)
    {
        final Integer playerId = ((Number)eventData.getData().get("memberId")).intValue();
        if (playerId <= 0) {
            return false; // ignore guests
        }
        memberIds.add(playerId);
        return true;
    }

    public boolean putData (final Map<String, Object> result)
    {
        result.put("uniqueReturns", memberIds.size());
        return false;
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        memberIds.clear();
        final int count = in.readInt();
        for (int i = 0; i < count; i++) {
            memberIds.add(in.readInt());
        }
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeInt(memberIds.size());
        for (final int value : memberIds) {
            out.writeInt(value);
        }
    }

    private final Set<Integer> memberIds = new HashSet<Integer>();
}
