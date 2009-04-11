//
// $Id$

package com.threerings.msoy.avrg.data;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartySummary;

public class AVRGameOccupantInfo extends OccupantInfo
    implements PartyOccupantInfo
{
    /** Suitable for unserializing. */
    public AVRGameOccupantInfo ()
    {
    }

    public AVRGameOccupantInfo (PlayerObject plObj)
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
        if (_partyId != partyId) {
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

    /** The party id for this occupant. */
    protected int _partyId;
}
