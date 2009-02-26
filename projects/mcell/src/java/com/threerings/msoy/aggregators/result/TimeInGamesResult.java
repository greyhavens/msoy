// $Id: TimeInGamesResult.java 1349 2009-02-13 01:36:02Z charlie $
//
// Panopticon Copyright 2007-2009 Three Rings Design

package com.threerings.msoy.aggregators.result;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.panopticon.common.event.EventData;
import com.threerings.panopticon.reporter.aggregator.result.AggregatedResult;

public class TimeInGamesResult implements AggregatedResult<TimeInGamesResult>
{
    public boolean init (final EventData eventData)
    {
        final int playerId = ((Number)eventData.getData().get("playerId")).intValue();
        if (playerId == 0) {
            return false; // this is a whirled page lurker - ignore
        }

        // if someone played for over half an hour, record it as half an hour
        long secondsInGame = ((Number)eventData.getData().get("secondsInGame")).longValue();
        if (secondsInGame > MAX_SECONDS) {
            secondsInGame = MAX_SECONDS;
        }

        // positive player ids mean a player, negative mean a guest
        if (playerId < 0) {
            guestPlayerIds.add(playerId);
            guestSecondsInGame = secondsInGame;
        } else {
            playerPlayerIds.add(playerId);
            playerSecondsInGame = secondsInGame;
        }
        return true;
    }

    public void combine (final TimeInGamesResult result)
    {
        guestSecondsInGame += result.guestSecondsInGame;
        playerSecondsInGame += result.playerSecondsInGame;
        this.guestPlayerIds.addAll(result.guestPlayerIds);
        this.playerPlayerIds.addAll(result.playerPlayerIds);
    }

    public boolean putData (final Map<String, Object> result)
    {
        result.put("guestAvgMinutesInGames", guestPlayerIds.size() == 0 ? 0
            : (guestSecondsInGame / new Double(guestPlayerIds.size())) / 60);
        result.put("playerAvgMinutesInGames", playerPlayerIds.size() == 0 ? 0
            : (playerSecondsInGame / new Double(playerPlayerIds.size())) / 60);

        return false;
    }

    public void readFields (final DataInput in)
        throws IOException
    {
        guestSecondsInGame = in.readLong();
        playerSecondsInGame = in.readLong();

        guestPlayerIds.clear();
        final int guestCount = in.readInt();
        for (int i = 0; i < guestCount; i++) {
            guestPlayerIds.add(in.readInt());
        }

        playerPlayerIds.clear();
        final int playerCount = in.readInt();
        for (int i = 0; i < playerCount; i++) {
            playerPlayerIds.add(in.readInt());
        }
    }

    public void write (final DataOutput out)
        throws IOException
    {
        out.writeLong(guestSecondsInGame);
        out.writeLong(playerSecondsInGame);

        out.writeInt(guestPlayerIds.size());
        for (final int value : guestPlayerIds) {
            out.writeInt(value);
        }

        out.writeInt(playerPlayerIds.size());
        for (final int value : playerPlayerIds) {
            out.writeInt(value);
        }
    }

    /** Total seconds unregistered users have spent in games */
    private long guestSecondsInGame = 0;

    /** Total seconds registered users have spent in games */
    private long playerSecondsInGame = 0;

    /** set of player ids representing unique guests */
    private final Set<Integer> guestPlayerIds = new HashSet<Integer>();

    /** set of player ids representing unique players */
    private final Set<Integer> playerPlayerIds = new HashSet<Integer>();

    /** If a single play lasts longer than 30 minutes, set it to exactly 30 minutes */
    private static int MAX_SECONDS = 60 * 30;
}
