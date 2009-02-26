// $Id: GameAffinityResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

public class GameAffinityResult implements AggregatedResult<GameAffinityResult>
{
    public void combine (final GameAffinityResult result)
    {
        for (final Entry<Integer, Set<Integer>> entry : result.gamesPlayedByPlayers.entrySet()) {
            Set<Integer> set = this.gamesPlayedByPlayers.get(entry.getKey());
            if (set == null) {
                set = new HashSet<Integer>();
                this.gamesPlayedByPlayers.put(entry.getKey(), set);
            }
            set.addAll(entry.getValue());
        }
    }

    public boolean init (final EventData eventData)
    {
        if (! eventData.getData().containsKey("playerId")) {
            return false; // old event type
        }

        final int playerId = ((Number) eventData.getData().get("playerId")).intValue();
        final int gameId = Math.abs(((Number) eventData.getData().get("gameId")).intValue());
        final Set<Integer> set = new HashSet<Integer>();
        set.add(playerId);
        gamesPlayedByPlayers.put(gameId, set);

        return true;
    }

    public boolean putData (final Map<String, Object> result)
    {
        // Do not process any records if nothing was recorded.
        if (gamesPlayedByPlayers.isEmpty()) {
            return false;
        }

        // Initialize if we haven't already.
        if (itor1 == null) {
            itor1 = gamesPlayedByPlayers.entrySet().iterator();
            itor2 = gamesPlayedByPlayers.entrySet().iterator();
            curEntry = itor1.next();
        }

        // If we've reached the end of the second iterator, restart and go to the next item
        // in the first iterator.
        if (!itor2.hasNext()) {
            curEntry = itor1.next();
            itor2 = gamesPlayedByPlayers.entrySet().iterator();
        }

        // Get the next item in the second iterator.  We should only create a record if the
        // second game ID > first game ID.
        final Entry<Integer, Set<Integer>> thisEntry = itor2.next();
        if (thisEntry.getKey() > curEntry.getKey()) {
            result.put("game1", curEntry.getKey());
            result.put("game2", thisEntry.getKey());
            result.put("players", intersection(curEntry.getValue(), thisEntry.getValue()).size());
        }

        return itor1.hasNext() || itor2.hasNext();
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        gamesPlayedByPlayers.clear();
        final int count = in.readInt();
        for (int i = 0; i < count; i++) {
            final int key = in.readInt();
            final Set<Integer> set = new HashSet<Integer>();
            final int setCount = in.readInt();
            for (int j = 0; j < setCount; j++) {
                set.add(in.readInt());
            }
            gamesPlayedByPlayers.put(key, set);
        }
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeInt(gamesPlayedByPlayers.size());
        for (final Entry<Integer, Set<Integer>> entry : gamesPlayedByPlayers.entrySet()) {
            out.writeInt(entry.getKey());
            out.writeInt(entry.getValue().size());
            for (final int value : entry.getValue()) {
                out.writeInt(value);
            }
        }
    }

    private static <T> Set<T> intersection (final Set<T> set1, final Set<T> set2)
    {
        final Set<T> set = new HashSet<T>(set1);
        set.retainAll(set2);
        return set;
    }

    private final Map<Integer, Set<Integer>> gamesPlayedByPlayers = new HashMap<Integer, Set<Integer>>();

    // Transient data used by putData()
    private Iterator<Entry<Integer, Set<Integer>>> itor1 = null;
    private Entry<Integer, Set<Integer>> curEntry = null;
    private Iterator<Entry<Integer, Set<Integer>>> itor2 = null;
}
