package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet_Entry;

/** Represent the amount of flow a player may be granted per minute. */
public class FlowRate
    implements DSet_Entry
{
    /** The id of the player whose flow rate budget we represent. */
    public var playerId :int;

    /** The rate of flow per minute this player can successfully be awarded. */
    public var flowRate :int;

    // from DSet_Entry
    public function getKey () :Object
    {
        return playerId;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        playerId = ins.readInt();
        flowRate = ins.readInt();
    }
}
}
