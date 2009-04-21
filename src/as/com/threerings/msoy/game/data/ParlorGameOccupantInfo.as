//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.util.StringBuilder;

import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.party.data.PartyOccupantInfo;

public class ParlorGameOccupantInfo extends WhirledGameOccupantInfo
    implements PartyOccupantInfo
{
    // from PartyOccupantInfo
    public function getPartyId () :int
    {
        return _partyId;
    }

    override public function clone () :Object
    {
        var that :ParlorGameOccupantInfo = super.clone() as ParlorGameOccupantInfo;
        that._partyId = this._partyId;
        return that;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _partyId = ins.readInt();
    }

    override protected function toStringBuilder (buf :StringBuilder) :void
    {
        super.toStringBuilder(buf);
        buf.append(", partyId=", _partyId);
    }

    protected var _partyId :int;
}
}
