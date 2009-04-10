//
// $Id$

package com.threerings.msoy.server;

import com.threerings.crowd.server.BodyLocal;

import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;
import com.threerings.msoy.party.server.PartyPlaceUtil;

/**
 * Contains server-side only information for a MsoyUserObject.
 */
public class MsoyUserLocal extends BodyLocal
{
    /** Info on the party that this user is currently rocking (or null if they're dull). */
    public PartySummary party;

    /**
     * Called when we enter or leave a party.
     */
    public void updateParty (MsoyUserObject userObj, PartySummary summ)
    {
        userObj.setPartyId((summ == null) ? 0 : summ.id);
        party = summ;
    }

    /**
     * Should be called by the managers of PartyPlaceObjects when someone enters.
     */
    public void willEnterPartyPlace (PartyPlaceObject plObj)
    {
        if (party != null) {
            PartyPlaceUtil.maybeAddParty(plObj, party);
        }
    }

    /**
     * Should be called by the managers of PartyPlaceObjects when someone leaves.
     */
    public void willLeavePartyPlace (PartyPlaceObject plObj)
    {
        if (party != null) {
            PartyPlaceUtil.maybeRemoveParty(plObj, party.id);
        }
    }
}
