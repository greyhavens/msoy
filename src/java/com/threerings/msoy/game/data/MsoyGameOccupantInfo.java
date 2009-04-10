//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.party.data.PartyOccupantInfo;

public class MsoyGameOccupantInfo extends WhirledGameOccupantInfo
    implements PartyOccupantInfo
{
    /** Suitable for unserializing. */
    public MsoyGameOccupantInfo ()
    {
    }

    public MsoyGameOccupantInfo (PlayerObject plObj)
    {
        super(plObj);
        updatePartyId(plObj.partyId);
    }

    // from PartyOccupantInfo
    public int getPartyId ()
    {
        return _partyId;
    }

    // from PartyOccupantInfo
    public boolean updatePartyId (int partyId)
    {
        if (partyId != _partyId) {
            _partyId = partyId;
            return true;
        }
        return false;
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", party=").append(_partyId);
    }

    protected int _partyId;
}
