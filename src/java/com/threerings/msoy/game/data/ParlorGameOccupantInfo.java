//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartySummary;

public class ParlorGameOccupantInfo extends WhirledGameOccupantInfo
    implements PartyOccupantInfo
{
    /** Suitable for unserializing. */
    public ParlorGameOccupantInfo ()
    {
    }

    public ParlorGameOccupantInfo (PlayerObject plObj)
    {
        super(plObj);
        PartySummary party = plObj.getParty();
        updatePartyId((party == null) ? 0 : party.id);
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
