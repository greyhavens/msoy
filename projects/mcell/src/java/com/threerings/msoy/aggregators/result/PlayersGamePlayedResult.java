// $Id: PlayersGamePlayedResult.java 1349 2009-02-13 01:36:02Z charlie $
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

public class PlayersGamePlayedResult implements AggregatedResult<PlayersGamePlayedResult>
{
    public void combine (final PlayersGamePlayedResult result)
    {
        this.values.addAll(result.values);
    }

    public boolean init (final EventData eventData)
    {
        // Get the value of the unique field
        values.add(Math.abs(((Number)eventData.getData().get("gameId")).intValue()));
        return true;
    }

    public boolean putData (final Map<String, Object> result)
    {
        result.put("gamesPlayed", values.size());
        return false;
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        values.clear();
        final int count = in.readInt();
        for (int i = 0; i < count; i++) {
            values.add(in.readInt());
        }
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeInt(values.size());
        for (final Integer value : values) {
            out.writeInt(value);
        }
    }

    private final Set<Integer> values = new HashSet<Integer>();
}
