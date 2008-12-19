//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.crowd.data.OccupantInfo;

public class PlayerInfo extends OccupantInfo
{
    /**
     * Get this player's partyId.
     */
    public function getPartyId () :int
    {
        return _partyId;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        _partyId = ins.readInt();
    }

    protected var _partyId :int;
}
}
