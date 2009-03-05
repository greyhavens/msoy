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

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.HadoopSerializationUtil;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

public class SessionsResult implements AggregatedResult<SessionsResult>
{
    public void combine (final SessionsResult result)
    {
        if (this.playerStats == null) {
            this.playerStats = result.playerStats;
            this.guestStats = result.guestStats;
        } else {
            this.playerStats = new GroupStats(this.playerStats, result.playerStats);
            this.guestStats = new GroupStats(this.guestStats, result.guestStats);
        }
    }

    public boolean init (final EventData eventData)
    {
        final int memberId = ((Number) eventData.getData().get("memberId")).intValue();
        if (memberId == 0) {
            return false; // this is a whirled page lurker - ignore
        }
        
        Boolean isGuest = (Boolean) eventData.getData().get("isGuest");

        final Map<String, Object> data = eventData.getData();
        final int inOwnRoomsTotal = getClampedSeconds(data, "inMyRooms");
        final int inFriendsRoomsTotal = getClampedSeconds(data, "inFriendRooms");
        final int inOtherRoomsTotal = getClampedSeconds(data, "inStrangerRooms");
        final int inWhirledsTotal = getClampedSeconds(data, "inWhirleds");
        final int activeTotal = getClampedSeconds(data, "totalActive");
        final int idleTotal = getClampedSeconds(data, "totalIdle");

        final GroupStats stats = new GroupStats(inOwnRoomsTotal, inFriendsRoomsTotal,
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
        final int result = ((Number) data.get(field)).intValue();
        return Math.min(Math.max(0, result), MAX_SECONDS);
    }

    /** Divides total by the number of samples in the given set. Returns zero for empty sets. */
    private double perSample (int total, GroupStats sampleSet) {
        return sampleSet.samples == 0 ? 0 : total / (sampleSet.samples * 60.0);
    }

    public boolean putData (final Map<String, Object> result)
    {
        final int guestInRoomsTotal = guestStats.inOwnRoomsTotal + guestStats.inFriendsRoomsTotal +
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

    public void readFields (final DataInput in)
        throws IOException
    {
        this.playerStats = readGroupStats(in);
        this.guestStats = readGroupStats(in);
    }

    public void write (final DataOutput out)
        throws IOException
    {
        writeGroupStats(out, this.playerStats);
        writeGroupStats(out, this.guestStats);
    }

    private GroupStats readGroupStats (final DataInput in)
        throws IOException
    {
        final int samples = in.readInt();
        final int inOwnRoomsTotal = in.readInt();
        final int inFriendsRoomsTotal = in.readInt();
        final int inOtherRoomsTotal = in.readInt();
        final int inWhirledsTotal = in.readInt();
        final int idleTotal = in.readInt();
        final int activeTotal = in.readInt();

        @SuppressWarnings("unchecked")
        final Set<Integer> uniques = (Set<Integer>)HadoopSerializationUtil.readObject(in);
        return new GroupStats(samples, inOwnRoomsTotal, inFriendsRoomsTotal, inOtherRoomsTotal,
            inWhirledsTotal, idleTotal, activeTotal, uniques);
    }

    private void writeGroupStats (final DataOutput out, final GroupStats stats)
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

    private final static class GroupStats {
        public final int samples;
        public final int inOwnRoomsTotal;
        public final int inFriendsRoomsTotal;
        public final int inOtherRoomsTotal;
        public final int inWhirledsTotal;
        public final int idleTotal;
        public final int activeTotal;
        public final Set<Integer> uniques = new TreeSet<Integer>();

        public GroupStats (final GroupStats stats1, final GroupStats stats2)
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

        public GroupStats (final int samples, final int inOwnRoomsTotal, final int inFriendsRoomsTotal,
            final int inOtherRoomsTotal, final int inWhirledsTotal, final int idleTotal,
            final int activeTotal, final Set<Integer> memberIds)
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

        public GroupStats (final int inOwnRoomsTotal, final int inFriendsRoomsTotal,
            final int inOtherRoomsTotal, final int inWhirledsTotal, final int idleTotal,
            final int activeTotal, final int memberId)
        {
            this(1, inOwnRoomsTotal, inFriendsRoomsTotal, inOtherRoomsTotal, inWhirledsTotal,
                idleTotal, activeTotal, Collections.singleton(memberId));
        }

        public GroupStats ()
        {
            this(0, 0, 0, 0, 0, 0, 0, Collections.<Integer>emptySet());
        }

        private static <T> Set<T> union (final Set<T> set1, final Set<T> set2)
        {
            final Set<T> set = new TreeSet<T>();
            set.addAll(set1);
            set.addAll(set2);
            return set;
        }
    }

    private GroupStats playerStats;
    private GroupStats guestStats;

    private static final int MAX_SECONDS = 4 * 60 * 60; // clamp at 4h
}
