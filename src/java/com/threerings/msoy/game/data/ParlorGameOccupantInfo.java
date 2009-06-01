//
// $Id$

package com.threerings.msoy.game.data;

import com.whirled.game.data.WhirledGameOccupantInfo;

import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.MsoyUserOccupantInfo;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartySummary;

public class ParlorGameOccupantInfo extends WhirledGameOccupantInfo
    implements MsoyUserOccupantInfo, PartyOccupantInfo
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
        updateTokens(plObj.tokens);
    }

    // from MsoyUserOccupantInfo
    public boolean isSubscriber ()
    {
        return _subscriber;
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

    // from MsoyUserOccupantInfo
    public boolean updateTokens (MsoyTokenRing tokens)
    {
        boolean changed = false;
        if (isSubscriber() != tokens.isSubscriber()) {
            _subscriber = !_subscriber;
            changed = true;
        }
        return changed;
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        super.toString(buf);
        buf.append(", party=").append(_partyId);
    }

    protected int _partyId;
    protected boolean _subscriber;
    // if we add more flags, we can create a _flags byte and combine subscriber into it
}
