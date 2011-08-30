//
// $Id$

package com.threerings.msoy.game.data {

import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.io.ObjectInputStream;

import com.threerings.util.Joiner;

import com.threerings.msoy.data.MsoyUserOccupantInfo;
import com.threerings.msoy.party.data.PartyOccupantInfo;

public class ParlorGameOccupantInfo extends WhirledGameOccupantInfo
    implements MsoyUserOccupantInfo, PartyOccupantInfo
{
    // from PartyOccupantInfo
    public function getPartyId () :int
    {
        return _partyId;
    }

    // from MsoyUserOccupantInfo
    public function isSubscriber () :Boolean
    {
        return _subscriber;
    }

    override public function clone () :Object
    {
        var that :ParlorGameOccupantInfo = super.clone() as ParlorGameOccupantInfo;
        that._partyId = this._partyId;
        that._subscriber = this._subscriber;
        return that;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _partyId = ins.readInt();
        _subscriber = ins.readBoolean();
    }

    override protected function toStringJoiner (j :Joiner) :void
    {
        super.toStringJoiner(j);
        j.add("partyId", _partyId, "sub", _subscriber);
    }

    protected var _partyId :int;
    protected var _subscriber :Boolean;
}
}
