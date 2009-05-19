// $Id: SessionsResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.hadoop.io.WritableComparable;

import com.threerings.panopticon.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.aggregator.result.AggregatedResult;
import com.threerings.panopticon.common.event.EventData;

public class SessionsResult implements AggregatedResult<WritableComparable<?>, SessionsResult>
{
    public void combine (SessionsResult result)
    {
        if (this.playerStats == null) {
            this.playerStats = result.playerStats;
            this.guestStats = result.guestStats;
        } else {
            this.playerStats = new GroupStats(this.playerStats, result.playerStats);
            this.guestStats = new GroupStats(this.guestStats, result.guestStats);
        }
    }

    public boolean init (WritableComparable<?> key, EventData eventData)
    {
        int memberId = ((Number) eventData.getData().get("memberId")).intValue();
        if (memberId == 0) {
            return false; // this is a whirled page lurker - ignore
        }

        Boolean isGuest = (Boolean) eventData.getData().get("isGuest");

        Map<String, Object> data = eventData.getData();
        int inOwnRoomsTotal = getClampedSeconds(data, "inMyRooms");
        int inFriendsRoomsTotal = getClampedSeconds(data, "inFriendRooms");
        int inOtherRoomsTotal = getClampedSeconds(data, "inStrangerRooms");
        int inWhirledsTotal = getClampedSeconds(data, "inWhirleds");
        int activeTotal = getClampedSeconds(data, "totalActive");
        int idleTotal = getClampedSeconds(data, "totalIdle");

        GroupStats stats = new GroupStats(inOwnRoomsTotal, inFriendsRoomsTotal,
            inOtherRoomsTotal, inWhirledsTotal, idleTotal, activeTotal, memberId);

        if ((isGuest != null) ? isGuest.booleanValue() : (memberId < 0)) {
            playerStats = new GroupStats();
            guestStats = stats;
        } else {
            playerStats = stats;
            guestStats = new GroupStats();
        }
        return true;
    }

    /** Retrieves the number of seconds, and clamps it to be in the range [0, MAX_SECONDS]. */
    private int getClampedSeconds (Map<String, Object> data, String field)
    {
        int result = ((Number) data.get(field)).intValue();
        return Math.min(Math.max(0, result), MAX_SECONDS);
    }

    /** Divides total by the number of samples in the given set. Returns zero for empty sets. */
    private double perSample (int total, GroupStats sampleSet) {
        return sampleSet.samples == 0 ? 0 : total / (sampleSet.samples * 60.0);
    }

    public boolean putData (Map<String, Object> result)
    {
        int guestInRoomsTotal = guestStats.inOwnRoomsTotal + guestStats.inFriendsRoomsTotal +
            guestStats.inOtherRoomsTotal;

        result.put("uniqueGuests", guestStats.uniques.size());
        result.put("uniquePlayers", playerStats.uniques.size());

        result.put("guestsInRoomsTime", perSample(guestInRoomsTotal, guestStats));
        result.put("guestsInWhirledsTime", perSample(guestStats.inWhirledsTotal, guestStats));

        result.put("playersInOwnRoomsTime", perSample(playerStats.inOwnRoomsTotal, playerStats));
        result.put("playersInFriendRoomsTime", perSample(playerStats.inFriendsRoomsTotal, playerStats));
        result.put("playersInOtherRoomsTime", perSample(playerStats.inOtherRoomsTotal, playerStats));
        result.put("playersInWhirledsTime", perSample(playerStats.inWhirledsTotal, playerStats));

        result.put("guestsAvg", perSample(guestStats.activeTotal + guestStats.idleTotal, guestStats));
        result.put("guestsAvgIdle", perSample(guestStats.idleTotal, guestStats));
        result.put("guestsAvgActive", perSample(guestStats.activeTotal, guestStats));

        result.put("playersAvg", perSample(playerStats.activeTotal + playerStats.idleTotal, playerStats));
        result.put("playersAvgIdle", perSample(playerStats.idleTotal, playerStats));
        result.put("playersAvgActive", perSample(playerStats.activeTotal, playerStats));

        result.put("guestsTotal", (guestStats.activeTotal + guestStats.idleTotal) / 60.0);
        result.put("playersTotal", (playerStats.activeTotal + playerStats.idleTotal) / 60.0);

        return false;
    }

    public void readFields (DataInput in)
        throws IOException
    {
        this.playerStats = readGroupStats(in);
        this.guestStats = readGroupStats(in);
    }

    public void write (DataOutput out)
        throws IOException
    {
        writeGroupStats(out, this.playerStats);
        writeGroupStats(out, this.guestStats);
    }

    private GroupStats readGroupStats (DataInput in)
        throws IOException
    {
        int samples = in.readInt();
        int inOwnRoomsTotal = in.readInt();
        int inFriendsRoomsTotal = in.readInt();
        int inOtherRoomsTotal = in.readInt();
        int inWhirledsTotal = in.readInt();
        int idleTotal = in.readInt();
        int activeTotal = in.readInt();

        @SuppressWarnings("unchecked")
        Set<Integer> uniques = (Set<Integer>)HadoopSerializationUtil.readObject(in);
        return new GroupStats(samples, inOwnRoomsTotal, inFriendsRoomsTotal, inOtherRoomsTotal,
            inWhirledsTotal, idleTotal, activeTotal, uniques);
    }

    private void writeGroupStats (DataOutput out, GroupStats stats)
        throws IOException
    {
        out.writeInt(stats.samples);
        out.writeInt(stats.inOwnRoomsTotal);
        out.writeInt(stats.inFriendsRoomsTotal);
        out.writeInt(stats.inOtherRoomsTotal);
        out.writeInt(stats.inWhirledsTotal);
        out.writeInt(stats.idleTotal);
        out.writeInt(stats.activeTotal);
        HadoopSerializationUtil.writeObject(out, stats.uniques);
    }

    private static class GroupStats {
        public int samples;
        public int inOwnRoomsTotal;
        public int inFriendsRoomsTotal;
        public int inOtherRoomsTotal;
        public int inWhirledsTotal;
        public int idleTotal;
        public int activeTotal;
        public Set<Integer> uniques = new TreeSet<Integer>();

        public GroupStats (GroupStats stats1, GroupStats stats2)
        {
            this(stats1.samples + stats2.samples,
                stats1.inOwnRoomsTotal + stats2.inOwnRoomsTotal,
                stats1.inFriendsRoomsTotal + stats2.inFriendsRoomsTotal,
                stats1.inOtherRoomsTotal + stats2.inOtherRoomsTotal,
                stats1.inWhirledsTotal + stats2.inWhirledsTotal,
                stats1.idleTotal + stats2.idleTotal,
                stats1.activeTotal + stats2.activeTotal,
                union(stats1.uniques, stats2.uniques));
        }

        public GroupStats (int samples, int inOwnRoomsTotal, int inFriendsRoomsTotal,
                int inOtherRoomsTotal, int inWhirledsTotal, int idleTotal, int activeTotal,
                Set<Integer> memberIds)
        {
            this.samples = samples;
            this.inOwnRoomsTotal = inOwnRoomsTotal;
            this.inFriendsRoomsTotal = inFriendsRoomsTotal;
            this.inOtherRoomsTotal = inOtherRoomsTotal;
            this.inWhirledsTotal = inWhirledsTotal;
            this.idleTotal = idleTotal;
            this.activeTotal = activeTotal;
            uniques.addAll(memberIds);
        }

        public GroupStats (int inOwnRoomsTotal, int inFriendsRoomsTotal, int inOtherRoomsTotal,
                int inWhirledsTotal, int idleTotal, int activeTotal, int memberId)
        {
            this(1, inOwnRoomsTotal, inFriendsRoomsTotal, inOtherRoomsTotal, inWhirledsTotal,
                idleTotal, activeTotal, Collections.singleton(memberId));
        }

        public GroupStats ()
        {
            this(0, 0, 0, 0, 0, 0, 0, Collections.<Integer>emptySet());
        }

        private static <T> Set<T> union (Set<T> set1, Set<T> set2)
        {
            Set<T> set = new TreeSet<T>();
            set.addAll(set1);
            set.addAll(set2);
            return set;
        }
    }

    private GroupStats playerStats;
    private GroupStats guestStats;

    private static final int MAX_SECONDS = 4 * 60 * 60; // clamp at 4h
}
