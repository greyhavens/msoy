package com.threerings.msoy.game.server;

import com.threerings.presents.dobj.DSet;

/** Represent the amount of flow a player may be granted per minute. */
public class FlowRate
    implements DSet.Entry
{
    /** The id of the player whose flow rate budget we represent. */
    public int playerId;

    /** The rate of flow per minute this player can successfully be awarded. */
    public int flowRate;

    /**
     */
    public FlowRate (int playerId, int flowRate)
    {
        this.playerId = playerId;
        this.flowRate = flowRate;
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return playerId;
    }

}
