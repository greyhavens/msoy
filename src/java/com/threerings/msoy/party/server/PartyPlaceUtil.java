//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

public class PartyPlaceUtil
{
    /**
     * Possibly add the specified party to the place.
     */
    public static void maybeAddParty (PartyPlaceObject placeObj, PartySummary party)
    {
        if (!placeObj.getParties().containsKey(party.id)) {
            placeObj.addToParties(party);
        }
    }

    /**
     * Maybe remove a party summary from the place- should be called AFTER we any current
     * party user's occupant info.
     */
    public static void maybeRemoveParty (PartyPlaceObject placeObj, int partyId)
    {
        if (!placeObj.getParties().containsKey(partyId)) {
            return;
        }
        for (OccupantInfo info : placeObj.getOccupantInfo()) {
            if ((info instanceof PartyOccupantInfo) &&
                    (((PartyOccupantInfo) info).getPartyId() == partyId)) {
                return; // there's still a partier here!
            }
        }
        placeObj.removeFromParties(partyId);
    }
}
